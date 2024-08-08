/*
 * SPDX-License-Identifier: Apache-2.0
 */

package org.hyperledger.fabric.samples.pharma;

import lombok.extern.slf4j.Slf4j;
import org.hyperledger.fabric.contract.Context;
import org.hyperledger.fabric.contract.ContractInterface;
import org.hyperledger.fabric.contract.annotation.Contract;
import org.hyperledger.fabric.contract.annotation.Default;
import org.hyperledger.fabric.contract.annotation.Info;
import org.hyperledger.fabric.contract.annotation.Transaction;
import org.hyperledger.fabric.samples.pharma.adapter.DrugAdapter;
import org.hyperledger.fabric.samples.pharma.adapter.DrugPoAdapter;
import org.hyperledger.fabric.samples.pharma.adapter.DrugShipmentAdapter;
import org.hyperledger.fabric.samples.pharma.helper.DrugHelper;
import org.hyperledger.fabric.samples.pharma.helper.DrugPoHelper;
import org.hyperledger.fabric.samples.pharma.helper.DrugShipmentHelper;
import org.hyperledger.fabric.samples.pharma.helper.EventHelper;
import org.hyperledger.fabric.samples.pharma.helper.PharmaAssetKeyHelper;
import org.hyperledger.fabric.samples.pharma.model.Drug;
import org.hyperledger.fabric.samples.pharma.model.DrugPo;
import org.hyperledger.fabric.samples.pharma.model.DrugShipment;
import org.hyperledger.fabric.samples.pharma.model.DrugShipmentStatus;
import org.hyperledger.fabric.samples.pharma.model.DrugStatus;
import org.hyperledger.fabric.samples.pharma.model.PharmaErrors;
import org.hyperledger.fabric.shim.ChaincodeException;
import org.hyperledger.fabric.shim.ext.sbe.StateBasedEndorsement;
import org.hyperledger.fabric.shim.ext.sbe.impl.StateBasedEndorsementFactory;
import org.hyperledger.fabric.shim.ledger.CompositeKey;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import static org.hyperledger.fabric.samples.pharma.model.PharmaOrgRoles.DISTRIBUTOR;
import static org.hyperledger.fabric.samples.pharma.model.PharmaOrgRoles.MANUFACTURER;
import static org.hyperledger.fabric.samples.pharma.model.PharmaOrgRoles.RETAILER;
import static org.hyperledger.fabric.samples.pharma.model.PharmaOrgRoles.TRANSPORTER;

@Contract(
        name = "drug-transfer",
        info = @Info(
                title = "Drug Transfer",
                description = "Drug Transfer",
                version = "0.0.1-SNAPSHOT"))
@Default
@Slf4j
public final class DrugTransfer implements ContractInterface {

  /**
   * Creates a purchase order for a drug
   *
   * @param ctx
   * @param seller
   * @param drugName
   * @return
   */
  @Transaction(intent = Transaction.TYPE.SUBMIT)
  public DrugPo createDrugPO(final Context ctx, final String seller, final String drugName, final int quantity) {
    var buyerOrg = PharmaOrgRepository.getOrg(ctx.getClientIdentity().getMSPID());

    if (!List.of(DISTRIBUTOR, RETAILER).contains(buyerOrg.getRole())) {
      throw new ChaincodeException("Only DISTRIBUTOR or RETAILER can create POs.");
    }

    var sellerOrg = PharmaOrgRepository.getOrg(seller);
    if (!List.of(MANUFACTURER, DISTRIBUTOR).contains(sellerOrg.getRole())) {
      throw new ChaincodeException("Only MANUFACTURER or DISTRIBUTOR can be a seller.");
    }

    //Check if drug exists
    if (!DrugHelper.drugExists(ctx, drugName)) {
      throw new ChaincodeException(String.format("Drug %s does not exist.", drugName));
    }

    CompositeKey poKey = PharmaAssetKeyHelper.drugPoKey(ctx, buyerOrg.getName(), drugName);
    DrugPo newDrugPo = DrugPo.builder()
            .poId(poKey.toString())
            .drugName(drugName)
            .quantity(quantity)
            .seller(sellerOrg.getName())
            .buyer(buyerOrg.getName())
            .build();

    ctx.getStub().putStringState(poKey.toString(), DrugPoAdapter.serialise(newDrugPo));

    EventHelper.sendEvent(ctx, "DRUG-PO-CREATED", DrugPoAdapter.toBytes(newDrugPo));

    return newDrugPo;
  }

  /**
   * Creates a new shipment asset for a drug
   *
   * @param ctx         transaction context
   * @param drugName    name of drug to be shipped
   * @param tagIds      tag ID of drug to be shipped
   * @param buyer       org recipient of shipment
   * @param transporter org transporting shipment
   * @return drugShipment newly created shipment asset
   */
  @Transaction(intent = Transaction.TYPE.SUBMIT)
  public DrugShipment createDrugShipment(final Context ctx, final String buyer, final String drugName, final List<String> tagIds, final String transporter) {
    var sellerOrg = PharmaOrgRepository.getOrg(ctx.getClientIdentity().getMSPID());
    var buyerOrg = PharmaOrgRepository.getOrg(buyer);

    if (!List.of(MANUFACTURER, DISTRIBUTOR).contains(sellerOrg.getRole())) {
      throw new ChaincodeException("Only MANUFACTURER or DISTRIBUTOR can create shipments.",
              PharmaErrors.ACTION_FORBIDDEN_FOR_ROLE.message(sellerOrg.getRole().toString(), "CreateDrugShipment"));
    }

    var transporterOrg = PharmaOrgRepository.getOrg(transporter);
    if (!transporterOrg.getRole().equals(TRANSPORTER)) {
      throw new ChaincodeException("Shipments can only be created with a TRANSPORTER.",
              PharmaErrors.ACTION_FORBIDDEN_FOR_ROLE.message(transporterOrg.getRole().toString(), "CreateDrugShipment"));
    }

    var drugsToShip = new ArrayList<Drug>();
    tagIds.forEach(tagId -> {
      DrugHelper.verifyDrugState(ctx, drugName, tagId, sellerOrg, "createDrugShipment");
      drugsToShip.add(DrugHelper.getDrug(ctx, drugName, tagId));
    });

    if (!DrugPoHelper.poExists(ctx, drugName, sellerOrg.getName(), buyerOrg.getName())) {
      throw new ChaincodeException(String.format("Not PO found for drug %s, seller %s and buyer %s", drugName, sellerOrg.getName(), buyerOrg.getName()),
              PharmaErrors.PO_NOT_FOUND.message(drugName, sellerOrg.getName(), buyerOrg.getName()));
    }

    DrugPo drugPo = DrugPoHelper.getPo(ctx, drugName, buyerOrg.getName());
    if (!DrugHelper.ownsEnoughDrugQuantity(ctx, sellerOrg.getName(), drugName, drugPo.getQuantity())) {
      String errorMessage = String.format("Not enough sellable %s drugs exists to fulfill requested quantity %s in PO %s", drugName, drugPo.getQuantity(), drugPo.getPoId());
      throw new ChaincodeException(errorMessage,
              PharmaErrors.PO_QUANTITY_NOT_FULLFILLABLE.message(drugName, String.valueOf(drugPo.getQuantity()), drugPo.getPoId()));
    }

    var shipmentKey = PharmaAssetKeyHelper.drugShipmentKey(ctx, buyer, drugName);
    var newDrugShipment = DrugShipment.builder()
            .id(shipmentKey.toString())
            .seller(sellerOrg.getName())
            .buyer(buyer)
            .transporter(transporterOrg.getName())
            .drugName(drugName)
            .lineItems(drugsToShip.stream().map(Drug::getTagId).collect(Collectors.toList()))
            .created(ctx.getStub().getTxTimestamp())
            .build();

    ctx.getStub().putStringState(shipmentKey.toString(), DrugShipmentAdapter.serialise(newDrugShipment));

    //set key level endorsement for buyer org on shipment
    var stateBasedEndorsement = StateBasedEndorsementFactory.getInstance().newStateBasedEndorsement(new byte[0]);
    stateBasedEndorsement.addOrgs(StateBasedEndorsement.RoleType.RoleTypePeer, buyerOrg.getName(), transporterOrg.getName());
    ctx.getStub().setStateValidationParameter(shipmentKey.toString(), stateBasedEndorsement.policy());

    //transfer drug to transporter
    drugsToShip.forEach(drugToShip -> transferDrug(ctx, drugToShip.getName(), drugToShip.getTagId(), transporterOrg.getName()));

    return newDrugShipment;
  }

  /**
   * Updates a shipment for a drug
   *
   * @param ctx      transaction context
   * @param buyer    org recipient of shipment
   * @param drugName name of drug in shipment
   * @return DrugShipment - updated shipment
   */
  @Transaction(intent = Transaction.TYPE.SUBMIT)
  public DrugShipment updateDrugShipment(final Context ctx, final String buyer, final String drugName) {

    var requestingOrg = PharmaOrgRepository.getOrg(ctx.getClientIdentity().getMSPID());
    var buyerOrg = PharmaOrgRepository.getOrg(buyer);

    if (!TRANSPORTER.equals(requestingOrg.getRole())) {
      throw new ChaincodeException("Only transporters can update shipping status.",
              PharmaErrors.ACTION_FORBIDDEN_FOR_ROLE.message(requestingOrg.getName(), "updateDrugShipment"));
    }

    var drugShipment = Optional.ofNullable(DrugShipmentHelper.getShipment(ctx, buyer, drugName));

    return drugShipment.map(shipment -> {
              if (!shipment.getTransporter().equals(requestingOrg.getName())) {
                throw new ChaincodeException(String.format("transporter %s is not the handler of drug shipment %s.", requestingOrg.getName(), shipment.getId()),
                        PharmaErrors.ACTION_ON_ASSET_FORBIDDEN_FOR_NON_OWNER.message("updateDrugShipment", shipment.getId(), requestingOrg.getName()));
              }

              if (!shipment.getBuyer().equals(buyerOrg.getName())) {
                throw new ChaincodeException(String.format("Buyer %s is not the buyer of drug shipment %s.", buyerOrg.getName(), shipment.getId()));
              }

              if (!DrugShipmentStatus.IN_TRANSIT.equals(shipment.getStatus())) {
                throw new ChaincodeException(String.format("Drug shipment %s is not in transit.", shipment.getId()));
              }

              //Transfer drug to buyer
              shipment.getLineItems().stream()
                      .map(drugTagId -> DrugHelper.getDrug(ctx, drugName, drugTagId))
                      .forEach(drug -> transferDrug(ctx, drug.getName(), drug.getTagId(), buyerOrg.getName()));

              //Set shipment status
              shipment.setStatus(DrugShipmentStatus.DELIVERED);
              shipment.setUpdated(ctx.getStub().getTxTimestamp());
              DrugShipmentHelper.saveShipment(ctx, drugName, shipment);

              ctx.getStub().setEvent("DRUG-SHIPMENT-DELIVERED", DrugShipmentAdapter.toBytes(shipment));

              return shipment;
            })
            .orElseThrow(() -> new ChaincodeException(String.format("drug shipment for buyer %s and drug %s not found.", buyer, drugName)));
  }


  /**
   * Internal method to change the owner of a drug on the ledger.
   *
   * @param ctx      the transaction context
   * @param drugName name of drug to be transferred
   * @param tagId    the ID of the drug to be transferred
   * @param newOwner org whom drug is to be transferred to
   * @return Drug - updated drug
   */
  private Drug transferDrug(final Context ctx, final String drugName, final String tagId, final String newOwner) {
    var requestingOrg = PharmaOrgRepository.getOrg(ctx.getClientIdentity().getMSPID());
    var newOwnerOrg = PharmaOrgRepository.getOrg(newOwner);

    System.out.printf("transferDrug -> transferring drug %s-%s from current owner %s to new owner %s%n", drugName, tagId, requestingOrg.getName(), newOwnerOrg.getName());

    DrugHelper.verifyDrugState(ctx, drugName, tagId, requestingOrg, "transferDrug");

    var drugToTransfer = DrugHelper.getDrug(ctx, drugName, tagId);

    var updatedDrug = Drug.builder()
            .tagId(drugToTransfer.getTagId())
            .name(drugToTransfer.getName())
            .mfgDate(drugToTransfer.getMfgDate())
            .manufacturer(drugToTransfer.getManufacturer())
            .owner(newOwnerOrg.getName())
            .status(DrugStatus.ASSIGNED)
            .build();

    var drug = DrugHelper.saveDrug(ctx, updatedDrug);
    var drugKey = PharmaAssetKeyHelper.drugAssetKey(ctx, drug.getName(), drug.getTagId());

    //set key level endorsement for new owner for transferred drug
    var stateBasedEndorsement = StateBasedEndorsementFactory.getInstance().newStateBasedEndorsement(new byte[0]);
    stateBasedEndorsement.addOrgs(StateBasedEndorsement.RoleType.RoleTypePeer, newOwnerOrg.getName());
    ctx.getStub().setStateValidationParameter(drugKey.toString(), stateBasedEndorsement.policy());

    ctx.getStub().setEvent("DRUG-TRANSFERRED", DrugAdapter.serialize(drug).getBytes(StandardCharsets.UTF_8));

    return drug;
  }
}
