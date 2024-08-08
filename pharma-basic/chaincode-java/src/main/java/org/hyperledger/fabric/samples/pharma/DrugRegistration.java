/*
 * SPDX-License-Identifier: Apache-2.0
 */

package org.hyperledger.fabric.samples.pharma;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.hyperledger.fabric.contract.Context;
import org.hyperledger.fabric.contract.ContractInterface;
import org.hyperledger.fabric.contract.annotation.Contract;
import org.hyperledger.fabric.contract.annotation.Default;
import org.hyperledger.fabric.contract.annotation.Info;
import org.hyperledger.fabric.contract.annotation.Transaction;
import org.hyperledger.fabric.samples.pharma.adapter.DrugAdapter;
import org.hyperledger.fabric.samples.pharma.adapter.DrugCrpAdapter;
import org.hyperledger.fabric.samples.pharma.helper.DigestHelper;
import org.hyperledger.fabric.samples.pharma.helper.DrugHelper;
import org.hyperledger.fabric.samples.pharma.helper.PharmaAssetKeyHelper;
import org.hyperledger.fabric.samples.pharma.model.CreateDrugPayload;
import org.hyperledger.fabric.samples.pharma.model.Drug;
import org.hyperledger.fabric.samples.pharma.model.DrugCrp;
import org.hyperledger.fabric.samples.pharma.model.PharmaErrors;
import org.hyperledger.fabric.samples.pharma.model.PharmaNamespaces;
import org.hyperledger.fabric.samples.pharma.model.PharmaOrgRoles;
import org.hyperledger.fabric.shim.ChaincodeException;
import org.hyperledger.fabric.shim.ext.sbe.StateBasedEndorsement;
import org.hyperledger.fabric.shim.ext.sbe.impl.StateBasedEndorsementFactory;
import org.hyperledger.fabric.shim.ledger.CompositeKey;
import org.hyperledger.fabric.shim.ledger.KeyValue;
import org.hyperledger.fabric.shim.ledger.QueryResultsIterator;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Contract(
        name = "drug-registration",
        info = @Info(
                title = "Drug Registration",
                description = "Drug registration",
                version = "0.0.1-SNAPSHOT"))
@Default
@Slf4j
public final class DrugRegistration implements ContractInterface {
  private final ObjectMapper objectMapper = new ObjectMapper();

  public DrugRegistration() {
    objectMapper.setSerializationInclusion(JsonInclude.Include.NON_NULL);
  }

  /**
   * Creates a new asset on the ledger.
   *
   * @param ctx the transaction context
   * @return the created asset
   */
  @Transaction(intent = Transaction.TYPE.SUBMIT)
  public Drug RegisterDrug(final Context ctx,
                         final String createDrugPayloadJson) {

    var requestingOrg = PharmaOrgRepository.getOrg(ctx.getClientIdentity().getMSPID());

    if (!PharmaOrgRoles.MANUFACTURER.equals(requestingOrg.getRole())) {
      log.error("Org type {} cannot register drugs in the network", requestingOrg.getRole());
      throw new ChaincodeException("Only MANUFACTURER can create drugs.",
              PharmaErrors.ACTION_FORBIDDEN_FOR_ROLE.message(requestingOrg.getRole().toString(), "CreateDrug"));
    }

    var createDrugPayload = toCreateDrugPayload(createDrugPayloadJson);

    if (DrugHelper.drugExists(ctx, createDrugPayload.getDrugName(), createDrugPayload.getTagId())) {
      log.error("Drug {} tagId {} is already registered in the network", createDrugPayload.getDrugName(), createDrugPayload.getTagId());
      throw new ChaincodeException(String.format("Drug with tagId %s already exists", createDrugPayload.getTagId()), PharmaErrors.DRUG_ALREADY_EXISTS.message(createDrugPayload.getDrugName(), createDrugPayload.getTagId()));
    }

    Map<String, byte[]> transientData = ctx.getStub().getTransient();
    byte[] drugCrpBytes = transientData.get("crps");
    log.info("received drug drugCrpBytes {}", new String(drugCrpBytes));
    List<DrugCrp> drugCrpList = DrugCrpAdapter.fromBytes(drugCrpBytes);
    log.info("received drug crps {}", drugCrpList);
    String crpListJson = DrugCrpAdapter.serialize(drugCrpList);
    String crpKey = PharmaAssetKeyHelper.crpAssetKey(ctx, createDrugPayload.getTagId()).toString();
    ctx.getStub().putPrivateData(requestingOrg.getImplicitCollectionName(), crpKey, crpListJson);

    byte[] drugCrpHash = DigestHelper.computeSHA256Hash(drugCrpBytes);
    var newDrug = Drug.builder()
            .tagId(createDrugPayload.getTagId())
            .name(createDrugPayload.getDrugName())
            .mfgDate(createDrugPayload.getMfgDate())
            .manufacturer(requestingOrg.getName())
            .owner(requestingOrg.getName())
            .crpHash(drugCrpHash)
            .build();

    //Save new drug
    CompositeKey drugKey = PharmaAssetKeyHelper.drugAssetKey(ctx, newDrug.getName(), newDrug.getTagId());
    DrugHelper.saveDrug(ctx, newDrug);

    //Set the manufacturer in key level endorsement on drug
    var stateBasedEndorsement = StateBasedEndorsementFactory.getInstance().newStateBasedEndorsement(new byte[0]);
    stateBasedEndorsement.addOrgs(StateBasedEndorsement.RoleType.RoleTypePeer, requestingOrg.getName());
    ctx.getStub().setStateValidationParameter(drugKey.toString(), stateBasedEndorsement.policy());

    ctx.getStub().setEvent("DRUG-REGISTRATION", DrugAdapter.serialize(newDrug).getBytes(StandardCharsets.UTF_8));

    return newDrug;
  }

  /**
   * Retrieves an asset with the specified ID from the ledger.
   *
   * @param ctx   the transaction context
   * @param tagId the ID of the asset
   * @return the asset found on the ledger if there was one
   */
  @Transaction(intent = Transaction.TYPE.EVALUATE)
  public Drug ReadDrug(final Context ctx, final String drugName, final String tagId) {

    if(!DrugHelper.drugExists(ctx, drugName, tagId)){
      String errorMessage = String.format("Asset %s does not exist", tagId);
      throw new ChaincodeException(errorMessage, PharmaErrors.ASSET_NOT_FOUND.message("Drug", String.format("%s-%s", drugName, tagId)));
    }

    return DrugHelper.getDrug(ctx, drugName, tagId);
  }

  /**
   * Retrieves an asset with the specified ID from the ledger.
   *
   * @param ctx   the transaction context
   * @return the asset found on the ledger if there was one
   */
  @Transaction(intent = Transaction.TYPE.EVALUATE)
  public Drug[] ReadAllDrugs(final Context ctx) {

    CompositeKey partialDrugsKey = ctx.getStub().createCompositeKey(PharmaNamespaces.DRUG.getPrefix());
    QueryResultsIterator<KeyValue> drugsIterator = ctx.getStub().getStateByPartialCompositeKey(partialDrugsKey);
    List<Drug> allDrugs = new ArrayList<>();
    for (KeyValue result : drugsIterator) {
      allDrugs.add(DrugAdapter.fromJson(result.getStringValue()));
    }

    log.info("returning all drugs {}", allDrugs);
    return allDrugs.toArray(new Drug[0]);
  }

  private CreateDrugPayload toCreateDrugPayload(String createDrugPayloadJson) {
    CreateDrugPayload createDrugPayload;
    try {
      createDrugPayload = objectMapper.readValue(createDrugPayloadJson, CreateDrugPayload.class);
    } catch (JsonProcessingException e) {
      throw new ChaincodeException(e);
    }
    return createDrugPayload;
  }
}
