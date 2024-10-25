'use strict';

const { WorkloadModuleBase } = require('@hyperledger/caliper-core');
const { getResult } = require('./utils');

class UpdateDrugShipmentWorkload extends WorkloadModuleBase {
    constructor() {
        super();
        this.txIndex = 0;
        this.processedTagIds = [];
    }

    async initializeWorkloadModule(workerIndex, totalWorkers, roundIndex, roundArguments, sutAdapter, sutContext) {
        await super.initializeWorkloadModule(workerIndex, totalWorkers, roundIndex, roundArguments, sutAdapter, sutContext);
        this.workerIndex = workerIndex;
        this.totalDrugNames = roundArguments.drugNames;
        let drugNamesPerWorker = Math.floor(this.totalDrugNames / totalWorkers);
        this.drugNameIndex = Math.max(this.workerIndex*drugNamesPerWorker,0)
        this.buyerOrg = roundArguments.buyerOrg
        if (this.workerIndex === (totalWorkers - 1)) {
            this.drugNameEndIndex = (this.drugNameIndex) + (drugNamesPerWorker - 1) + (this.totalDrugNames % totalWorkers)
        } else {
            this.drugNameEndIndex = (this.drugNameIndex) + (drugNamesPerWorker - 1)
        }
    }

    async submitTransaction() {
        let buyer = `${this.buyerOrg}`;
        let drugName = `drug_1_${this.drugNameIndex}`;
        const updateShipmentRequest = {
            contractId: this.roundArguments.contractId,
            contractFunction: 'drug-transfer:updateDrugShipment',
            invokerIdentity: 'transporter',
            invokerMspId: 'Org2MSP',
            contractArguments: [buyer, drugName],
            readOnly: false,
            timeout: 60
        };

        this.drugNameIndex++
        if(this.drugNameIndex > this.drugNameEndIndex){
            console.warn(`drugNameIndex value [${this.drugNameIndex}] has exceeded max value of ${this.drugNameEndIndex}`)
        }

        const updateShipmentResponse = await this.sutAdapter.sendRequests(updateShipmentRequest);
        let updateShippingJson = getResult(updateShipmentResponse)
        console.log(`updateShipmentResponse: ${updateShippingJson}`)
        this.processedTagIds.push(drugName)
    }

    async cleanupWorkloadModule() {
        console.log(`DrugShipmentWorkload ${this.workerIndex}: Tx: ${this.txIndex} processed ${this.processedTagIds.length} drugName: ${this.processedTagIds}`)
    }
}

function createWorkloadModule() {
    return new UpdateDrugShipmentWorkload();
}

module.exports.createWorkloadModule = createWorkloadModule;
