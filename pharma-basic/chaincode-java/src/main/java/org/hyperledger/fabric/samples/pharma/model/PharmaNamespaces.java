package org.hyperledger.fabric.samples.pharma.model;

public enum PharmaNamespaces {
  DRUG("org.pharma-network.pharma.drug."),
  DRUG_CRPS("org.pharma-network.pharma.drug-crps."),
  DRUG_PO("org.pharma-network.pharma.drug-po."),
  DRUG_SHIPMENT("org.pharma-network.pharma.drug-shipment."),
  DRUG_CRP_CHALLENGE("org.pharma-network.pharma.drug-crp-challenge."),
  DRUG_CRP_VERIFICATION("org.pharma-network.pharma.drug-crp-verification."),
  ASSIGNED_DRUG_CRPS("org.pharma-network.pharma.drug-crps-assigned."),
  DRUG_CRP_VERIFICATION_OUTCOME("org.pharma-network.pharma.drug-crp-verification-outcome.");

  private final String prefix;
  PharmaNamespaces(String prefix) {
    this.prefix = prefix;
  }

  public String getPrefix() {
    return prefix;
  }
}
