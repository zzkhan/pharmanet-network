/*
Copyright 2021 IBM All Rights Reserved.

SPDX-License-Identifier: Apache-2.0
*/

package main

import (
	"encoding/json"
	"fmt"
	"github.com/hyperledger/fabric-gateway/pkg/client"
)

type DistributorClient struct {
	client *PharmaClient
}

func NewDistributorClient() *DistributorClient {
	return &DistributorClient{
		client: NewPharmaClient(distributor),
	}
}
func (m *DistributorClient) createPo(seller string, drugName string) {
	fmt.Printf("\n--> createPo, creates a Po for buying drug name %s from seller %s \n", drugName, seller)
	res, err := m.client.getContract("drug").Submit("drug-transfer:createDrugPO",
		client.WithArguments(seller, drugName))
	if err != nil {
		panic(fmt.Errorf("failed to submit transaction: %w", err))
	}
	fmt.Printf("*** Transaction committed successfully %s\n", res)
}
func (m *DistributorClient) submitDrugVerificationCrps(drugName string, tagId string, verificationCrps []Crp) {
	fmt.Printf("\n--> submitDrugVerificationCrps, submits verification CRPs for drug name %s with tagId %s\n", drugName, tagId)
	marshalled, err := json.Marshal(verificationCrps)
	transientData := map[string][]byte{
		"verification-crps": marshalled,
	}
	res, err := m.client.getContract("drug").Submit("drug-verification:submitDrugVerificationCrps",
		client.WithArguments(drugName, tagId),
		client.WithTransient(transientData),
	)
	if err != nil {
		panic(fmt.Errorf("failed to submit transaction: %w", err))
	}
	fmt.Printf("*** Transaction committed successfully %s\n", res)
}
func (m *DistributorClient) getChallenges(drugName string, tagId string) []Challenge {
	res, err := m.client.getContract("drug").Evaluate("drug-verification:getDrugChallenges",
		client.WithArguments(drugName, tagId))

	if err != nil {
		panic(fmt.Errorf("failed to submit transaction: %w", err))
	}
	fmt.Printf("*** getChallenges Transaction committed successfully %s\n", res)

	var challenges []Challenge
	err = json.Unmarshal(res, &challenges)
	return challenges
}

func (m *DistributorClient) verifyCrps(drugName string, tagId string, verificationCrps []Crp) VerificationOutcome {
	marshalled, err := json.Marshal(verificationCrps)
	crpData := map[string][]byte{
		"verification-crps": marshalled,
	}
	res, err := m.client.getContract("drug").Submit("drug-verification:verifyDrugCrps",
		client.WithArguments(drugName, tagId),
		client.WithTransient(crpData),
	)
	if err != nil {
		panic(fmt.Errorf("failed to submit transaction: %w", err))
	}
	fmt.Printf("*** Transaction committed successfully %s\n", res)

	var outcome VerificationOutcome
	err = json.Unmarshal(res, &outcome)
	if err != nil {
		panic(fmt.Errorf("failed to unmarshall outcome response: %w", err))
	}

	return outcome
}

func (m *DistributorClient) createShipment(buyerOrg string, drugName string, tagId string, transporterOrg string) {
	fmt.Printf("\n--> createShipment, creates new shipment for buyer %s of drugname %s with tagId %s shipped by transporter %s \n", buyerOrg, drugName, tagId, transporterOrg)

	res, err := m.client.getContract("drug").Submit("drug-transfer:createDrugShipment",
		client.WithArguments(buyerOrg, drugName, tagId, transporterOrg))

	if err != nil {
		panic(fmt.Errorf("failed to submit transaction: %w", err))
	}
	fmt.Printf("*** Transaction committed successfully %s\n", res)
}
