'use strict';

const {WorkloadModuleBase} = require('@hyperledger/caliper-core');
const moment = require('moment');
const {v4: uuidv4} = require('uuid');
const fs = require('fs');
const {getResult} = require('./utils');

const FIXED_CRP_RESPONSE_UUID = `0d76886b-d139-4ea0-83d5-19d0d961aa88`
module.exports = {FIXED_CRP_RESPONSE_UUID};
class RetailDrugWorkload extends WorkloadModuleBase {
    constructor() {
        super();
        this.txIndex = 0;
        this.drugNameIndex = 0;
        this.drugInstanceIndex = 0;
        this.totalDrugNames = 0;
        this.totalDrugInstances = 0;
        this.processedTagIds = []
    }

    async initializeWorkloadModule(workerIndex, totalWorkers, roundIndex, roundArguments, sutAdapter, sutContext) {
        await super.initializeWorkloadModule(workerIndex, totalWorkers, roundIndex, roundArguments, sutAdapter, sutContext);
        console.log(`Worker-RetailDrugWorkload ${this.workerIndex}: totalWorkers ${totalWorkers}`)

        this.workerIndex = workerIndex;
        this.totalDrugNames = roundArguments.drugNames;
        this.totalDrugInstances = roundArguments.drugInstances;
        let totalDrugsToCreate = this.totalDrugNames * this.totalDrugInstances;
        let transactionsPerWorker = Math.floor(totalDrugsToCreate / totalWorkers);
        let drugNamesPerWorker = Math.floor(this.totalDrugNames / totalWorkers);
        this.drugNameIndex = Math.max(this.workerIndex * drugNamesPerWorker, 0)
        if (this.workerIndex === (totalWorkers - 1)) {
            this.drugNameEndIndex = (this.drugNameIndex) + (drugNamesPerWorker - 1) + (this.totalDrugNames % totalWorkers)
        } else {
            this.drugNameEndIndex = (this.drugNameIndex) + (drugNamesPerWorker - 1)
        }

        console.log(`Worker-RetailDrugWorkload ${this.workerIndex}: Tx: ${this.txIndex} drugNamesPerWorker: ${drugNamesPerWorker} transactionsPerWorker: ${transactionsPerWorker} startIndex ${this.drugNameIndex} end index: ${this.drugNameEndIndex}`);
    }

    async submitTransaction() {
        this.txIndex++

        let drugName = `drug_1_${this.drugNameIndex}`;
        let tagID = `TAG_ID_1_${this.drugNameIndex}_${this.drugInstanceIndex}`;

        console.log(`Worker-RetailDrugWorkload ${this.workerIndex}: Tx: ${this.txIndex} Creating drug ${drugName} with tagId: ${tagID}`);

        const retailDrugRequest = {
            contractId: this.roundArguments.contractId,
            contractFunction: 'drug-retail:retailDrug',
            invokerIdentity: 'pharmacy',
            invokerMspId: 'Org4MSP',
            contractArguments: [drugName, tagID],
            readOnly: false,
            timeout: 60
        };

        this.drugInstanceIndex++
        if (this.drugInstanceIndex >= this.totalDrugInstances) {
            this.drugInstanceIndex = 0
            this.drugNameIndex++
            if (this.drugNameIndex > this.drugNameEndIndex) {
                console.warn(`Worker-RetailDrugWorkload ${this.workerIndex}: Tx: ${this.txIndex} drugNameIndex value [${this.drugNameIndex}] has exceeded max value of ${this.drugNameEndIndex}`)
                // this.drugNameIndex = 0
                this.drugInstanceIndex = 0
            }
        }

        let retailDrugResponse = await this.sutAdapter.sendRequests(retailDrugRequest);
        let registerDrugResult = getResult(retailDrugResponse)

        this.processedTagIds.push(tagID)
    }

    function

    createDrugCrps(maxCrps) {
        let crpList = [];
        // Use a for loop to generate JSON objects
        for (let i = 0; i < maxCrps; i++) {
            // Create a new JSON object
            let crp = {
                challenge: `${uuidv4()}`,
                response: FIXED_CRP_RESPONSE_UUID
            };

            // Add the JSON object to the array
            crpList.push(crp);
        }
        return crpList;
    }

    async cleanupWorkloadModule() {
        console.log(`Worker-RetailDrugWorkload ${this.workerIndex}: Tx: ${this.txIndex} processed ${this.processedTagIds.length} tagIds: ${this.processedTagIds}`)
    }
}

function createWorkloadModule() {
    return new RetailDrugWorkload();
}

module.exports.createWorkloadModule = createWorkloadModule;
