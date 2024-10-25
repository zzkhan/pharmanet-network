package org.hyperledger.fabric.samples.pharma.helper;

import org.hyperledger.fabric.contract.Context;
import org.hyperledger.fabric.samples.pharma.adapter.DrugCrpChallengeAdapter;
import org.hyperledger.fabric.samples.pharma.model.DrugCrpChallenge;
import org.hyperledger.fabric.samples.pharma.model.PharmaOrg;
import org.hyperledger.fabric.shim.ledger.CompositeKey;

import java.util.Collections;
import java.util.List;

public class DrugChallengesHelper {
  public static List<DrugCrpChallenge> getDrugChallenges(Context ctx, PharmaOrg org, String tagId) {
    String drugCrpChallengeKey = PharmaAssetKeyHelper.crpChallengeAssetKey(ctx, org.getName(), tagId).toString();
    byte[] crpChallengeData = ctx.getStub().getPrivateData(org.getCrpSharedCollectionName(), drugCrpChallengeKey);
    if(crpChallengeData == null || crpChallengeData.length == 0) {
      return Collections.emptyList();
    }
    return DrugCrpChallengeAdapter.fromJson(new String(crpChallengeData));
  }

  public static void saveChallenges(Context ctx, PharmaOrg assigneeOrg, CompositeKey drugCrpChallengeKey, List<DrugCrpChallenge> crpChallenges) {
    String crpChallengesJson = DrugCrpChallengeAdapter.serialize(crpChallenges);
    ctx.getStub().putPrivateData(assigneeOrg.getCrpSharedCollectionName(), drugCrpChallengeKey.toString(), crpChallengesJson);
  }
}
