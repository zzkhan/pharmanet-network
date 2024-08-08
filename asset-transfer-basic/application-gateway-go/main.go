package main

import "fmt"

func main() {
	//New Drug data
	drugName := "steriline-14"
	tagId := "drug-0014"
	drugCrps := []Crp{
		{Challenge: "c1", Response: "r1"},
		{Challenge: "c2", Response: "r2"},
		{Challenge: "c3", Response: "r3"},
		{Challenge: "c4", Response: "r4"},
		{Challenge: "c5", Response: "r5"},
		{Challenge: "c6", Response: "r6"},
	}

	//Org client apps
	manufacturer := NewManufacturerClient()
	transporter := NewTransporterClient()
	distributor := NewDistributorClient()
	pharmacy := NewPharmacyClient()

	//New drug Registration
	manufacturer.registerDrug(drugName, tagId, drugCrps)

	//===== DRUG TRANSFER ======//

	//Distributor purchase of drug from manufacturer
	distributor.createPo(manufacturer.client.connectionDetails.mspID, drugName)

	//Manufacturer creates and dispatches drug shipment via transporter
	manufacturer.createShipment(distributor.client.connectionDetails.mspID, drugName, tagId, transporter.client.connectionDetails.mspID)

	//Transporter updates drug shipment as delivered to distributor
	transporter.updateShipment(distributor.client.connectionDetails.mspID, drugName)

	//===== DRUG VERIFICATION ======//

	//Manufacturer assigns drug CRPs to distributor
	crpsToAssign := manufacturer.getUnassignedCrps(drugName, tagId)
	fmt.Printf("%+v\n", crpsToAssign)
	manufacturer.assignCrps(drugName, tagId, distributor.client.connectionDetails.mspID, crpsToAssign)

	//Manufacturer sends distributor challenge part of assigned CRPs
	assignedCrps := manufacturer.getAssignedCrps(drugName, tagId, distributor.client.connectionDetails.mspID)
	fmt.Printf("%+v\n", assignedCrps)
	manufacturer.assignChallenges(drugName, tagId, distributor.client.connectionDetails.mspID, crpsToAssign)

	//Distributor reads drup challenges from blockchain
	challenges := distributor.getChallenges(drugName, tagId)
	fmt.Printf("%+v\n", challenges)

	//Distributor inputs challenges to drug RFID tag, records responses and generates verification CRPs
	verificationCrps := []Crp{
		{Challenge: challenges[0].Value, Response: "r1"},
		{Challenge: challenges[1].Value, Response: "r2"},
	}
	fmt.Printf("submitting verification %+v\n", verificationCrps)
	//Distributor sends generated CRPs to blockchain
	distributor.submitDrugVerificationCrps(drugName, tagId, verificationCrps)

	//Manufacturer shares assigned CRPs with distributor
	assignedCrps = manufacturer.getAssignedCrps(drugName, tagId, distributor.client.connectionDetails.mspID)
	fmt.Printf("%+v\n", assignedCrps)
	manufacturer.shareAssignedCrps(drugName, tagId, distributor.client.connectionDetails.mspID, assignedCrps)

	//Distributor verifies CRPs to determine if drug is authentic
	verificationOutcome := distributor.verifyCrps(drugName, tagId, verificationCrps)
	fmt.Printf("%+v\n", verificationOutcome)

	//===== DRUG TRANSFER ======//

	//Distributor purchase of drug from manufacturer
	pharmacy.createPo(distributor.client.connectionDetails.mspID, drugName)

	//Manufacturer creates and dispatches drug shipment via transporter
	distributor.createShipment(pharmacy.client.connectionDetails.mspID, drugName, tagId, transporter.client.connectionDetails.mspID)

	//Transporter updates drug shipment as delivered to distributor
	transporter.updateShipment(pharmacy.client.connectionDetails.mspID, drugName)

	//===== DRUG VERIFICATION ======//

	//Manufacturer assigns drug CRPs to distributor
	crpsToAssign = manufacturer.getUnassignedCrps(drugName, tagId)
	fmt.Printf("%+v\n", crpsToAssign)
	manufacturer.assignCrps(drugName, tagId, pharmacy.client.connectionDetails.mspID, crpsToAssign)

	//Manufacturer sends distributor challenge part of assigned CRPs
	assignedCrps = manufacturer.getAssignedCrps(drugName, tagId, pharmacy.client.connectionDetails.mspID)
	fmt.Printf("%+v\n", assignedCrps)
	manufacturer.assignChallenges(drugName, tagId, pharmacy.client.connectionDetails.mspID, crpsToAssign)

	//Distributor reads drup challenges from blockchain
	challenges = pharmacy.getChallenges(drugName, tagId)
	fmt.Printf("%+v\n", challenges)

	//Distributor inputs challenges to drug RFID tag, records responses and generates verification CRPs
	verificationCrps = []Crp{
		{Challenge: challenges[0].Value, Response: "r3"},
		{Challenge: challenges[1].Value, Response: "r4"},
	}
	fmt.Printf("submitting verification %+v\n", verificationCrps)
	//Distributor sends generated CRPs to blockchain
	pharmacy.submitDrugVerificationCrps(drugName, tagId, verificationCrps)

	//Manufacturer shares assigned CRPs with distributor
	assignedCrps = manufacturer.getAssignedCrps(drugName, tagId, pharmacy.client.connectionDetails.mspID)
	fmt.Printf("%+v\n", assignedCrps)
	manufacturer.shareAssignedCrps(drugName, tagId, pharmacy.client.connectionDetails.mspID, assignedCrps)

	//Distributor verifies CRPs to determine if drug is authentic
	verificationOutcome = pharmacy.verifyCrps(drugName, tagId, verificationCrps)
	fmt.Printf("%+v\n", verificationOutcome)
}
