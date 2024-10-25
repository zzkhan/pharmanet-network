package org.hyperledger.fabric.samples.pharma;

import org.hyperledger.fabric.contract.ContractRouter;
import org.hyperledger.fabric.shim.ChaincodeServer;
import org.hyperledger.fabric.shim.ChaincodeServerProperties;
import org.hyperledger.fabric.shim.NettyChaincodeServer;

import java.io.IOException;

public class ContractBootstrap {
  private static final String CHAINCODE_SERVER_ADDRESS = "CHAINCODE_SERVER_ADDRESS";
  private static final String CORE_CHAINCODE_ID = "CORE_CHAINCODE_ID";
  public static final String CHAINCODE_TLS_DISABLED = "CHAINCODE_TLS_DISABLED";
  private static final String CHAINCODE_TLS_KEY = "CHAINCODE_TLS_KEY";
  private static final String CHAINCODE_TLS_CERT = "CHAINCODE_TLS_CERT";

  public static void main(String[] args) throws Exception {
    ChaincodeServerProperties chaincodeServerProperties = new ChaincodeServerProperties();

    final String chaincodeServerPort = System.getenv(CHAINCODE_SERVER_ADDRESS);
    if (chaincodeServerPort == null || chaincodeServerPort.isEmpty()) {
      throw new IOException("chaincode server port not defined in system env. for example 'CHAINCODE_SERVER_ADDRESS=0.0.0.0:9999'");
    }

//    final int port = Integer.parseInt(chaincodeServerPort.split(":")[1]);
//    chaincodeServerProperties.setPortChaincodeServer(port);

    final String coreChaincodeIdName = System.getenv(CORE_CHAINCODE_ID);
    if (coreChaincodeIdName == null || coreChaincodeIdName.isEmpty()) {
      throw new IOException("chaincode id not defined in system env. for example 'CORE_CHAINCODE_ID=externalcc:06d1d324e858751d6eb4211885e9fd9ff74b62cb4ffda2242277fac95d467033'");
    }

    String tlsDisabledString = System.getenv(CHAINCODE_TLS_DISABLED);
    if (tlsDisabledString == null) {
      tlsDisabledString = "true";
    }
    boolean tlsDisabled = Boolean.parseBoolean(tlsDisabledString);
    if (!tlsDisabled) {
      String ccTLSKeyFile = System.getenv(CHAINCODE_TLS_KEY);
      String ccTLSCertFile = System.getenv(CHAINCODE_TLS_CERT);

      // set values on the server properties
      chaincodeServerProperties.setTlsEnabled(true);
      chaincodeServerProperties.setKeyFile(ccTLSKeyFile);
      chaincodeServerProperties.setKeyCertChainFile(ccTLSCertFile);
    }

    ContractRouter contractRouter = new ContractRouter(new String[] {"-i", coreChaincodeIdName});
    ChaincodeServer chaincodeServer = new NettyChaincodeServer(contractRouter, chaincodeServerProperties);

    contractRouter.startRouterWithChaincodeServer(chaincodeServer);
  }
}
