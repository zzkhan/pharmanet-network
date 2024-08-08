package org.hyperledger.fabric.samples.pharma.helper;

import org.hyperledger.fabric.contract.Context;
import org.hyperledger.fabric.samples.pharma.model.PharmaNamespaces;
import org.hyperledger.fabric.shim.ledger.CompositeKey;

public class PharmaAssetKeyHelper {
  public static CompositeKey crpAssetKey(Context ctx, String tagId) {
    return ctx.getStub().createCompositeKey(PharmaNamespaces.DRUG_CRPS.getPrefix(), tagId);
  }

  public static CompositeKey drugAssetKey(Context ctx, String name, String tagId) {
    return ctx.getStub().createCompositeKey(PharmaNamespaces.DRUG.getPrefix(), name, tagId);
  }

  public static CompositeKey crpChallengeAssetKey(Context ctx, String assigneeOrg, String tagId) {
    return ctx.getStub().createCompositeKey(PharmaNamespaces.DRUG_CRP_CHALLENGE.getPrefix(), assigneeOrg, tagId);
  }

  public static CompositeKey assignedCrpsAssetKey(Context ctx, String tagId, String assigneeOrg) {
    return ctx.getStub().createCompositeKey(PharmaNamespaces.ASSIGNED_DRUG_CRPS.getPrefix(), tagId, assigneeOrg);
  }

  public static CompositeKey drugVerificationCrpsKey(Context ctx, String verifierOrg, String tagId) {
    return ctx.getStub().createCompositeKey(PharmaNamespaces.DRUG_CRP_VERIFICATION.getPrefix(), verifierOrg, tagId);
  }

  public static CompositeKey drugCrpVerificationOutcome(Context ctx, String tagId, String verifierOrg) {
    return ctx.getStub().createCompositeKey(PharmaNamespaces.DRUG_CRP_VERIFICATION_OUTCOME.getPrefix(), tagId, verifierOrg);
  }

  public static CompositeKey drugPoKey(Context ctx, String buyerOrg, String drugName) {
    return ctx.getStub().createCompositeKey(PharmaNamespaces.DRUG_PO.getPrefix(), buyerOrg, drugName);
  }

  public static CompositeKey drugShipmentKey(Context ctx, String buyerOrg, String drugName) {
    return ctx.getStub().createCompositeKey(PharmaNamespaces.DRUG_SHIPMENT.getPrefix(), buyerOrg, drugName);
  }
}
