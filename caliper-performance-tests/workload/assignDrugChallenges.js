'use strict';

const {WorkloadModuleBase} = require('@hyperledger/caliper-core');
const {getResult} = require('./utils');
const {createDrugs, findByTagId} = require("./Utils");

class AssignDrugChallengesWorkload extends WorkloadModuleBase {
    constructor() {
        super();
        this.txIndex = 0;
        this.drugNameIndex = 0;
        this.drugInstanceIndex = 0;
        this.totalDrugNames = 0;
        this.totalDrugInstances = 0;
        this.assigneeOrg = ''
        this.processedTagIds = []
        this.transactionsPerWorker = 0
        this.drugs = []
    }

    async initializeWorkloadModule(workerIndex, totalWorkers, roundIndex, roundArguments, sutAdapter, sutContext) {
        await super.initializeWorkloadModule(workerIndex, totalWorkers, roundIndex, roundArguments, sutAdapter, sutContext);
        this.workerIndex = workerIndex;
        this.assigneeOrg = roundArguments.assigneeOrg;
        this.totalDrugNames = roundArguments.drugNames;
        this.totalDrugInstances = roundArguments.drugInstances;
        let totalDrugsToCreate = this.totalDrugNames * this.totalDrugInstances;

        this.transactionsPerWorker = Math.floor(totalDrugsToCreate / totalWorkers);
        let drugNamesPerWorker = this.totalDrugNames / totalWorkers;
        this.drugNameIndex = Math.max(this.workerIndex * drugNamesPerWorker, 0)

        if (this.workerIndex === (totalWorkers - 1)) {
            this.drugNameEndIndex = (this.drugNameIndex) + (drugNamesPerWorker - 1) + (this.totalDrugNames % totalWorkers)
        } else {
            this.drugNameEndIndex = (this.drugNameIndex) + (drugNamesPerWorker - 1)
        }

        this.drugs = createDrugs(this.drugNameIndex, this.drugNameEndIndex, this.totalDrugInstances)

        console.log(`Worker-AssignDrugVerificationCrpsWorkload - initializeWorkloadModule - ${this.workerIndex}: Tx: ${this.txIndex} this.drugs: ${this.drugs.length} first ${JSON.stringify(this.drugs[0])} - last ${JSON.stringify(this.drugs[this.drugs.length-1])}`);
    }

    async submitTransaction() {
        let drugName = 'NA'
        let tagID = 'NA'
        this.txIndex++
        console.log(`Worker-AssignDrugChallengesWorkload - ${this.workerIndex}: Tx: ${this.txIndex} ABOUT TO PROCESS indices - this.drugNameIndex ${this.drugNameIndex} this.drugInstanceIndex: ${this.drugInstanceIndex}`);

        drugName = `drug_1_${this.drugNameIndex}`;
        tagID = `TAG_ID_1_${this.drugNameIndex}_${this.drugInstanceIndex}`;
        let assigneeOrg = `${this.assigneeOrg}`;

        this.drugInstanceIndex++
        console.log(`Worker-AssignDrugChallengesWorkload - ${this.workerIndex}: Tx: ${this.txIndex} AFTER INC this.drugInstanceIndex ${this.drugInstanceIndex}`)
        if (this.drugInstanceIndex >= this.totalDrugInstances) {
            this.drugInstanceIndex = 0
            this.drugNameIndex++
            if (this.drugNameIndex > this.drugNameEndIndex) {
                console.warn(`Worker-AssignDrugChallengesWorkload ${this.workerIndex}: Tx: ${this.txIndex} drugNameIndex value [${this.drugNameIndex}] has exceeded max value of ${this.drugNameEndIndex}`)
                // this.drugNameIndex = 0
                // this.drugInstanceIndex = 0
            }
        }

        let drug = findByTagId(this.drugs, tagID)
        if (drug) {
            console.log(`Worker-AssignDrugChallengesWorkload - ${this.workerIndex}: Tx: ${this.txIndex} processing drug ${JSON.stringify(drug)}`);
            drugName = drug.name;
            tagID = drug.tagId;

            let getAssignedCrpsRequest = {
                contractId: this.roundArguments.contractId,
                contractFunction: 'drug-verification:getAssignedCrps',
                invokerIdentity: 'manufacturer',
                contractArguments: [drugName, tagID, assigneeOrg],
                readOnly: true,
                timeout: 60
            };

            console.log(`Worker-AssignDrugChallengesWorkload ${this.workerIndex}: Tx: ${this.txIndex} getAssignedCrpsRequest ${JSON.stringify(getAssignedCrpsRequest)}`);

            let getAssignedCrpsResponse = await this.sutAdapter.sendRequests(getAssignedCrpsRequest);
            let getAssignCrpsResponseJson = getResult(getAssignedCrpsResponse)
            console.log(`Worker-AssignDrugChallengesWorkload ${this.workerIndex}: Tx: ${this.txIndex} getAssignCrpsResponse ${JSON.stringify(getAssignCrpsResponseJson)}`);
            let assignedCrpsJsonArray = JSON.parse(getAssignCrpsResponseJson);
            console.log(`Worker-AssignDrugChallengesWorkload ${this.workerIndex}: Tx: ${this.txIndex} assignedCrpsJsonArray ${JSON.stringify(assignedCrpsJsonArray)}`)

            let assignChallengesRequest = {
                contractId: this.roundArguments.contractId,
                contractFunction: 'drug-verification:assignChallenges',
                invokerIdentity: 'manufacturer',
                contractArguments: [drugName, tagID, assigneeOrg],
                transientMap: {'assignment-crps': Buffer.from(getAssignedCrpsResponse.GetResult())},
                readOnly: false,
                timeout: 60
            };
            console.log(`Worker-AssignDrugChallengesWorkload ${this.workerIndex}: Tx: ${this.txIndex} assignChallengesRequest ${JSON.stringify(assignChallengesRequest)}`);
            let assignChallengesResponse = await this.sutAdapter.sendRequests(assignChallengesRequest);
            let assignChallengesResponseJson = getResult(assignChallengesResponse)
            console.log(`Worker-AssignDrugChallengesWorkload ${this.workerIndex}: Tx: ${this.txIndex} assignChallengesResponse ${assignChallengesResponseJson}`);
        }else {

            let getDrugRequest = {
                contractId: this.roundArguments.contractId,
                contractFunction: 'pharmanet-queries:ReadDrug',
                invokerIdentity: 'manufacturer',
                contractArguments: [drugName, tagID],
                readOnly: true,
                timeout: 60
            };
            console.log(`Worker-AssignDrugChallengesWorkload ${this.workerIndex}: Tx: ${this.txIndex} getDrugRequest ${JSON.stringify(getDrugRequest)}`)
            let getDrugResponse = await this.sutAdapter.sendRequests(getDrugRequest);
            let result = getResult(getDrugResponse);
        }
    }
    async cleanupWorkloadModule() {
        console.log(`Worker-AssignDrugChallengesWorkload ${this.workerIndex}: Tx: ${this.txIndex} processed tagIds: ${this.processedTagIds}`)
    }
}


function createWorkloadModule() {
    return new AssignDrugChallengesWorkload();
}

module.exports.createWorkloadModule = createWorkloadModule;
