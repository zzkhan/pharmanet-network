package org.hyperledger.fabric.samples.pharma.model;

public class PharmaErrors {
  public static final PharmaErrors DRUG_ALREADY_EXISTS = new PharmaErrors("Drug %s with tagId %s already exists");
  public static final PharmaErrors ACTION_ON_ASSET_FORBIDDEN_FOR_NON_OWNER = new PharmaErrors("Action: s% not permitted as asset %s is not owned by %s");
  public static final PharmaErrors DRUG_VERIFICATION_FAILED_STATE = new PharmaErrors("Drug %s cannot be traded due to failed verification.");
  public static final PharmaErrors PO_NOT_FOUND = new PharmaErrors("Not PO found for drug %s, seller %s and buyer %s");
  public static final PharmaErrors PO_QUANTITY_NOT_FULLFILLABLE = new PharmaErrors("Not enough sellable %s drugs exists to fulfill requested quantity %s in PO %s");
  public static PharmaErrors ACTION_FORBIDDEN_FOR_ROLE = new PharmaErrors("%s are not permitted to execute action: %s");
  public static PharmaErrors CANNOT_SHIP_TO_ROLE = new PharmaErrors("%s can not receive drug shipments.");
  public static PharmaErrors ASSET_NOT_FOUND = new PharmaErrors("Asset %s with key %s not found in world state.");
  public static PharmaErrors DRUG_VERIFICATION_STATE_INVALID = new PharmaErrors("Drug %s must be verified by current owner %s before transfer.");
  public static PharmaErrors DRUG_ALREADY_RETAILED = new PharmaErrors("Drug %s has already been sold.");
  private final String messageTemplate;

  public PharmaErrors(String messageTemplate) {
    this.messageTemplate = messageTemplate;
  }
  public String message(String... args){
    return String.format(messageTemplate, args);
  }
}
