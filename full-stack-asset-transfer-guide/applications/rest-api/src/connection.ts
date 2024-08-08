import * as grpc from '@grpc/grpc-js';
import { connect, Contract, Identity, Signer, signers } from '@hyperledger/fabric-gateway';
import * as crypto from 'crypto';
import * as path from 'path';

import { promises as fs } from 'fs';
const channelName = envOrDefault('CHANNEL_NAME', 'mychannel');
const chaincodeName = envOrDefault('CHAINCODE_NAME', 'drug');
const mspId = envOrDefault('MSP_ID', 'Org1MSP');
//Local development and testing uncomment below code
const WORKSHOP_CRYPTO =envOrDefault('CRYPTO_PATH', path.resolve('/Users/zzkhan/go/src/github.com/zzkhan/fabric-samples/test-network/organizations/peerOrganizations/org1.example.com'));
const keyPath = WORKSHOP_CRYPTO + "/users/User1@org1.example.com/msp/keystore/priv_sk";
const certPath = WORKSHOP_CRYPTO + "/users/User1@org1.example.com/msp/signcerts/User1@org1.example.com-cert.pem"
const tlsCertPath = WORKSHOP_CRYPTO + "/peers/peer0.org1.example.com/tls/ca.crt";

// //kubenetes certificates file path
// const WORKSHOP_CRYPTO = "/etc/secret-volume/"
// const keyPath = WORKSHOP_CRYPTO + "keyPath";
// const certPath = WORKSHOP_CRYPTO + "certPath"
// const tlsCertPath = WORKSHOP_CRYPTO + "tlsCertPath";
console.log("keyPath " + keyPath);
console.log("certPath " + certPath);
console.log("tlsCertPath " + tlsCertPath);
const peerEndpoint = "dns:///localhost:7051";
const peerHostAlias = "peer0.org1.example.com";
export class Connection {
    public static contract: Contract;
    public init() {
        initFabric();
    }
}
async function initFabric(): Promise<void> {
    // The gRPC client connection should be shared by all Gateway connections to this endpoint.
    const client = await newGrpcConnection();

    const gateway = connect({
        client,
        identity: await newIdentity(),
        signer: await newSigner(),
        // Default timeouts for different gRPC calls
        evaluateOptions: () => {
            return { deadline: Date.now() + 5000 }; // 5 seconds
        },
        endorseOptions: () => {
            return { deadline: Date.now() + 15000 }; // 15 seconds
        },
        submitOptions: () => {
            return { deadline: Date.now() + 5000 }; // 5 seconds
        },
        commitStatusOptions: () => {
            return { deadline: Date.now() + 60000 }; // 1 minute
        },
    });

    try {
        // Get a network instance representing the channel where the smart contract is deployed.
        const network = gateway.getNetwork(channelName);

        // Get the smart contract from the network.
        const contract = network.getContract(chaincodeName);
        Connection.contract = contract;

        // Initialize a set of asset data on the ledger using the chaincode 'InitLedger' function.
        //        await initLedger(contract);


    } catch (e: any) {
        console.log('sample log');
        console.log(e.message);
    } finally {
        console.log('error log ');
        // gateway.close();
        // client.close();
    }
}
async function newGrpcConnection(): Promise<grpc.Client> {
    const tlsRootCert = await fs.readFile(tlsCertPath);
    const tlsCredentials = grpc.credentials.createSsl(tlsRootCert);
    return new grpc.Client(peerEndpoint, tlsCredentials, {
        'grpc.ssl_target_name_override': peerHostAlias,
    });
}

async function newIdentity(): Promise<Identity> {
    const credentials = await fs.readFile(certPath);
    return { mspId, credentials };
}

async function newSigner(): Promise<Signer> {
    //const files = await fs.readdir(keyDirectoryPath);
    // path.resolve(keyDirectoryPath, files[0]);
    const privateKeyPem = await fs.readFile(keyPath);
    const privateKey = crypto.createPrivateKey(privateKeyPem);
    return signers.newPrivateKeySigner(privateKey);
}
/**
 * envOrDefault() will return the value of an environment variable, or a default value if the variable is undefined.
 */
function envOrDefault(key: string, defaultValue: string): string {
    return process.env[key] || defaultValue;
}
