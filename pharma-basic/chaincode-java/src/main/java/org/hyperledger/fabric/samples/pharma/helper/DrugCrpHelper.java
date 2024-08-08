package org.hyperledger.fabric.samples.pharma.helper;

import lombok.extern.slf4j.Slf4j;
import org.hyperledger.fabric.contract.Context;
import org.hyperledger.fabric.samples.pharma.adapter.DrugCrpAdapter;
import org.hyperledger.fabric.samples.pharma.model.DrugCrp;
import org.hyperledger.fabric.shim.ChaincodeException;
import org.hyperledger.fabric.shim.ledger.CompositeKey;
import org.hyperledger.fabric.shim.ledger.KeyValue;
import org.hyperledger.fabric.shim.ledger.QueryResultsIterator;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import static org.apache.commons.lang3.StringUtils.isNotBlank;
import static org.hyperledger.fabric.samples.pharma.model.PharmaNamespaces.ASSIGNED_DRUG_CRPS;

@Slf4j
public class DrugCrpHelper {
  public static List<DrugCrp> getUnassignedCrps(Context ctx, String collectionName, String tagId, int numberOfCrps) {
    CompositeKey drugCrpKey = PharmaAssetKeyHelper.crpAssetKey(ctx, tagId);
    CompositeKey assignedCrpsPartialKey = ctx.getStub().createCompositeKey(ASSIGNED_DRUG_CRPS.getPrefix(), tagId);

    System.out.println("assignedCrpsPartialKey: " + assignedCrpsPartialKey);
    QueryResultsIterator<KeyValue> assignedCrpsResult = ctx.getStub().getPrivateDataByPartialCompositeKey(collectionName, assignedCrpsPartialKey);
    System.out.println("assignedCrpsResult: " + assignedCrpsResult.iterator());
    var assignedCrpChallenges = new ArrayList<String>();
    for (KeyValue result : assignedCrpsResult) {
      System.out.println("result.getStringValue():" + result.getStringValue());
      if(isNotBlank(result.getStringValue())) {
        var assignedCrp = DrugCrpAdapter.fromJson(result.getStringValue());
        assignedCrpChallenges.addAll(assignedCrp.stream().map(DrugCrp::getChallenge).collect(Collectors.toList()));
      }
    }
    System.out.println("assignedCrpChallenges: " + assignedCrpChallenges);

    String drugCrpsJson = ctx.getStub().getPrivateDataUTF8(collectionName, drugCrpKey.toString());
    System.out.println("drugCrpsJson: " + drugCrpsJson);
    List<DrugCrp> drugCrps = DrugCrpAdapter.fromJson(drugCrpsJson);

    List<DrugCrp> unassignedCrps = drugCrps
            .stream()
            .filter(crp -> !assignedCrpChallenges.contains(crp.getChallenge()))
            .limit(numberOfCrps)
            .collect(Collectors.toList());

    if (unassignedCrps.isEmpty() || unassignedCrps.size() < numberOfCrps) {
      log.error("unassignedCrps empty or not enuf");
      throw new ChaincodeException("Drug with tagId %s has insufficient unassigned CRPs.", tagId);
    }

    log.info("unassignedCrps: " + unassignedCrps);
    return unassignedCrps;
  }

  public static String saveAssignedCrps(Context ctx, String collectionName, String assignee, String tagId, List<DrugCrp> assignedCrps) {
    String key = PharmaAssetKeyHelper.assignedCrpsAssetKey(ctx, tagId, assignee).toString();
    String crpString = DrugCrpAdapter.serialize(assignedCrps);
    log.info("saving assigned crps with key {} : value {}", key, crpString);
    ctx.getStub().putPrivateData(collectionName, key, crpString);
    return key;
  }
}
