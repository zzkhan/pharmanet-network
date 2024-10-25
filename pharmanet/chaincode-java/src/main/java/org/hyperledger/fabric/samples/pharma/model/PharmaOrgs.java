package org.hyperledger.fabric.samples.pharma.model;

import lombok.Getter;

@Getter
public enum PharmaOrgs {
  MANUFACTURER("Org1MSP"),
  TRANSPORTER("Org2MSP"),
  DISTRIBUTOR("Org3MSP"),
  PHARMACY("Org4MSP"),
  CONSUMER("Org5MSP");

  private final String mspId;
  PharmaOrgs(String mspId) {
    this.mspId = mspId;
  }

}
