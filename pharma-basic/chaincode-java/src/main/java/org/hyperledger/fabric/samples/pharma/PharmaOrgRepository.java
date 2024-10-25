package org.hyperledger.fabric.samples.pharma;

import org.apache.commons.lang3.StringUtils;
import org.hyperledger.fabric.contract.Context;
import org.hyperledger.fabric.samples.pharma.adapter.PharmaOrgAdapter;
import org.hyperledger.fabric.samples.pharma.helper.PharmaAssetKeyHelper;
import org.hyperledger.fabric.samples.pharma.model.PharmaOrg;
import org.hyperledger.fabric.shim.ChaincodeException;
import org.hyperledger.fabric.shim.ledger.CompositeKey;

public class PharmaOrgRepository {
    public static PharmaOrg getOrg(Context ctx, String mspid) {
    CompositeKey orgKey = PharmaAssetKeyHelper.orgKey(ctx, mspid);
    String orgJson = ctx.getStub().getStringState(orgKey.toString());
    if(StringUtils.isNotBlank(orgJson)){
      return PharmaOrgAdapter.fromJson(orgJson);
    }
    throw new ChaincodeException(String.format("Org %s not found", mspid));
  }
}
