'use strict';

const {WorkloadModuleBase} = require('@hyperledger/caliper-core');
const {getResult} = require('./utils');
const {createDrugs, findByTagId} = require("./Utils");

class VerifyDrugWorkload extends WorkloadModuleBase {
    constructor() {
        super();
        this.txIndex = 0;
        this.drugNameIndex = 0;
        this.drugInstanceIndex = 0;
        this.totalDrugNames = 0;
        this.totalDrugInstances = 0;
        this.transactionsPerWorker = 0;
        this.drugs = []
    }

    async initializeWorkloadModule(workerIndex, totalWorkers, roundIndex, roundArguments, sutAdapter, sutContext) {
        await super.initializeWorkloadModule(workerIndex, totalWorkers, roundIndex, roundArguments, sutAdapter, sutContext);
        this.workerIndex = workerIndex;
        this.totalDrugNames = roundArguments.drugNames;
        this.totalDrugInstances = roundArguments.drugInstances;
        let totalDrugsToVerify = this.totalDrugNames * this.totalDrugInstances;

        console.log(`Worker-VerifyDrugWorkload ${this.workerIndex}: totalDrugsToVerify ${totalDrugsToVerify} - drugsPerWorker ${Math.floor(totalDrugsToVerify / totalWorkers)}`)
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
        console.log(`Worker-VerifyDrugWorkload ${this.workerIndex}: Tx: ${this.txIndex} transactionsPerWorker: ${this.transactionsPerWorker} startIndex ${this.drugNameIndex} end index: ${this.drugNameEndIndex}`);
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
                console.warn(`Worker-VerifyDrugWorkload ${this.workerIndex}: Tx: ${this.txIndex} - ${drugName} - ${tagID} - drugInstanceIndex current value: ${this.drugInstanceIndex}, RESETTING to 0`)
                this.drugInstanceIndex = 0
                this.drugNameIndex++
                if (this.drugNameIndex > this.drugNameEndIndex) {
                    console.warn(`Worker-VerifyDrugWorkload ${this.workerIndex}: Tx: ${this.txIndex} - drugNameIndex value [${this.drugNameIndex}] has exceeded max value of ${this.drugNameEndIndex}`)
                    // this.drugInstanceIndex = 0
                    // this.drugNameIndex = 0
                }
            }

            let drug = findByTagId(this.drugs, tagID)
            if (drug) {
                console.log(`Worker-VerifyDrugWorkload ${this.workerIndex}: Tx: ${this.txIndex} processing drug ${JSON.stringify(drug)}`);

                //Get Assigned CRPs
                let getAssignedCrpsRequest = {
                    contractId: this.roundArguments.contractId,
                    contractFunction: 'drug-verification:getAssignedCrps',
                    invokerIdentity: 'manufacturer',
                    contractArguments: [drugName, tagID, verifierOrg],
                    readOnly: true,
                    timeout: 10
                };
                console.log(`Worker-VerifyDrugWorkload ${this.workerIndex}: Tx: ${this.txIndex} getAssignedCrpsRequest ${JSON.stringify(getAssignedCrpsRequest)}`);
                let getAssignedCrpsResponse = await this.sutAdapter.sendRequests(getAssignedCrpsRequest);
                let getAssignedCrpsResponseJson = getResult(getAssignedCrpsResponse)
                console.log(`Worker-VerifyDrugWorkload ${this.workerIndex}: Tx: ${this.txIndex} getAssignedCrpsResponseJson ${getAssignedCrpsResponseJson}`)

                //Share assigned CRPs
                let vCrps = Buffer.from(getAssignedCrpsResponse.GetResult()).toString('utf8');
                console.log(`V CRPs ${vCrps}`)
                let shareAssignedCRPsRequest = {
                    contractId: this.roundArguments.contractId,
                    contractFunction: 'drug-verification:shareAssignedCrps',
                    invokerIdentity: 'manufacturer',
                    contractArguments: [drugName, tagID, verifierOrg],
                    transientMap: {'assigned-crps': vCrps},
                    readOnly: false,
                    timeout: 10
                };
                console.log(`Worker-VerifyDrugWorkload ${this.workerIndex}: Tx: ${this.txIndex} shareAssignedCRPsRequest ${JSON.stringify(shareAssignedCRPsRequest)}`);
                let shareAssignedCRPsResponse = await this.sutAdapter.sendRequests(shareAssignedCRPsRequest);
                let shareAssignedCRPsResponseJson = getResult(shareAssignedCRPsResponse)
                console.log(`Worker-VerifyDrugWorkload ${this.workerIndex}: Tx: ${this.txIndex} shareAssignedCRPsResponseJson ${JSON.stringify(shareAssignedCRPsResponseJson)}`);

                //Verify Drug CRPs
                let verifyDrugCRPsRequest = {
                    contractId: this.roundArguments.contractId,
                    contractFunction: 'drug-verification:verifyDrugCrps',
                    invokerIdentity: verifierIdentity,
                    invokerMspId: verifierOrg,
                    contractArguments: [drugName, tagID],
                    transientMap: {'verification-crps': vCrps},
                    readOnly: false,
                    timeout: 10
                };

                console.log(`Worker-VerifyDrugWorkload ${this.workerIndex}: Tx: ${this.txIndex} verifyDrugCRPsRequest ${JSON.stringify(verifyDrugCRPsRequest)}`);
                let verifyDrugCRPsResponse = await this.sutAdapter.sendRequests(verifyDrugCRPsRequest);
                let verifyDrugCRPsResponseJson = getResult(verifyDrugCRPsResponse)

            } else {
                let getDrugRequest = {
                    contractId: this.roundArguments.contractId,
                    contractFunction: 'pharmanet-queries:ReadDrug',
                    invokerIdentity: 'manufacturer',
                    contractArguments: [drugName, tagID],
                    readOnly: true,
                    timeout: 19
                };
                console.log(`Worker-SubmitDrugVerificationCrpsWorkload ${this.workerIndex}: Tx: ${this.txIndex} getDrugRequest ${JSON.stringify(getDrugRequest)}`)
                let getDrugResponse = await this.sutAdapter.sendRequests(getDrugRequest);
                let result = getResult(getDrugResponse);
            }
        } catch (e) {
            console.error(`Worker-VerifyDrugWorkload ${this.workerIndex}: Tx: ${this.txIndex} - - failed on drug ${drugName} and tagId ${tagID}`)
        }
    }

    async cleanupWorkloadModule() {
        console.log(`Worker-VerifyDrugWorkload ${this.workerIndex}: Tx: ${this.txIndex} processed ${this.drugs.length} tagIds: ${this.drugs}`)
    }
}

function createWorkloadModule() {
    return new VerifyDrugWorkload();
}

module.exports.createWorkloadModule = createWorkloadModule;
