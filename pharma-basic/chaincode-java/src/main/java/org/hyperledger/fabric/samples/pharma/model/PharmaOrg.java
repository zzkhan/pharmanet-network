package org.hyperledger.fabric.samples.pharma.model;

import lombok.Builder;
import lombok.ToString;
import lombok.Value;

@Value
@Builder
@ToString
public class PharmaOrg {
  String name;
  PharmaOrgRoles role;
  String crpSharedCollectionName;
  String implicitCollectionName;
}
