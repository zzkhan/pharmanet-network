/*
Copyright 2021 IBM All Rights Reserved.

SPDX-License-Identifier: Apache-2.0
*/

package main

import (
	"fmt"
	"github.com/hyperledger/fabric-gateway/pkg/client"
)

type TransporterClient struct {
	client *PharmaClient
}

func NewTransporterClient() *TransporterClient {
	return &TransporterClient{
		client: NewPharmaClient(transporter),
	}
}

func (t *TransporterClient) updateShipment(buyer string, drugName string) {
	fmt.Printf("\n--> updateShipment, updates shipment for drug name %s destined to buyer %s \n", drugName, buyer)
	res, err := t.client.getContract("drug").Submit("drug-transfer:updateDrugShipment",
		client.WithArguments(buyer, drugName))
	if err != nil {
		panic(fmt.Errorf("failed to submit transaction: %w", err))
	}
	fmt.Printf("*** Transaction committed successfully %s\n", res)
}

//func main() {
//	distributor := NewDistributorClient()
//	drugName := "drug-009"
//
//	distributor.createPo(drugName, "Org1MSP")
//
//	//readDrug(contract, "drug-004")
//	//transferDrug(contract, tagId, "Org2MSP")
//	//initLedger(contract)
//	//getAllAssets(contract)
//	//createAsset(contract)
//	//readAssetByID(contract)
//	//transferAssetAsync(contract)
//	//exampleErrorHandling(contract)
//}
