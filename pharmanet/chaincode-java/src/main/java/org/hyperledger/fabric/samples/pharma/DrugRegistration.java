/*
 * SPDX-License-Identifier: Apache-2.0
 */

package org.hyperledger.fabric.samples.pharma;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import lombok.extern.slf4j.Slf4j;
import org.hyperledger.fabric.contract.Context;
import org.hyperledger.fabric.contract.ContractInterface;
import org.hyperledger.fabric.contract.annotation.Contract;
import org.hyperledger.fabric.contract.annotation.Default;
import org.hyperledger.fabric.contract.annotation.Info;
import org.hyperledger.fabric.contract.annotation.Transaction;
import org.hyperledger.fabric.samples.pharma.adapter.DrugAdapter;
import org.hyperledger.fabric.samples.pharma.adapter.DrugCrpAdapter;
import org.hyperledger.fabric.samples.pharma.adapter.PharmaOrgAdapter;
import org.hyperledger.fabric.samples.pharma.helper.DigestGenerator;
import org.hyperledger.fabric.samples.pharma.helper.DrugHelper;
import org.hyperledger.fabric.samples.pharma.helper.PharmaAssetKeyHelper;
import org.hyperledger.fabric.samples.pharma.model.CreateDrugPayload;
import org.hyperledger.fabric.samples.pharma.model.Drug;
import org.hyperledger.fabric.samples.pharma.model.DrugCrp;
import org.hyperledger.fabric.samples.pharma.model.PharmaErrors;
import org.hyperledger.fabric.samples.pharma.model.PharmaOrg;
import org.hyperledger.fabric.samples.pharma.model.PharmaOrgRoles;
import org.hyperledger.fabric.samples.pharma.model.PharmaOrgs;
import org.hyperledger.fabric.shim.ChaincodeException;
import org.hyperledger.fabric.shim.ext.sbe.StateBasedEndorsement;
import org.hyperledger.fabric.shim.ext.sbe.impl.StateBasedEndorsementFactory;
import org.hyperledger.fabric.shim.ledger.CompositeKey;

import java.nio.charset.StandardCharsets;
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
  private final ObjectMapper objectMapper;

  public DrugRegistration() {
    objectMapper = new ObjectMapper();
    objectMapper.registerModule(new JavaTimeModule());
    objectMapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
    objectMapper.setSerializationInclusion(JsonInclude.Include.NON_NULL);
  }
  private static final String IMPLICIT_COLLECTION_PREFIX = "_implicit_org_";

  @Transaction(intent = Transaction.TYPE.SUBMIT)
  public void InitLedger(final Context ctx) {
    log.info("Initialising PharmaNet organisations");

    var pharmanetOrgs = List.of(PharmaOrg.builder()
            .name(PharmaOrgs.MANUFACTURER.getMspId())
            .role(PharmaOrgRoles.MANUFACTURER)
            .implicitCollectionName(IMPLICIT_COLLECTION_PREFIX + PharmaOrgs.MANUFACTURER.getMspId())
            .build(),
    PharmaOrg.builder()
            .name(PharmaOrgs.TRANSPORTER.getMspId())
            .role(PharmaOrgRoles.TRANSPORTER)
            .implicitCollectionName(IMPLICIT_COLLECTION_PREFIX + PharmaOrgs.TRANSPORTER.getMspId())
            .build(),
    PharmaOrg.builder()
            .name(PharmaOrgs.DISTRIBUTOR.getMspId())
            .role(PharmaOrgRoles.DISTRIBUTOR)
            .crpSharedCollectionName("Org1MSPOrg3MSPCRPCollection")
            .implicitCollectionName(IMPLICIT_COLLECTION_PREFIX + PharmaOrgs.DISTRIBUTOR.getMspId())
            .build(),
    PharmaOrg.builder()
            .name(PharmaOrgs.PHARMACY.getMspId())
            .role(PharmaOrgRoles.RETAILER)
            .crpSharedCollectionName("Org1MSPOrg4MSPCRPCollection")
            .implicitCollectionName(IMPLICIT_COLLECTION_PREFIX + PharmaOrgs.PHARMACY.getMspId())
            .build(),
    PharmaOrg.builder()
            .name(PharmaOrgs.CONSUMER.getMspId())
            .role(PharmaOrgRoles.CONSUMER)
            .implicitCollectionName(IMPLICIT_COLLECTION_PREFIX + PharmaOrgs.CONSUMER.getMspId())
            .crpSharedCollectionName("Org1MSPOrg5MSPCRPCollection")
            .build());

    pharmanetOrgs.forEach(org -> ctx.getStub().putStringState(PharmaAssetKeyHelper.orgKey(ctx, org.getName()).toString(), PharmaOrgAdapter.serialize(org)));
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

    var requestingOrg = PharmaOrgRepository.getOrg(ctx, ctx.getClientIdentity().getMSPID());

    DrugHelper.verifyRequestorRole(requestingOrg, PharmaOrgRoles.MANUFACTURER,"RegisterDrug");

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

    byte[] drugCrpHash = DigestGenerator.computeSHA256Hash(drugCrpBytes);
    var newDrug = Drug.builder()
            .tagId(createDrugPayload.getTagId())
            .name(createDrugPayload.getDrugName())
            .manufactureDate(createDrugPayload.getManufactureDate())
            .expiryDate(createDrugPayload.getExpiryDate())
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
