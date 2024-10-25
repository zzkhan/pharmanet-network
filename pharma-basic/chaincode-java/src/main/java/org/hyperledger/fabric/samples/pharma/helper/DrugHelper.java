package org.hyperledger.fabric.samples.pharma.helper;

import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.hyperledger.fabric.contract.Context;
import org.hyperledger.fabric.samples.pharma.adapter.DrugAdapter;
import org.hyperledger.fabric.samples.pharma.model.Drug;
import org.hyperledger.fabric.samples.pharma.model.DrugCrpVerificationOutcomeStatus;
import org.hyperledger.fabric.samples.pharma.model.DrugStatus;
import org.hyperledger.fabric.samples.pharma.model.PharmaErrors;
import org.hyperledger.fabric.samples.pharma.model.PharmaNamespaces;
import org.hyperledger.fabric.samples.pharma.model.PharmaOrg;
import org.hyperledger.fabric.samples.pharma.model.PharmaOrgRoles;
import org.hyperledger.fabric.shim.ChaincodeException;
import org.hyperledger.fabric.shim.ledger.CompositeKey;
import org.hyperledger.fabric.shim.ledger.KeyValue;
import org.hyperledger.fabric.shim.ledger.QueryResultsIterator;

import java.util.ArrayList;
import java.util.List;

import static org.hyperledger.fabric.samples.pharma.model.DrugStatus.ASSIGNED;
import static org.hyperledger.fabric.samples.pharma.model.DrugStatus.FOR_SALE;

@Slf4j
public class DrugHelper {
  public static void verifyDrugState(Context ctx, String drugName, String tagId, PharmaOrg expectedOwnerOrg, String intendedAction) {
    if (!DrugHelper.drugExists(ctx, drugName, tagId)) {
      String errorMessage = String.format("Drug %s-%s does not exist", drugName, tagId);
      log.error(errorMessage);
      throw new ChaincodeException(errorMessage,
              PharmaErrors.ASSET_NOT_FOUND.message("Drug", String.format("%s-%s", drugName, tagId)));
    }

    if(drugHasFailedVerifications(ctx, tagId)){
      String errorMessage = String.format("drug %s-%s has verification failures.", drugName, tagId);
      log.error(errorMessage);
      throw new ChaincodeException(errorMessage,
              PharmaErrors.DRUG_VERIFICATION_FAILED_STATE.message(String.format("%s-%s", drugName, tagId)));
    }

    if (!DrugHelper.isDrugOwner(ctx, expectedOwnerOrg, drugName, tagId)) {
      String errorMessage = String.format("Org %s does not own drug %s-%s", expectedOwnerOrg.getName(), drugName, tagId);
      log.error(errorMessage);
      throw new ChaincodeException(errorMessage,
              PharmaErrors.ACTION_ON_ASSET_FORBIDDEN_FOR_NON_OWNER.message(intendedAction, String.format("%s-%s", drugName, tagId), expectedOwnerOrg.getName()));
    }
  }

  public static boolean drugHasFailedVerifications(Context ctx, String tagId) {
    return DrugVerificationOutcomeHelper
            .getVerificationOutcomes(ctx, tagId).stream()
            .anyMatch(o -> DrugCrpVerificationOutcomeStatus.FAILED.equals(o.getStatus()));
  }
  public static boolean drugExists(Context ctx, String drugName, String tagId) {
    String assetJSON = readFromState(ctx, drugName, tagId);
    return (assetJSON != null && !assetJSON.isEmpty());
  }

  public static boolean drugExists(Context ctx, String drugName) {
    var totalSupply = getTotalSupply(ctx, drugName);
    return totalSupply > 0;
  }

  public static boolean ownsEnoughDrugQuantity(Context ctx, String ownedBy, String drugName, int quantity) {
    return getDrugs(ctx, drugName).stream()
            .filter(drug -> List.of(FOR_SALE, DrugStatus.ASSIGNED).contains(drug.getStatus()))
            .filter(drug -> drug.getOwner().equals(ownedBy))
            .count() >= quantity;
  }
  public static boolean isDrugOwner(Context ctx, PharmaOrg org, String drugName, String tagId) {
    Drug drug = getDrug(ctx, drugName, tagId);
    return drug.getOwner().equals(org.getName());
  }

  public static Drug getDrug(Context ctx, String drugName, String tagId) {
    return DrugAdapter.fromJson(readFromState(ctx, drugName, tagId));
  }

  private static String readFromState(Context ctx, String drugName, String tagId) {
    return ctx.getStub().getStringState(PharmaAssetKeyHelper.drugAssetKey(ctx, drugName, tagId).toString());
  }

  public static Drug saveDrug(Context ctx, Drug drug) {
    CompositeKey drugKey = PharmaAssetKeyHelper.drugAssetKey(ctx, drug.getName(), drug.getTagId());
    String drugJson = DrugAdapter.serialize(drug);
    ctx.getStub().putStringState(drugKey.toString(), drugJson);
    return drug;
  }

  public static void verifyRequestorRole(PharmaOrg requestingOrg, PharmaOrgRoles expectedRole, String intendedAction) {
    if (!expectedRole.equals(requestingOrg.getRole())) {
      log.warn("Org with role {} cannot execute action {}", requestingOrg.getRole(), intendedAction);
      throw new ChaincodeException(String.format("Action %s not allowed for role %s", intendedAction, requestingOrg.getRole()),
              PharmaErrors.ACTION_FORBIDDEN_FOR_ROLE.message(requestingOrg.getRole().toString(), intendedAction));
    }
  }

  private static int getTotalSupply(Context ctx, String drugName) {
    var totalSupply = 0;
    QueryResultsIterator<KeyValue> drugsByNameIterator = ctx.getStub().getStateByPartialCompositeKey(PharmaNamespaces.DRUG.getPrefix(), drugName);
    for (KeyValue result : drugsByNameIterator) {
      if (StringUtils.isNotBlank(result.getStringValue())) {
        var drug = DrugAdapter.fromJson(result.getStringValue());
        log.info("getTotalSupply - drug {}", drug);
        if(FOR_SALE == drug.getStatus() || ASSIGNED == drug.getStatus()) {
          totalSupply++;
        }
      }
    }
    return totalSupply;
  }

  private static ArrayList<Drug> getDrugs(Context ctx, String drugName) {
    QueryResultsIterator<KeyValue> drugsByNameIterator = ctx.getStub().getStateByPartialCompositeKey(PharmaNamespaces.DRUG.getPrefix(), drugName);
    var drugs = new ArrayList<Drug>();
    for (KeyValue result : drugsByNameIterator) {
      if (StringUtils.isNotBlank(result.getStringValue())) {
        drugs.add(DrugAdapter.fromJson(result.getStringValue()));
      }
    }
    return drugs;
  }
}
