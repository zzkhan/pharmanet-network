package org.hyperledger.fabric.samples.pharma.helper;

import org.hyperledger.fabric.contract.Context;
import org.hyperledger.fabric.samples.pharma.adapter.DrugPoAdapter;
import org.hyperledger.fabric.samples.pharma.model.DrugPo;
import org.hyperledger.fabric.shim.ledger.CompositeKey;

import java.util.Optional;

import static org.apache.commons.lang3.StringUtils.isNotBlank;

public class DrugPoHelper {
  public static boolean poExists(Context ctx, String drugName, String seller, String buyer) {
    Optional<DrugPo> maybeDrugPo = Optional.ofNullable(getPo(ctx, drugName, buyer));
    return maybeDrugPo.map(drugPo -> {
      return drugPo.getSeller().equals(seller);
    }).orElse(false);
  }

  public static DrugPo getPo(Context ctx, String drugName, String buyer) {
    CompositeKey drugPoKey = PharmaAssetKeyHelper.drugPoKey(ctx, buyer, drugName);
    String drugPoJson = ctx.getStub().getStringState(drugPoKey.toString());
    if(isNotBlank(drugPoJson)) {
      return DrugPoAdapter.deserialise(drugPoJson);
    }
    return null;
  }
}
