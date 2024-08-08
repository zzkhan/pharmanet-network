package main

import (
	"github.com/hyperledger/fabric-gateway/pkg/client"
	"github.com/hyperledger/fabric-gateway/pkg/identity"
	"google.golang.org/grpc"
	"time"
)

type PharmaClient struct {
	connectionDetails ConnectionDetails
	clientConn        *grpc.ClientConn
	id                *identity.X509Identity
	sign              identity.Sign
}

func NewPharmaClient(connectionDetails ConnectionDetails) *PharmaClient {
	return &PharmaClient{
		connectionDetails: connectionDetails,
		clientConn:        newGrpcConnection(connectionDetails),
		id:                newIdentity(connectionDetails),
		sign:              newSign(connectionDetails),
	}
}

func (m *PharmaClient) getContract(contractName string) (contract *client.Contract) {
	gw, err := m.createGateway()
	if err != nil {
		panic(err)
	}
	//defer gw.Close()

	return gw.GetNetwork(pharmaChannelName).GetContract(contractName)
}

func (m *PharmaClient) createGateway() (*client.Gateway, error) {
	gw, err := client.Connect(
		m.id,
		client.WithSign(m.sign),
		client.WithClientConnection(m.clientConn),
		// Default timeouts for different gRPC calls
		client.WithEvaluateTimeout(5*time.Second),
		client.WithEndorseTimeout(15*time.Second),
		client.WithSubmitTimeout(5*time.Second),
		client.WithCommitStatusTimeout(1*time.Minute),
	)
	return gw, err
}
