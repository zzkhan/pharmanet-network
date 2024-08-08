package main

import (
	"fmt"
	"time"
)

type ConnectionDetails struct {
	mspID        string
	cryptoPath   string
	certPath     string
	keyPath      string
	tlsCertPath  string
	peerEndpoint string
	gatewayPeer  string
}

const manufacturerCryptoPath = "../../test-network/organizations/peerOrganizations/org1.example.com"

var manufacturer = ConnectionDetails{
	mspID:        "Org1MSP",
	cryptoPath:   manufacturerCryptoPath,
	certPath:     manufacturerCryptoPath + "/users/User1@org1.example.com/msp/signcerts",
	keyPath:      manufacturerCryptoPath + "/users/User1@org1.example.com/msp/keystore",
	tlsCertPath:  manufacturerCryptoPath + "/peers/peer0.org1.example.com/tls/ca.crt",
	peerEndpoint: "dns:///localhost:7051",
	gatewayPeer:  "peer0.org1.example.com",
}

const transporterCryptoPath = "../../test-network/organizations/peerOrganizations/org2.example.com"

var transporter = ConnectionDetails{
	mspID:        "Org2MSP",
	cryptoPath:   transporterCryptoPath,
	certPath:     transporterCryptoPath + "/users/User1@org2.example.com/msp/signcerts",
	keyPath:      transporterCryptoPath + "/users/User1@org2.example.com/msp/keystore",
	tlsCertPath:  transporterCryptoPath + "/peers/peer0.org2.example.com/tls/ca.crt",
	peerEndpoint: "dns:///localhost:9051",
	gatewayPeer:  "peer0.org2.example.com",
}

const distributorCryptoPath = "../../test-network/organizations/peerOrganizations/org3.example.com"

var distributor = ConnectionDetails{
	mspID:        "Org3MSP",
	cryptoPath:   distributorCryptoPath,
	certPath:     distributorCryptoPath + "/users/User1@org3.example.com/msp/signcerts",
	keyPath:      distributorCryptoPath + "/users/User1@org3.example.com/msp/keystore",
	tlsCertPath:  distributorCryptoPath + "/peers/peer0.org3.example.com/tls/ca.crt",
	peerEndpoint: "dns:///localhost:11051",
	gatewayPeer:  "peer0.org3.example.com",
}

const pharmacyCryptoPath = "../../test-network/organizations/peerOrganizations/org4.example.com"

var pharmacy = ConnectionDetails{
	mspID:        "Org4MSP",
	cryptoPath:   pharmacyCryptoPath,
	certPath:     pharmacyCryptoPath + "/users/User1@org4.example.com/msp/signcerts",
	keyPath:      pharmacyCryptoPath + "/users/User1@org4.example.com/msp/keystore",
	tlsCertPath:  pharmacyCryptoPath + "/peers/peer0.org4.example.com/tls/ca.crt",
	peerEndpoint: "dns:///localhost:13051",
	gatewayPeer:  "peer0.org4.example.com",
}

const consumerCryptoPath = "../../test-network/organizations/peerOrganizations/org5.example.com"

var consumer = ConnectionDetails{
	mspID:        "Org5MSP",
	cryptoPath:   consumerCryptoPath,
	certPath:     consumerCryptoPath + "/users/User1@org5.example.com/msp/signcerts",
	keyPath:      consumerCryptoPath + "/users/User1@org5.example.com/msp/keystore",
	tlsCertPath:  consumerCryptoPath + "/peers/peer0.org5.example.com/tls/ca.crt",
	peerEndpoint: "dns:///localhost:15051",
	gatewayPeer:  "peer0.org5.example.com",
}

type Drug struct {
	Name    string `json:"drugName"`
	TagId   string `json:"tagId"`
	MfgDate string `json:"mfgDate"`
}

type Crp struct {
	Challenge string `json:"challenge"`
	Response  string `json:"response"`
}
type Challenge struct {
	Value string `json:"value"`
}

type VerificationOutcome struct {
	TagId string `json:tagId`
	VerifierOrg string `json:verifierOrg`
	Status string `json:status`
}
var now = time.Now()
var assetId = fmt.Sprintf("asset%d", now.Unix()*1e3+int64(now.Nanosecond())/1e6)

const pharmaChannelName = "mychannel"
