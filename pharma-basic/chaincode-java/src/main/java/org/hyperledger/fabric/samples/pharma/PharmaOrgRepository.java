package org.hyperledger.fabric.samples.pharma;

import org.hyperledger.fabric.samples.pharma.model.PharmaOrg;
import org.hyperledger.fabric.samples.pharma.model.PharmaOrgRoles;
import org.hyperledger.fabric.samples.pharma.model.PharmaOrgs;
import org.hyperledger.fabric.shim.ChaincodeException;

import java.util.HashMap;
import java.util.Map;

public class PharmaOrgRepository {
  private static final Map<PharmaOrgs, PharmaOrg> PHARMA_ORGS = new HashMap<>();

  public static final String IMPLICIT_COLLECTION_PREFIX = "_implicit_org_";

  static {
    PHARMA_ORGS.put(PharmaOrgs.MANUFACTURER, PharmaOrg.builder()
            .name(PharmaOrgs.MANUFACTURER.getMspId())
            .role(PharmaOrgRoles.MANUFACTURER)
            .implicitCollectionName(IMPLICIT_COLLECTION_PREFIX + PharmaOrgs.MANUFACTURER.getMspId())
            .build());
    PHARMA_ORGS.put(PharmaOrgs.TRANSPORTER, PharmaOrg.builder()
            .name(PharmaOrgs.TRANSPORTER.getMspId())
            .role(PharmaOrgRoles.TRANSPORTER)
            .implicitCollectionName(IMPLICIT_COLLECTION_PREFIX + PharmaOrgs.TRANSPORTER.getMspId())
            .build());
    PHARMA_ORGS.put(PharmaOrgs.DISTRIBUTOR, PharmaOrg.builder()
            .name(PharmaOrgs.DISTRIBUTOR.getMspId())
            .role(PharmaOrgRoles.DISTRIBUTOR)
            .crpSharedCollectionName("Org1MSPOrg3MSPCRPCollection")
            .implicitCollectionName(IMPLICIT_COLLECTION_PREFIX + PharmaOrgs.DISTRIBUTOR.getMspId())
            .build());
    PHARMA_ORGS.put(PharmaOrgs.PHARMACY, PharmaOrg.builder()
            .name(PharmaOrgs.PHARMACY.getMspId())
            .role(PharmaOrgRoles.RETAILER)
            .crpSharedCollectionName("Org1MSPOrg4MSPCRPCollection")
            .implicitCollectionName(IMPLICIT_COLLECTION_PREFIX + PharmaOrgs.PHARMACY.getMspId())
            .build());
    PHARMA_ORGS.put(PharmaOrgs.CONSUMER, PharmaOrg.builder()
            .name(PharmaOrgs.CONSUMER.getMspId())
            .role(PharmaOrgRoles.CONSUMER)
            .implicitCollectionName(IMPLICIT_COLLECTION_PREFIX + PharmaOrgs.CONSUMER.getMspId())
            .crpSharedCollectionName("Org1MSPOrg5MSPCRPCollection")
            .build());
  }

  public static PharmaOrg getOrg(String mspid) {
    return PHARMA_ORGS
            .entrySet()
            .stream()
            .filter(entry -> entry.getKey().getMspId().equals(mspid))
            .findFirst()
            .map(Map.Entry::getValue)
            .orElseThrow(() -> new ChaincodeException(String.format("Org %s not found", mspid)));
  }
}
