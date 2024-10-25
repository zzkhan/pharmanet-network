'use strict';

const {WorkloadModuleBase} = require('@hyperledger/caliper-core');
const {FIXED_CRP_RESPONSE_UUID} = require('./registerDrug');
const {getResult} = require('./utils');
const {createDrugs, findByTagId} = require("./Utils");

class SubmitDrugVerificationCrpsWorkload extends WorkloadModuleBase {
    constructor() {
        super();
        this.txIndex = 0;
        this.drugNameIndex = 0;
        this.drugInstanceIndex = 0;
        this.totalDrugNames = 0;
        this.totalDrugInstances = 0;
        this.transactionsPerWorker=0;
        this.drugs = []
    }

    async initializeWorkloadModule(workerIndex, totalWorkers, roundIndex, roundArguments, sutAdapter, sutContext) {
        await super.initializeWorkloadModule(workerIndex, totalWorkers, roundIndex, roundArguments, sutAdapter, sutContext);
        this.workerIndex = workerIndex;
        this.totalDrugNames = roundArguments.drugNames;
        this.totalDrugInstances = roundArguments.drugInstances;
        let totalDrugsToVerify = this.totalDrugNames * this.totalDrugInstances;

        console.log(`Worker-SubmitDrugVerificationCrpsWorkload ${this.workerIndex}: totalDrugsToVerify ${totalDrugsToVerify} - drugsPerWorker ${Math.floor(totalDrugsToVerify / totalWorkers)}`)
        this.transactionsPerWorker = Math.floor(totalDrugsToVerify / totalWorkers);
        let drugNamesPerWorker = this.totalDrugNames / totalWorkers;
        this.drugNameIndex = Math.max(this.workerIndex * drugNamesPerWorker, 0)
        this.verifyingOrg = roundArguments.verifyingOrg
        this.verifierIdentity = roundArguments.verifierIdentity
        if (this.workerIndex === (totalWorkers - 1)) {
            this.drugNameEndIndex = (this.drugNameIndex) + (drugNamesPerWorker - 1) + (this.totalDrugNames % totalWorkers)
        } else {
            this.drugNameEndIndex = (this.drugNameIndex) + (drugNamesPerWorker - 1)
        }
        this.drugs = createDrugs(this.drugNameIndex, this.drugNameEndIndex, this.totalDrugInstances)
        console.log(`Worker-SubmitDrugVerificationCrpsWorkload ${this.workerIndex}: Tx: ${this.txIndex} transactionsPerWorker: ${this.transactionsPerWorker} startIndex ${this.drugNameIndex} end index: ${this.drugNameEndIndex}`);
    }

    async submitTransaction() {
        let drugName = 'NA'
        let tagID = 'NA'
        try {
            this.txIndex++
            drugName = `drug_1_${this.drugNameIndex}`;
            tagID = `TAG_ID_1_${this.drugNameIndex}_${this.drugInstanceIndex}`;
            const verifierOrg = `${this.verifyingOrg}`
            const verifierIdentity = `${this.verifierIdentity}`

            this.drugInstanceIndex++
            if (this.drugInstanceIndex >= this.totalDrugInstances) {
                console.warn(`Worker-SubmitDrugVerificationCrpsWorkload ${this.workerIndex}: Tx: ${this.txIndex} - ${drugName} - ${tagID} - drugInstanceIndex current value: ${this.drugInstanceIndex}, RESETTING to 0`)
                this.drugInstanceIndex = 0
                this.drugNameIndex++
                if (this.drugNameIndex > this.drugNameEndIndex) {
                    console.warn(`Worker-SubmitDrugVerificationCrpsWorkload ${this.workerIndex}: Tx: ${this.txIndex} - drugNameIndex value [${this.drugNameIndex}] has exceeded max value of ${this.drugNameEndIndex}`)
                    // this.drugInstanceIndex = 0
                    // this.drugNameIndex = 0
                }
            }

            let drug = findByTagId(this.drugs, tagID)
            if (drug) {
                console.log(`Worker-SubmitDrugVerificationCrpsWorkload ${this.workerIndex}: Tx: ${this.txIndex} processing drug ${JSON.stringify(drug)}`);
                let getChallengesRequest = {
                    contractId: this.roundArguments.contractId,
                    contractFunction: 'drug-verification:getDrugChallenges',
                    invokerIdentity: verifierIdentity,
                    invokerMspId: verifierOrg,
                    contractArguments: [drugName, tagID],
                    readOnly: true,
                    timeout: 60
                };

                console.log(`Worker-SubmitDrugVerificationCrpsWorkload ${this.workerIndex}: Tx: ${this.txIndex} getChallengesRequest ${JSON.stringify(getChallengesRequest)}`);
                let getChallengesResponse = await this.sutAdapter.sendRequests(getChallengesRequest);
                let json = getResult(getChallengesResponse)
                let challengesArray = JSON.parse(json);

                let vCrps = this.createDrugVerificationCrps(challengesArray);
                console.log(`vCrps ${JSON.stringify(vCrps)}`)

                // submitDrugVerificationCrps
                let submitDrugVerificationCrpsRequest = {
                    contractId: this.roundArguments.contractId,
                    contractFunction: 'drug-verification:submitDrugVerificationCrps',
                    invokerIdentity: verifierIdentity,
                    invokerMspId: verifierOrg,
                    contractArguments: [drugName, tagID],
                    transientMap: {'verification-crps': JSON.stringify(vCrps)},
                    readOnly: false,
                    timeout: 60
                };

                console.log(`Worker-SubmitDrugVerificationCrpsWorkload ${this.workerIndex}: Tx: ${this.txIndex} submitDrugVerificationCrpsRequest ${JSON.stringify(submitDrugVerificationCrpsRequest)}`);
                let submitDrugVerificationCrpsResponse = await this.sutAdapter.sendRequests(submitDrugVerificationCrpsRequest);
                let submitDrugVerificationCrpsResponseJson = getResult(submitDrugVerificationCrpsResponse)
            } else {
                let getDrugRequest = {
                    contractId: this.roundArguments.contractId,
                    contractFunction: 'pharmanet-queries:ReadDrug',
                    invokerIdentity: 'manufacturer',
                    contractArguments: [drugName, tagID],
                    readOnly: true,
                    timeout: 60
                };
                console.log(`Worker-SubmitDrugVerificationCrpsWorkload ${this.workerIndex}: Tx: ${this.txIndex} getDrugRequest ${JSON.stringify(getDrugRequest)}`)
                let getDrugResponse = await this.sutAdapter.sendRequests(getDrugRequest);
                let result = getResult(getDrugResponse);
            }

        } catch (e) {
            console.error(`Worker-SubmitDrugVerificationCrpsWorkload ${this.workerIndex}: Tx: ${this.txIndex} - failed on drug ${drugName} and tagId ${tagID}`)
        }
    }

    async cleanupWorkloadModule() {
        console.log(`Worker-SubmitDrugVerificationCrpsWorkload ${this.workerIndex}: Tx: ${this.txIndex} processed ${this.drugs.length} tagIds: ${this.drugs}`)
    }

    function

    createDrugVerificationCrps(challengesArray) {
        let crpList = [];
        // Use a for loop to generate JSON objects
        for (const challenge of challengesArray) {
            // Create a new JSON object
            let crp = {
                challenge: `${challenge.value}`,
                response: FIXED_CRP_RESPONSE_UUID
            };

            // Add the JSON object to the array
            crpList.push(crp);
        }
        return crpList;
    }
}

function createWorkloadModule() {
    return new SubmitDrugVerificationCrpsWorkload();
}

module.exports.createWorkloadModule = createWorkloadModule;
