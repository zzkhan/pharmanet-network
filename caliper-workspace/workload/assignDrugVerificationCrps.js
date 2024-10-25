'use strict';

const {WorkloadModuleBase} = require('@hyperledger/caliper-core');
const {getResult} = require('./utils');
const {createDrugs} = require('./utils');
const {findByTagId} = require('./utils');

class AssignDrugVerificationCrpsWorkload extends WorkloadModuleBase {
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

        this.drugs = JSON.parse(JSON.stringify(createDrugs(this.drugNameIndex, this.drugNameEndIndex, this.totalDrugInstances)))
        console.log(`Worker-AssignDrugVerificationCrpsWorkload - initializeWorkloadModule - ${this.workerIndex}: Tx: ${this.txIndex} transactionsPerWorker: ${this.transactionsPerWorker} startIndex ${this.drugNameIndex} end index: ${this.drugNameEndIndex}`);
    }

    async submitTransaction() {
        let drugName = 'NA'
        let tagID = 'NA'
        this.txIndex++
        console.log(`Worker-AssignDrugVerificationCrpsWorkload - ${this.workerIndex}: Tx: ${this.txIndex} ABOUT TO PROCESS indices - this.drugNameIndex ${this.drugNameIndex} this.drugInstanceIndex: ${this.drugInstanceIndex}`);

        drugName = `drug_1_${this.drugNameIndex}`;
        tagID = `TAG_ID_1_${this.drugNameIndex}_${this.drugInstanceIndex}`;
        let assigneeOrg = `${this.assigneeOrg}`;

        this.drugInstanceIndex++
        console.log(`Worker-AssignDrugVerificationCrpsWorkload - ${this.workerIndex}: Tx: ${this.txIndex} AFTER INC this.drugInstanceIndex ${this.drugInstanceIndex}`)
        if (this.drugInstanceIndex >= this.totalDrugInstances) {
            this.drugInstanceIndex = 0
            this.drugNameIndex++
            if (this.drugNameIndex > this.drugNameEndIndex) {
                console.warn(`Worker-AssignDrugVerificationCrpsWorkload ${this.workerIndex}: Tx: ${this.txIndex} drugNameIndex value [${this.drugNameIndex}] has exceeded max value of ${this.drugNameEndIndex}`)
                // this.drugNameIndex = 0
                // this.drugInstanceIndex = 0
            }
        }

        let drug = findByTagId(this.drugs, tagID)
        if (drug) {
            console.log(`Worker-AssignDrugVerificationCrpsWorkload ${this.workerIndex}: Tx: ${this.txIndex} doing drug ${drug}`);

            let getUnassignedCrpsRequest = {
                contractId: this.roundArguments.contractId,
                contractFunction: 'drug-verification:getUnassignedCrps',
                invokerIdentity: 'manufacturer',
                invokerMspId: 'Org1MSP',
                contractArguments: [drugName, tagID],
                readOnly: true,
                timeout: 60
            };

            let unassignedCrpsResponse = await this.sutAdapter.sendRequests(getUnassignedCrpsRequest);
            let responseJson = getResult(unassignedCrpsResponse)
            console.log(`Worker-AssignDrugVerificationCrpsWorkload ${this.workerIndex}: Tx: ${this.txIndex} JSON unassignedCrpsResponse ${responseJson}`);
            let unassignedCrpsJsonArray = JSON.parse(responseJson);
            console.log(`Worker-AssignDrugVerificationCrpsWorkload ${this.workerIndex}: Tx: ${this.txIndex} unassignedCrpsResponse ${unassignedCrpsJsonArray}`);

            let assignCrpsRequest = {
                contractId: this.roundArguments.contractId,
                contractFunction: 'drug-verification:assignCRPs',
                invokerIdentity: 'manufacturer',
                contractArguments: [drugName, tagID, assigneeOrg],
                transientMap: {'assignment-crps': JSON.stringify(unassignedCrpsJsonArray)},
                readOnly: false,
                timeout: 60
            };

            console.log(`Worker-AssignDrugVerificationCrpsWorkload ${this.workerIndex}: Tx: ${this.txIndex} assignCrpsRequest ${JSON.stringify(assignCrpsRequest)}`);
            let assignCrpsResponse = await this.sutAdapter.sendRequests(assignCrpsRequest);
            let assignCrpsResponseJson = getResult(assignCrpsResponse)
            console.log(`Worker-AssignDrugVerificationCrpsWorkload ${this.workerIndex}: Tx: ${this.txIndex} assignCrpsResponse ${assignCrpsResponseJson}`);
        }
    }

    async cleanupWorkloadModule() {
        console.log(`Worker-AssignDrugVerificationCrpsWorkload ${this.workerIndex}: Tx: ${this.txIndex} processed processed ${this.processedTagIds.length} tagIds: ${this.processedTagIds}`)
    }
}


function createWorkloadModule() {
    return new AssignDrugVerificationCrpsWorkload();
}

module.exports.createWorkloadModule = createWorkloadModule;
