package org.hyperledger.fabric.samples.pharma;

import lombok.extern.slf4j.Slf4j;
import org.hyperledger.fabric.contract.Context;
import org.hyperledger.fabric.contract.ContractInterface;
import org.hyperledger.fabric.contract.annotation.Contract;
import org.hyperledger.fabric.contract.annotation.Info;
import org.hyperledger.fabric.contract.annotation.Transaction;
import org.hyperledger.fabric.samples.pharma.adapter.DrugCrpAdapter;
import org.hyperledger.fabric.samples.pharma.event.DrugVerificationSubmissionEvent;
import org.hyperledger.fabric.samples.pharma.event.EventAdapter;
import org.hyperledger.fabric.samples.pharma.helper.DigestHelper;
import org.hyperledger.fabric.samples.pharma.helper.DrugChallengesHelper;
import org.hyperledger.fabric.samples.pharma.helper.DrugCrpHelper;
import org.hyperledger.fabric.samples.pharma.helper.DrugHelper;
import org.hyperledger.fabric.samples.pharma.helper.DrugVerificationOutcomeHelper;
import org.hyperledger.fabric.samples.pharma.helper.PharmaAssetKeyHelper;
import org.hyperledger.fabric.samples.pharma.model.DrugCrp;
import org.hyperledger.fabric.samples.pharma.model.DrugCrpChallenge;
import org.hyperledger.fabric.samples.pharma.model.DrugCrpVerificationOutcome;
import org.hyperledger.fabric.samples.pharma.model.DrugCrpVerificationOutcomeStatus;
import org.hyperledger.fabric.samples.pharma.model.PharmaErrors;
import org.hyperledger.fabric.samples.pharma.model.PharmaOrg;
import org.hyperledger.fabric.samples.pharma.model.PharmaOrgRoles;
import org.hyperledger.fabric.shim.ChaincodeException;
import org.hyperledger.fabric.shim.ext.sbe.StateBasedEndorsement;
import org.hyperledger.fabric.shim.ext.sbe.impl.StateBasedEndorsementFactory;

import java.security.MessageDigest;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

@Contract(
        name = "drug-verification",
        info = @Info(
                title = "Drug verification",
                description = "Drug verification",
                version = "0.0.1-SNAPSHOT"))
@Slf4j
public class DrugVerification implements ContractInterface {
  @Transaction(intent = Transaction.TYPE.EVALUATE)
  public DrugCrp[] getUnassignedCrps(final Context ctx, final String drugName, final String tagId) {
    PharmaOrg requestingOrg = PharmaOrgRepository.getOrg(ctx.getClientIdentity().getMSPID());

    verifyRequestorRole(requestingOrg, PharmaOrgRoles.MANUFACTURER, "getUnassignedCrps");

    String existingDrugKey = PharmaAssetKeyHelper.drugAssetKey(ctx, drugName, tagId).toString();
    if (!DrugHelper.drugExists(ctx, drugName, tagId)) {
      String errorMessage = String.format("Asset %s does not exist", tagId);
      log.error(errorMessage);
      throw new ChaincodeException(errorMessage, PharmaErrors.ASSET_NOT_FOUND.message("Drug", existingDrugKey));
    }

    //Check drug already sold?
    List<DrugCrp> unassignedCrps = DrugCrpHelper.getUnassignedCrps(ctx, requestingOrg.getImplicitCollectionName(), tagId, 2);
    log.info("returning unassigned CRPs {}", unassignedCrps);
    return unassignedCrps.toArray(new DrugCrp[0]);
  }

  @Transaction(intent = Transaction.TYPE.EVALUATE)
  public DrugCrp[] getAssignedCrps(final Context ctx, final String drugName, final String tagId, final String assignee) {
    PharmaOrg requestingOrg = PharmaOrgRepository.getOrg(ctx.getClientIdentity().getMSPID());
    PharmaOrg assigneeOrg = PharmaOrgRepository.getOrg(assignee);

    verifyRequestorRole(requestingOrg, PharmaOrgRoles.MANUFACTURER, "AssignCRPs");

    DrugHelper.verifyDrugState(ctx, drugName, tagId, assigneeOrg, "AssignCRPs");

    var assignedCrpsKey = PharmaAssetKeyHelper.assignedCrpsAssetKey(ctx, tagId, assigneeOrg.getName());
    String assignedCrpJson = ctx.getStub().getPrivateDataUTF8(requestingOrg.getImplicitCollectionName(), assignedCrpsKey.toString());
    List<DrugCrp> drugCrps = DrugCrpAdapter.fromJson(assignedCrpJson);

    return drugCrps.toArray(new DrugCrp[0]);
  }

  @Transaction(intent = Transaction.TYPE.EVALUATE)
  public DrugCrpChallenge[] getDrugChallenges(final Context ctx, String drugName, String tagId) {
    var requestingOrg = PharmaOrgRepository.getOrg(ctx.getClientIdentity().getMSPID());

    if (!DrugHelper.drugExists(ctx, drugName, tagId)) {
      throw new ChaincodeException(String.format("drug %s does not exist.", tagId));
    }

    if (!DrugHelper.isDrugOwner(ctx, requestingOrg, drugName, tagId)) {
      throw new ChaincodeException(String.format("org %s does not own drug.", requestingOrg.getName()));
    }

    var assignedDrugChallenges = DrugChallengesHelper.getDrugChallenges(ctx, requestingOrg, tagId);
    if (assignedDrugChallenges.isEmpty()) {
      String errorMessage = String.format("No CRP challenges assigned to org %s for drug %s-%s", requestingOrg.getName(), drugName, tagId);
      log.error(errorMessage);
      throw new ChaincodeException(errorMessage,
              PharmaErrors.ASSET_NOT_FOUND.message("DrugChallenges", PharmaAssetKeyHelper.crpChallengeAssetKey(ctx, requestingOrg.getName(), tagId).toString()));
    }

    log.info("returning challenges {}", assignedDrugChallenges);
    return assignedDrugChallenges.toArray(new DrugCrpChallenge[0]);
  }

  @Transaction(intent = Transaction.TYPE.SUBMIT)
  public String assignCRPs(final Context ctx, final String drugName, final String tagId, final String assignee) {
    PharmaOrg requestingOrg = PharmaOrgRepository.getOrg(ctx.getClientIdentity().getMSPID());
    PharmaOrg assigneeOrg = PharmaOrgRepository.getOrg(assignee);

    verifyRequestorRole(requestingOrg, PharmaOrgRoles.MANUFACTURER, "submitAssignCRPs");

    DrugHelper.verifyDrugState(ctx, drugName, tagId, assigneeOrg, "submitAssignCRPs");

    //Get assignment CRPs from transient data
    Map<String, byte[]> transientData = ctx.getStub().getTransient();
    byte[] assignmentCrpsBytes = transientData.get("assignment-crps");
    List<DrugCrp> crpsToAssign = DrugCrpAdapter.fromBytes(assignmentCrpsBytes);
    return DrugCrpHelper.saveAssignedCrps(ctx, requestingOrg.getImplicitCollectionName(), assigneeOrg.getName(), tagId, crpsToAssign);
  }

  @Transaction(intent = Transaction.TYPE.SUBMIT)
  public void assignChallenges(final Context ctx, final String drugName, final String tagId, final String assignee) {
    PharmaOrg requestingOrg = PharmaOrgRepository.getOrg(ctx.getClientIdentity().getMSPID());
    PharmaOrg assigneeOrg = PharmaOrgRepository.getOrg(assignee);

    verifyRequestorRole(requestingOrg, PharmaOrgRoles.MANUFACTURER, "assignChallenges");

    DrugHelper.verifyDrugState(ctx, drugName, tagId, assigneeOrg, "assignChallenges");

    //Get assignment CRPs from transient data
    Map<String, byte[]> transientData = ctx.getStub().getTransient();
    byte[] assignmentCrpsBytes = transientData.get("assignment-crps");

    var assignedDrugCrpsKey = PharmaAssetKeyHelper.assignedCrpsAssetKey(ctx, tagId, assigneeOrg.getName());
    var assignedCrpHash = ctx.getStub().getPrivateDataHash(requestingOrg.getImplicitCollectionName(), assignedDrugCrpsKey.toString());

    var assignmentCrpsBytesComputedHash = DigestHelper.computeSHA256Hash(assignmentCrpsBytes);

    if (!MessageDigest.isEqual(assignmentCrpsBytesComputedHash, assignedCrpHash)) {
      log.error("submitted CRPs do not match assigned CRPs.");
      throw new ChaincodeException("submitted CRPs do not match assigned CRPs.");
    }

    var assignmentCrps = DrugCrpAdapter.fromBytes(assignmentCrpsBytes);
    //Create challenges
    var crpChallenges = assignmentCrps.stream()
            .map(crp -> DrugCrpChallenge.builder()
                    .value(crp.getChallenge())
                    .build()
            )
            .collect(Collectors.toList());

    //Save challenges to shared collection
    var drugCrpChallengeKey = PharmaAssetKeyHelper.crpChallengeAssetKey(ctx, assigneeOrg.getName(), tagId);
    DrugChallengesHelper.saveChallenges(ctx, assigneeOrg, drugCrpChallengeKey, crpChallenges);

    //Create PENDING verification outcome with assigned CRP hash
    var drugCrpVerificationOutcome = DrugCrpVerificationOutcome.builder()
            .tagId(tagId)
            .assignedCrpsHash(assignedCrpHash)
            .verifierOrg(assigneeOrg.getName())
            .created(ctx.getStub().getTxTimestamp())
            .build();
    var outcomeKey = DrugVerificationOutcomeHelper.saveVerificationOutcome(ctx, drugCrpVerificationOutcome);

    //set key level endorsement on outcome to be manufacturer and verifier
    var stateBasedEndorsement = StateBasedEndorsementFactory.getInstance().newStateBasedEndorsement(new byte[0]);
    stateBasedEndorsement.addOrgs(StateBasedEndorsement.RoleType.RoleTypePeer, requestingOrg.getName(), assigneeOrg.getName());
    ctx.getStub().setStateValidationParameter(outcomeKey, stateBasedEndorsement.policy());
  }

  @Transaction(intent = Transaction.TYPE.SUBMIT)
  public void submitDrugVerificationCrps(final Context ctx, String drugName, String tagId) {
    PharmaOrg verifyingOrg = PharmaOrgRepository.getOrg(ctx.getClientIdentity().getMSPID());

    if (!DrugHelper.drugExists(ctx, drugName, tagId)) {
      throw new ChaincodeException(String.format("drug %s does not exist.", tagId));
    }
    if (!DrugHelper.isDrugOwner(ctx, verifyingOrg, drugName, tagId)) {
      throw new ChaincodeException(String.format("org %s does not own drug.", verifyingOrg.getName()));
    }

    var maybeVerificationOutcome = Optional.ofNullable(DrugVerificationOutcomeHelper.getVerificationOutcome(ctx, tagId, verifyingOrg.getName()));
    maybeVerificationOutcome.ifPresentOrElse(verificationOutcome -> {
      if (!verificationOutcome.getStatus().equals(DrugCrpVerificationOutcomeStatus.PENDING)) {
        throw new ChaincodeException(String.format("Existing outcome for drug tagId %s and org %s not in PENDING state.",
                tagId, verifyingOrg.getName()));
      }
      if (verificationOutcome.getAssignedCrpsHash() == null || verificationOutcome.getAssignedCrpsHash().length == 0) {
        throw new ChaincodeException(String.format("No CRPs have been assigned to org %s for drug %s.", verifyingOrg.getName(), tagId));
      }
    }, () -> {
      throw new ChaincodeException(String.format("No outcome found drug tagId %s and org %s.",
              tagId, verifyingOrg.getName()));
    });

    Map<String, byte[]> transientData = ctx.getStub().getTransient();
    byte[] verificationCrpsBytes = transientData.get("verification-crps");
    log.info("submitDrugVerificationCrps: Hash of VCRPs from transient data: {}", DigestHelper.computeSHA256Hash(verificationCrpsBytes));
    List<DrugCrp> verificationCrps = DrugCrpAdapter.fromBytes(verificationCrpsBytes);
    String verificationCrpsJson = DrugCrpAdapter.serialize(verificationCrps);
    String drugVerificationCrpsKey = PharmaAssetKeyHelper.drugVerificationCrpsKey(ctx, verifyingOrg.getName(), tagId).toString();
    ctx.getStub().putPrivateData(verifyingOrg.getImplicitCollectionName(), drugVerificationCrpsKey, verificationCrpsJson);

    ctx.getStub().setEvent("DRUG-VERIFICATION-CRP-SUBMITTED", EventAdapter.toBytes(DrugVerificationSubmissionEvent.builder().drugName(drugName).tagId(tagId).submitter(verifyingOrg.getName()).build()));
  }

  @Transaction(intent = Transaction.TYPE.SUBMIT)
  public void shareAssignedCrps(final Context ctx, String drugName, String tagId, String verifier) {
    PharmaOrg requestingOrg = PharmaOrgRepository.getOrg(ctx.getClientIdentity().getMSPID());
    PharmaOrg verifierOrg = PharmaOrgRepository.getOrg(verifier);

    verifyRequestorRole(requestingOrg, PharmaOrgRoles.MANUFACTURER, "shareAssignedCrps");

    DrugHelper.verifyDrugState(ctx, drugName, tagId, verifierOrg, "shareAssignedCrps");

    var maybeVerificationOutcome = Optional.ofNullable(DrugVerificationOutcomeHelper.getVerificationOutcome(ctx, tagId, verifierOrg.getName()));
    maybeVerificationOutcome.ifPresentOrElse(outcome -> {
      if (!DrugCrpVerificationOutcomeStatus.PENDING.equals(outcome.getStatus())) {
        log.error("CRP verification outcome status [{}] should be PENDING", outcome.getStatus());
        throw new ChaincodeException(String.format("Existing outcome for drug tagId %s and org %s not in SUBMITTED state.",
                tagId, verifierOrg.getName()));
      }

      var submittedVerificationCrpsKey = PharmaAssetKeyHelper.drugVerificationCrpsKey(ctx, verifierOrg.getName(), tagId).toString();
      byte[] submittedVerificationCrpsDataHash = ctx.getStub().getPrivateDataHash(verifierOrg.getImplicitCollectionName(), submittedVerificationCrpsKey);
      log.info("shareAssignedCrps: submittedVerificationCrpsDataHash: {}", submittedVerificationCrpsDataHash);

      if (submittedVerificationCrpsDataHash == null || submittedVerificationCrpsDataHash.length == 0) {
        log.error("verification CRPs for drug {} tagId {} have not been submitted by verifier org {}", drugName, tagId, verifierOrg.getName());
        throw new ChaincodeException(String.format("verification CRPs have not been submitted by verifier org %s yet.", verifierOrg.getName()));
      }

      Map<String, byte[]> transientData = ctx.getStub().getTransient();
      byte[] transientDataAssignedCrpsBytes = transientData.get("assigned-crps");
      byte[] transientDataAssignedCrpsBytesHash = DigestHelper.computeSHA256Hash(transientDataAssignedCrpsBytes);

      var assignedCrpsKey = PharmaAssetKeyHelper.assignedCrpsAssetKey(ctx, tagId, verifierOrg.getName()).toString();
      byte[] assignedCrpsDataHash = ctx.getStub().getPrivateDataHash(requestingOrg.getImplicitCollectionName(), assignedCrpsKey);

      if (!MessageDigest.isEqual(transientDataAssignedCrpsBytesHash, assignedCrpsDataHash)) {
        log.error("CRPs being shared for drug {} tagId {} does not match assigned CRPs hash.", drugName, tagId);
        throw new ChaincodeException("Assigned CRP data hashes do not match.");
      }

      List<DrugCrp> assignedCrps = DrugCrpAdapter.fromBytes(transientDataAssignedCrpsBytes);
      String assignedCrpsJson = DrugCrpAdapter.serialize(assignedCrps);
      ctx.getStub().putPrivateData(verifierOrg.getCrpSharedCollectionName(), assignedCrpsKey, assignedCrpsJson);

      outcome.setAssignedCrpsHash(assignedCrpsDataHash);
      outcome.setVerificationCrpsHash(submittedVerificationCrpsDataHash);
      outcome.setStatus(DrugCrpVerificationOutcomeStatus.SUBMITTED);

      DrugVerificationOutcomeHelper.saveVerificationOutcome(ctx, outcome);

    }, () -> {
      throw new ChaincodeException(String.format("No outcome found for drug %s-%s and org %s.",
              drugName, tagId, verifierOrg.getName()));
    });
  }

  @Transaction(intent = Transaction.TYPE.SUBMIT)
  public DrugCrpVerificationOutcome verifyDrugCrps(final Context ctx, String drugName, String tagId) {

    var verifierOrg = PharmaOrgRepository.getOrg(ctx.getClientIdentity().getMSPID());

    DrugHelper.verifyDrugState(ctx, drugName, tagId, verifierOrg, "verifyDrugCrps");

    var maybeVerificationOutcome = Optional.ofNullable(DrugVerificationOutcomeHelper.getVerificationOutcome(ctx, tagId, verifierOrg.getName()));
    return maybeVerificationOutcome.map(outcome -> {
      if (DrugCrpVerificationOutcomeStatus.FAILED.equals(outcome.getStatus())) {
        log.info("CRP verification outcome status is already FAILED");
        return outcome;
      } else if (!DrugCrpVerificationOutcomeStatus.SUBMITTED.equals(outcome.getStatus())) {
        log.error("CRP verification outcome status [{}] should be SUBMITTED", outcome.getStatus());
        throw new ChaincodeException(String.format("Existing outcome for drug tagId %s and org %s not in SUBMITTED state.",
                tagId, verifierOrg.getName()));
      }

      Map<String, byte[]> transientData = ctx.getStub().getTransient();
      byte[] transientDataVerificationCrpsBytes = transientData.get("verification-crps");
      byte[] transientDataVerificationCrpsHash = DigestHelper.computeSHA256Hash(transientDataVerificationCrpsBytes);

      if (!MessageDigest.isEqual(outcome.getVerificationCrpsHash(), transientDataVerificationCrpsHash)) {
        log.error("verification CRPs for drug {} tagId {} does not match with submitted verification CRP hash", drugName, tagId);
        throw new ChaincodeException("verification CRP does not match with hash in outcome.");
      }

      List<DrugCrp> verificationCrps = DrugCrpAdapter.fromBytes(transientDataVerificationCrpsBytes);

      var assignedCrpsKey = PharmaAssetKeyHelper.assignedCrpsAssetKey(ctx, tagId, verifierOrg.getName()).toString();
      byte[] assignedCrpsBytes = ctx.getStub().getPrivateData(verifierOrg.getCrpSharedCollectionName(), assignedCrpsKey);
      List<DrugCrp> assignedCrps = DrugCrpAdapter.fromBytes(assignedCrpsBytes);

      boolean crpComparisonResult = verificationCrps.stream().allMatch(assignedCrps::contains);

      if (crpComparisonResult) {
        log.info("CRP verification passed");
        outcome.setStatus(DrugCrpVerificationOutcomeStatus.SUCCESS);
      } else {
        log.info("CRP verification failed");
        outcome.setStatus(DrugCrpVerificationOutcomeStatus.FAILED);
      }

      outcome.setUpdated(ctx.getStub().getTxTimestamp());
      DrugVerificationOutcomeHelper.saveVerificationOutcome(ctx, outcome);

      return outcome;
    }).orElseThrow(() -> new ChaincodeException(String.format("No outcome found drug tagId %s and org %s.",
            tagId, verifierOrg.getName())));
  }

  private static void verifyRequestorRole(PharmaOrg requestingOrg, PharmaOrgRoles expectedRole, String intendedAction) {
    if (!expectedRole.equals(requestingOrg.getRole())) {
      log.warn("Org with role {} cannot execute action {}", requestingOrg.getRole(), intendedAction);
      throw new ChaincodeException(String.format("Action %s not allowed for role %s", intendedAction, requestingOrg.getRole()),
              PharmaErrors.ACTION_FORBIDDEN_FOR_ROLE.message(requestingOrg.getRole().toString(), intendedAction));
    }
  }

}
