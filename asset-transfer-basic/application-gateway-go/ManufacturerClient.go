/*
Copyright 2021 IBM All Rights Reserved.

SPDX-License-Identifier: Apache-2.0
*/

package main

import (
	"bytes"
	"context"
	"encoding/json"
	"errors"
	"fmt"
	"github.com/hyperledger/fabric-gateway/pkg/client"
	"github.com/hyperledger/fabric-protos-go-apiv2/gateway"
	"google.golang.org/grpc/status"
)

type ManufacturerClient struct {
	client *PharmaClient
}

func NewManufacturerClient() *ManufacturerClient {
	return &ManufacturerClient{client: NewPharmaClient(manufacturer)}
}
func (m *ManufacturerClient) registerDrug(drugName string, tagId string, crps []Crp) {
	fmt.Printf("\n--> registerDrug, creates new drug with ID %s \n", tagId)

	marshal, err := json.Marshal(crps)
	if err != nil {
		panic(fmt.Errorf("marshalling failed: %w", err))
	}
	crpData := map[string][]byte{
		"crps": marshal,
	}
	newDrug := Drug{
		Name:    drugName,
		TagId:   tagId,
		MfgDate: "21/06/2024",
	}

	marshalledDrug, err := json.Marshal(newDrug)
	if err != nil {
		panic(fmt.Errorf("marshall error: %w", err))
	}

	res, err := m.client.getContract("drug").Submit("drug-registration:RegisterDrug",
		client.WithTransient(crpData),
		client.WithArguments(string(marshalledDrug)))

	if err != nil {
		panic(fmt.Errorf("failed to submit transaction: %w", err))
	}

	fmt.Printf("*** Transaction committed successfully %s\n", res)
}

func (m *ManufacturerClient) createShipment(buyerOrg string, drugName string, tagId string, transporterOrg string) {
	fmt.Printf("\n--> createShipment, creates new shipment for buyer %s of drugname %s with tagId %s shipped by transporter %s \n", buyerOrg, drugName, tagId, transporterOrg)

	res, err := m.client.getContract("drug").Submit("drug-transfer:createDrugShipment",
		client.WithArguments(buyerOrg, drugName, tagId, transporterOrg))

	if err != nil {
		panic(fmt.Errorf("failed to submit transaction: %w", err))
	}
	fmt.Printf("*** Transaction committed successfully %s\n", res)
}

func (m *ManufacturerClient) getUnassignedCrps(drugName string, tagId string) []Crp {
	res, err := m.client.getContract("drug").Evaluate("drug-verification:getUnassignedCrps",
		client.WithArguments(drugName, tagId))

	if err != nil {
		panic(fmt.Errorf("failed to submit transaction: %w", err))
	}
	fmt.Printf("*** Transaction committed successfully %s\n", res)
	var unassignedCrps []Crp
	err = json.Unmarshal(res, &unassignedCrps)
	return unassignedCrps
}

func (m *ManufacturerClient) getAssignedCrps(drugName string, tagId string, drugOwnerOrg string) []Crp {
	res, err := m.client.getContract("drug").Evaluate("drug-verification:getAssignedCrps",
		client.WithArguments(drugName, tagId, drugOwnerOrg))

	if err != nil {
		panic(fmt.Errorf("query getAssignedCrps failed: %w", err))
	}
	fmt.Printf("*** getAssignedCrps response %s\n", res)
	var unassignedCrps []Crp
	err = json.Unmarshal(res, &unassignedCrps)
	return unassignedCrps
}

func (m *ManufacturerClient) assignCrps(drugName string, tagId string, assigneeOrg string, assign []Crp) {
	fmt.Printf("\n--> assignCrps, assigned CRPs to assignee %s for drugname %s with tagId %s \n", assigneeOrg, drugName, tagId)

	marshal, err := json.Marshal(assign)
	if err != nil {
		panic(fmt.Errorf("failed to submit transaction: %w", err))
	}
	fmt.Printf("*** marshalled CRPs %s\n", marshal)
	transientData := map[string][]byte{
		"assignment-crps": marshal,
	}

	res, err := m.client.getContract("drug").Submit("drug-verification:assignCRPs",
		client.WithArguments(drugName, tagId, assigneeOrg),
		client.WithTransient(transientData),
	)

	if err != nil {
		panic(fmt.Errorf("failed to submit transaction: %w", err))
	}
	fmt.Printf("*** Transaction committed successfully %s\n", res)
}

func (m *ManufacturerClient) assignChallenges(drugName string, tagId string, assigneeOrg string, assignedCrps []Crp) {
	marshal, err := json.Marshal(assignedCrps)
	if err != nil {
		panic(fmt.Errorf("marshalling failed: %w", err))
	}
	fmt.Printf("*** marshalled CRPs %s\n", marshal)
	transientData := map[string][]byte{
		"assignment-crps": marshal,
	}

	res, err := m.client.getContract("drug").Submit("drug-verification:assignChallenges",
		client.WithArguments(drugName, tagId, assigneeOrg),
		client.WithTransient(transientData),
	)

	if err != nil {
		panic(fmt.Errorf("failed to submit transaction: %w", err))
	}
	fmt.Printf("*** Transaction committed successfully %s\n", res)
}

func (m *ManufacturerClient) shareAssignedCrps(drugName string, tagId string, drugOwnerOrg string, assignedCrps []Crp) {
	fmt.Printf("\n--> shareAssignedCrps, shares assigned CRPs with assignee %s for drugname %s with tagId %s \n", drugOwnerOrg, drugName, tagId)

	marshal, err := json.Marshal(assignedCrps)
	if err != nil {
		panic(fmt.Errorf("marshalling failed: %w", err))
	}
	transientData := map[string][]byte{
		"assigned-crps": marshal,
	}

	res, err := m.client.getContract("drug").Submit("drug-verification:shareAssignedCrps",
		client.WithArguments(drugName, tagId, drugOwnerOrg),
		client.WithTransient(transientData),
	)

	if err != nil {
		panic(fmt.Errorf("failed to submit transaction: %w", err))
	}
	fmt.Printf("*** Transaction committed successfully %s\n", res)
}

// This type of transaction would typically only be run once by an application the first time it was started after its
// initial deployment. A new version of the chaincode deployed later would likely not need to run an "init" function.
func readDrug(contract *client.Contract, serialNo string) {
	fmt.Println("\n--> Evaluate Transaction: readDrug")

	evaluateResult, err := contract.EvaluateTransaction("ReadDrug", serialNo)
	if err != nil {
		panic(fmt.Errorf("failed to evaluate transaction: %w", err))
	}
	result := formatJSON(evaluateResult)

	fmt.Printf("*** Result:%s\n", result)
}
func initLedger(contract *client.Contract) {
	fmt.Printf("\n--> Submit Transaction: InitLedger, function creates the initial set of assets on the ledger \n")

	_, err := contract.SubmitTransaction("InitLedger")
	if err != nil {
		panic(fmt.Errorf("failed to submit transaction: %w", err))
	}

	fmt.Printf("*** Transaction committed successfully\n")
}

// Evaluate a transaction to query ledger state.
func getAllAssets(contract *client.Contract) {
	fmt.Println("\n--> Evaluate Transaction: GetAllAssets, function returns all the current assets on the ledger")

	evaluateResult, err := contract.EvaluateTransaction("GetAllAssets")
	if err != nil {
		panic(fmt.Errorf("failed to evaluate transaction: %w", err))
	}
	result := formatJSON(evaluateResult)

	fmt.Printf("*** Result:%s\n", result)
}

// Submit a transaction synchronously, blocking until it has been committed to the ledger.
func transferDrug(contract *client.Contract, serialNumber string, newOwner string) {
	fmt.Printf("\n--> transferDrug, drug with ID %s to owner %s \n", serialNumber, newOwner)

	res, err := contract.Submit("TransferAsset",
		client.WithArguments(serialNumber, newOwner),
		client.WithEndorsingOrganizations("Org1MSP", newOwner))

	if err != nil {
		panic(fmt.Errorf("error transferring drug: %w", err))
	}

	fmt.Printf("*** Transaction committed successfully %s\n", res)
}

// Evaluate a transaction by assetID to query ledger state.
func readAssetByID(contract *client.Contract) {
	fmt.Printf("\n--> Evaluate Transaction: ReadAsset, function returns asset attributes\n")

	evaluateResult, err := contract.EvaluateTransaction("ReadAsset", assetId)
	if err != nil {
		panic(fmt.Errorf("failed to evaluate transaction: %w", err))
	}
	result := formatJSON(evaluateResult)

	fmt.Printf("*** Result:%s\n", result)
}

// Submit transaction asynchronously, blocking until the transaction has been sent to the orderer, and allowing
// this thread to process the chaincode response (e.g. update a UI) without waiting for the commit notification
func transferAssetAsync(contract *client.Contract) {
	fmt.Printf("\n--> Async Submit Transaction: TransferAsset, updates existing asset owner")

	submitResult, commit, err := contract.SubmitAsync("TransferAsset", client.WithArguments(assetId, "Mark"))
	if err != nil {
		panic(fmt.Errorf("failed to submit transaction asynchronously: %w", err))
	}

	fmt.Printf("\n*** Successfully submitted transaction to transfer ownership from %s to Mark. \n", string(submitResult))
	fmt.Println("*** Waiting for transaction commit.")

	if commitStatus, err := commit.Status(); err != nil {
		panic(fmt.Errorf("failed to get commit status: %w", err))
	} else if !commitStatus.Successful {
		panic(fmt.Errorf("transaction %s failed to commit with status: %d", commitStatus.TransactionID, int32(commitStatus.Code)))
	}

	fmt.Printf("*** Transaction committed successfully\n")
}

// Submit transaction, passing in the wrong number of arguments ,expected to throw an error containing details of any error responses from the smart contract.
func exampleErrorHandling(contract *client.Contract) {
	fmt.Println("\n--> Submit Transaction: UpdateAsset asset70, asset70 does not exist and should return an error")

	_, err := contract.SubmitTransaction("UpdateAsset", "asset70", "blue", "5", "Tomoko", "300")
	if err == nil {
		panic("******** FAILED to return an error")
	}

	fmt.Println("*** Successfully caught the error:")

	var endorseErr *client.EndorseError
	var submitErr *client.SubmitError
	var commitStatusErr *client.CommitStatusError
	var commitErr *client.CommitError

	if errors.As(err, &endorseErr) {
		fmt.Printf("Endorse error for transaction %s with gRPC status %v: %s\n", endorseErr.TransactionID, status.Code(endorseErr), endorseErr)
	} else if errors.As(err, &submitErr) {
		fmt.Printf("Submit error for transaction %s with gRPC status %v: %s\n", submitErr.TransactionID, status.Code(submitErr), submitErr)
	} else if errors.As(err, &commitStatusErr) {
		if errors.Is(err, context.DeadlineExceeded) {
			fmt.Printf("Timeout waiting for transaction %s commit status: %s", commitStatusErr.TransactionID, commitStatusErr)
		} else {
			fmt.Printf("Error obtaining commit status for transaction %s with gRPC status %v: %s\n", commitStatusErr.TransactionID, status.Code(commitStatusErr), commitStatusErr)
		}
	} else if errors.As(err, &commitErr) {
		fmt.Printf("Transaction %s failed to commit with status %d: %s\n", commitErr.TransactionID, int32(commitErr.Code), err)
	} else {
		panic(fmt.Errorf("unexpected error type %T: %w", err, err))
	}

	// Any error that originates from a peer or orderer node external to the createGateway will have its details
	// embedded within the gRPC status error. The following code shows how to extract that.
	statusErr := status.Convert(err)

	details := statusErr.Details()
	if len(details) > 0 {
		fmt.Println("Error Details:")

		for _, detail := range details {
			switch detail := detail.(type) {
			case *gateway.ErrorDetail:
				fmt.Printf("- address: %s, mspId: %s, message: %s\n", detail.Address, detail.MspId, detail.Message)
			}
		}
	}
}

// Format JSON data
func formatJSON(data []byte) string {
	var prettyJSON bytes.Buffer
	if err := json.Indent(&prettyJSON, data, "", "  "); err != nil {
		panic(fmt.Errorf("failed to parse JSON: %w", err))
	}
	return prettyJSON.String()
}
