package chaincode

import (
	"encoding/json"
	"fmt"
	"github.com/hyperledger/fabric-contract-api-go/contractapi"
)

// SmartContract provides functions for managing an Asset
type SmartContract struct {
	contractapi.Contract
}

type CreateDrug struct {
	ID      string `json:"id"`
	Name    string `json:"name"`
	MfgDate string `json:"manufactureDate"`
}
type CreateDrugCrps struct {
	Data map[string]string `json:"data"`
}

const drugCRPCollection = "Org1MSPCRPCollection"

// CreateAsset issues a new asset to the world state with given details.
func (s *SmartContract) CreateAsset(ctx contractapi.TransactionContextInterface, createDrug CreateDrug) error {
	drugId := createDrug.ID
	s.verifyDrugDoesNotExist(ctx, drugId)

	mspId, err := s.GetSubmittingClientIdentity(ctx)
	if err != nil {
		return err
	}

	//Get CRPs from transient map
	transientMap, err := ctx.GetStub().GetTransient()
	if err != nil {
		return fmt.Errorf("error getting transient: %v", err)
	}
	transientCrpJSON, ok := transientMap["crps"]
	if !ok {
		return fmt.Errorf("crps not found in the transient map input")
	}

	fmt.Println("transient crp data : %+s", transientCrpJSON)

	var crpMapData map[string]string
	err = json.Unmarshal(transientCrpJSON, &crpMapData)

	fmt.Println("unmarshalled crp data : %+v", crpMapData)

	if err != nil {
		return fmt.Errorf("failed to unmarshal JSON: %v", err)
	}
	drugCrps, err := createNewDrugCrps(crpMapData)
	drugCrpsJSON, err := json.Marshal(drugCrps)

	fmt.Println("Adding createDrug CRP: %+v", drugCrps)
	fmt.Println("Adding createDrug CRP JSON: %s", drugCrpsJSON)

	err = ctx.GetStub().PutPrivateData(drugCRPCollection, drugId, drugCrpsJSON)
	if err != nil {
		return fmt.Errorf("failed to put asset into private data collecton: %v", err)
	}

	newDrug := Drug{
		ID:           createDrug.ID,
		Name:         createDrug.Name,
		Manufacturer: mspId,
		MfgDate:      createDrug.MfgDate,
		Owner:        mspId,
	}

	fmt.Printf("Creating createDrug: %+v\n", newDrug)
	drugJSON, err := json.Marshal(createDrug)
	return ctx.GetStub().PutState(drugId, drugJSON)
}

// ReadAsset returns the asset stored in the world state with given id.
func (s *SmartContract) ReadAsset(ctx contractapi.TransactionContextInterface, id string) (*Drug, error) {
	assetJSON, err := ctx.GetStub().GetState(id)
	if err != nil {
		return nil, fmt.Errorf("failed to read from world state: %v", err)
	}
	if assetJSON == nil {
		return nil, fmt.Errorf("the asset %s does not exist", id)
	}

	var asset Drug
	err = json.Unmarshal(assetJSON, &asset)
	if err != nil {
		return nil, err
	}

	return &asset, nil
}

// Create a PO - initiated by buyer (distributor, Retailer), drugID
// Create a shipment - drug, shipper, buyer
// Transfer drug upon shipment complete - initiated by shipper, should be endorsed by buyer

func (s *SmartContract) TransferAsset(ctx contractapi.TransactionContextInterface, drugId string, newOwnerMSP string) error {
	assetJSON, err := ctx.GetStub().GetState(drugId)
	if err != nil {
		return fmt.Errorf("failed to read from world state: %v", err)
	}
	if assetJSON == nil {
		return fmt.Errorf("the drugToTransfer %s does not exist", drugId)
	}

	var drugToTransfer Drug
	err = json.Unmarshal(assetJSON, &drugToTransfer)
	if err != nil {
		return err
	}

	mspId, err := s.GetSubmittingClientIdentity(ctx)
	if drugToTransfer.Owner != mspId {
		return fmt.Errorf("cannot transfer because %s is not the current owner of drug %s", mspId, drugId)
	}

	drugToTransfer.Owner = newOwnerMSP

	drugJSON, err := json.Marshal(drugToTransfer)
	return ctx.GetStub().PutState(drugId, drugJSON)
}

func (s *SmartContract) AllocateCRPs(ctx contractapi.TransactionContextInterface, drugId string, verifierMSP string) error {
	drugCRPs, _ := s.getDrugCRPs(ctx, drugCRPCollection, drugId)
	// select unused crps
	var unusedCrps []Crp
	for _, crp := range drugCRPs.Crps {
		if crp.Assignee == "" && !crp.Used {
			crp.Assignee = verifierMSP
			unusedCrps = append(unusedCrps, crp)
		}
	}
	// assign crps to user

	// get challenges for assigned crps
	// save crp state
	// save challenges into shared collection
	//raise en event?
	return nil
}

func (s *SmartContract) ReadAssetPrivateData(ctx contractapi.TransactionContextInterface, id string) (*DrugCrps, error) {
	//drugCrps, err := ctx.GetStub().GetPrivateData(drugCRPCollection, id)
	//if err != nil {
	//	return nil, fmt.Errorf("failed to read from world state: %v", err)
	//}
	//if drugCrps == nil {
	//	return nil, fmt.Errorf("the asset %s does not exist", id)
	//}
	//fmt.Printf("drug crps from collection %s", drugCrps)
	//var crps DrugCrps
	//err = json.Unmarshal(drugCrps, &crps)
	//if err != nil {
	//	return nil, err
	//}
	//fmt.Printf("crp for drug id %s is %v", id, crps)
	drugCRPs, _ := s.getDrugCRPs(ctx, drugCRPCollection, id)
	return drugCRPs, nil
}

// AssetExists returns true when asset with given ID exists in world state
func (s *SmartContract) AssetExists(ctx contractapi.TransactionContextInterface, id string) (bool, error) {
	assetJSON, err := ctx.GetStub().GetState(id)
	if err != nil {
		return false, fmt.Errorf("failed to read from world state: %v", err)
	}

	return assetJSON != nil, nil
}
