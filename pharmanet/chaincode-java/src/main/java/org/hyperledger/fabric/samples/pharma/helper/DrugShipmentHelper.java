package org.hyperledger.fabric.samples.pharma.helper;

import org.hyperledger.fabric.contract.Context;
import org.hyperledger.fabric.samples.pharma.adapter.DrugShipmentAdapter;
import org.hyperledger.fabric.samples.pharma.model.DrugShipment;
import org.hyperledger.fabric.shim.ledger.CompositeKey;

import static org.apache.commons.lang3.StringUtils.isNotBlank;

public class DrugShipmentHelper {
  public static DrugShipment getShipment(Context ctx, String buyer, String drugName) {
    CompositeKey shipmentKey = PharmaAssetKeyHelper.drugShipmentKey(ctx, buyer, drugName);
    String shipmentJson = ctx.getStub().getStringState(shipmentKey.toString());
    if(isNotBlank(shipmentJson)){
      return DrugShipmentAdapter.fromJson(shipmentJson);
    }
    return null;
  }

  public static String saveShipment(Context ctx, String drugName, DrugShipment shipment) {
    String shipmentJson = DrugShipmentAdapter.serialise(shipment);
    CompositeKey shipmentKey = PharmaAssetKeyHelper.drugShipmentKey(ctx, shipment.getBuyer(), drugName);
    ctx.getStub().putStringState(shipmentKey.toString(), shipmentJson);
    return shipmentKey.toString();
  }
}
