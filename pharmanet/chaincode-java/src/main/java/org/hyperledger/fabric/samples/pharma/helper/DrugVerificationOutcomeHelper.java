package org.hyperledger.fabric.samples.pharma.helper;

import lombok.extern.slf4j.Slf4j;
import org.hyperledger.fabric.contract.Context;
import org.hyperledger.fabric.samples.pharma.adapter.DrugCrpVerificationAdapter;
import org.hyperledger.fabric.samples.pharma.model.DrugCrpVerificationOutcome;
import org.hyperledger.fabric.samples.pharma.model.PharmaNamespaces;
import org.hyperledger.fabric.samples.pharma.model.PharmaOrg;
import org.hyperledger.fabric.shim.ledger.KeyValue;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.apache.commons.lang3.StringUtils.isNotBlank;


@Slf4j
public class DrugVerificationOutcomeHelper {
  public static String saveVerificationOutcome(Context ctx, DrugCrpVerificationOutcome outcome) {
    log.info("Saving outcome {}", outcome);

    var outcomeKey = PharmaAssetKeyHelper.drugCrpVerificationOutcome(ctx, outcome.getTagId(), outcome.getVerifierOrg());
    var outcomeJson = DrugCrpVerificationAdapter.serialise(outcome);
    log.info("persisting updatedOutcomes {} with key {}", outcomeJson, outcomeKey);
    ctx.getStub().putStringState(outcomeKey.toString(), outcomeJson);

    return outcomeKey.toString();
  }

  public static DrugCrpVerificationOutcome getVerificationOutcome(Context ctx, String tagId, String verifierOrgName) {
    var outcomeKey = PharmaAssetKeyHelper.drugCrpVerificationOutcome(ctx, tagId, verifierOrgName);
    var outcomeJson = ctx.getStub().getStringState(outcomeKey.toString());
    if (isNotBlank(outcomeJson)) {
      return DrugCrpVerificationAdapter.deserialise(outcomeJson);
    }
    return null;
  }

  public static List<DrugCrpVerificationOutcome> getVerificationOutcomes(Context ctx, String tagId) {
    var partialKey = ctx.getStub().createCompositeKey(PharmaNamespaces.DRUG_CRP_VERIFICATION_OUTCOME.getPrefix(), tagId);
    var outcomeIterator = ctx.getStub().getStateByPartialCompositeKey(partialKey.toString());
    var outcomes = new ArrayList<DrugCrpVerificationOutcome>();
    for (KeyValue keyValue : outcomeIterator) {
      outcomes.add(DrugCrpVerificationAdapter.deserialise(keyValue.getStringValue()));
    }
    return outcomes;
  }

  public static Optional<DrugCrpVerificationOutcome> findVerificationOutcomeByVerifier(Context ctx, String tagId, PharmaOrg verifyingOrg) {
    return getVerificationOutcomes(ctx, tagId)
            .stream()
            .filter(v -> v.getVerifierOrg().equals(verifyingOrg.getName()))
            .findFirst();
  }
}
