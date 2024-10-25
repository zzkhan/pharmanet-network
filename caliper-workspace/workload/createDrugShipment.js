'use strict';

const {WorkloadModuleBase} = require('@hyperledger/caliper-core');
const {getResult} = require('./utils');

class CreateDrugShipmentWorkload extends WorkloadModuleBase {
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
        this.drugNameIndex = Math.max(this.workerIndex * drugNamesPerWorker, 0)
        this.drugsInShipment = roundArguments.drugsPerShipment

        this.requestingOrg = roundArguments.requestingOrg;
        this.requestingIdentity = roundArguments.requestingIdentity;
        this.buyerOrg = roundArguments.buyerOrg;
        if (this.workerIndex === (totalWorkers - 1)) {
            this.drugNameEndIndex = (this.drugNameIndex) + (drugNamesPerWorker - 1) + (this.totalDrugNames % totalWorkers)
        } else {
            this.drugNameEndIndex = (this.drugNameIndex) + (drugNamesPerWorker - 1)
        }

        console.log(`Worker ${this.workerIndex}: Tx: ${this.txIndex}  startIndex ${this.drugNameIndex} end index: ${this.drugNameEndIndex}`);
    }

    async submitTransaction() {
        try {
            this.txIndex++;

            const drugName = `drug_1_${this.drugNameIndex}`;
            const buyer = this.buyerOrg;
            const transporter = `Org2MSP`;
            const requestingIdentity = this.requestingIdentity;
            const requestingOrg = this.requestingOrg;

            let tagIds = ''
            for (let i = 0; i < this.drugsInShipment; i++) {
                tagIds += `TAG_ID_1_${this.drugNameIndex}_${i}`;
                if (i < this.drugsInShipment - 1) {
                    tagIds += ','

                }
            }
            console.log(`Worker ${this.workerIndex}: Creating drug shipment with drugName: ${drugName} and tags ${tagIds}`);
            const createShipmentRequest = {
                contractId: this.roundArguments.contractId,
                contractFunction: 'drug-transfer:createDrugShipment',
                invokerIdentity: `${requestingIdentity}`,
                invokerMspId: `${requestingOrg}`,
                contractArguments: [buyer, drugName, tagIds, transporter],
                readOnly: false,
                timeout: 60
            };

            this.drugNameIndex++
            if (this.drugNameIndex > this.drugNameEndIndex) {
                console.warn(`drugNameIndex value [${this.drugNameIndex}] has exceeded max value of ${this.drugNameEndIndex}`)
            }

            const createShipmentResponse = await this.sutAdapter.sendRequests(createShipmentRequest);
            const createShippingJson = getResult(createShipmentResponse)
            console.log(`createShipmentResponse: ${createShippingJson}`)
            this.processedTagIds.push(drugName)
        }catch (e) {
            console.error(`createShipment - Worker ${this.workerIndex}: Tx: ${this.txIndex} - failed on drug ${drugName}`)
        }
    }

    async cleanupWorkloadModule() {
        console.log(`CreateDrugShipmentWorkload ${this.workerIndex}: Tx: ${this.txIndex} processed ${this.processedTagIds.length} drugNames: ${this.processedTagIds}`)
    }
}

function createWorkloadModule() {
    return new CreateDrugShipmentWorkload();
}

module.exports.createWorkloadModule = createWorkloadModule;
