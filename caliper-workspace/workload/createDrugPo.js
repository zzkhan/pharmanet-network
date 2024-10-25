'use strict';

const { WorkloadModuleBase } = require('@hyperledger/caliper-core');
const { getResult } = require('./utils');

class CreateDrugPoWorkload extends WorkloadModuleBase {
    constructor() {
        super();
        this.txIndex = 0;
        this.processedTagIds = [];
    }

    async initializeWorkloadModule(workerIndex, totalWorkers, roundIndex, roundArguments, sutAdapter, sutContext) {
        await super.initializeWorkloadModule(workerIndex, totalWorkers, roundIndex, roundArguments, sutAdapter, sutContext);
        this.workerIndex = workerIndex;
        this.totalDrugNames = roundArguments.drugNames;
        this.drugQty = roundArguments.drugInstances;
        let drugNamesPerWorker = Math.floor(this.totalDrugNames / totalWorkers);
        this.drugNameIndex = Math.max(this.workerIndex*drugNamesPerWorker,0)
        this.requestingOrg = roundArguments.requestingOrg;
        this.requestingIdentity = roundArguments.requestingIdentity;
        this.sellerOrg = roundArguments.sellerOrg;
        if (this.workerIndex === (totalWorkers - 1)) {
            this.drugNameEndIndex = (this.drugNameIndex) + (drugNamesPerWorker - 1) + (this.totalDrugNames % totalWorkers)
        } else {
            this.drugNameEndIndex = (this.drugNameIndex) + (drugNamesPerWorker - 1)
        }

        console.log(`Worker ${this.workerIndex}: Tx: ${this.txIndex}  startIndex ${this.drugNameIndex} end index: ${this.drugNameEndIndex}`);
    }

    async submitTransaction() {
        try {
            let drugName = `drug_1_${this.drugNameIndex}`;
            let seller = `${this.sellerOrg}`;
            let quantity = `${this.drugQty}`
            console.log(`Worker ${this.workerIndex}: Creating drug po with drugName: ${drugName}`);

            const createPoRequest = {
                contractId: this.roundArguments.contractId,
                contractFunction: 'drug-transfer:createDrugPO',
                invokerIdentity: `${this.requestingIdentity}`,
                invokerMspId: `${this.requestingOrg}`,
                contractArguments: [seller, drugName, quantity],
                readOnly: false,
                timeout: 60
            };

            this.drugNameIndex++
            if (this.drugNameIndex > this.drugNameEndIndex) {
                console.warn(`drugNameIndex value [${this.drugNameIndex}] has exceeded max value of ${this.drugNameEndIndex}`)
            }

            const createPoResponse = await this.sutAdapter.sendRequests(createPoRequest);
            let createPoJson = getResult(createPoResponse);
            this.processedTagIds.push(drugName)
        } catch (e) {
            console.error(`createPo - Worker ${this.workerIndex}: Tx: ${this.txIndex} - failed on drug ${drugName}`)
        }
    }

    async cleanupWorkloadModule() {
        console.log(`Worker-RegisterDrugWorkload ${this.workerIndex}: Tx: ${this.txIndex} processed ${this.processedTagIds.length} drugName: ${this.processedTagIds}`)
    }
}

function createWorkloadModule() {
    return new CreateDrugPoWorkload();
}

module.exports.createWorkloadModule = createWorkloadModule;
