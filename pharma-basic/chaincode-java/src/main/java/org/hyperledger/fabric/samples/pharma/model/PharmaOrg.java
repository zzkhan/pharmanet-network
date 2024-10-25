package org.hyperledger.fabric.samples.pharma.model;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.RequiredArgsConstructor;
import lombok.ToString;
import lombok.Value;
import lombok.extern.jackson.Jacksonized;

import static com.fasterxml.jackson.annotation.JsonInclude.Include.NON_NULL;

@Value
@Builder
@ToString
@Jacksonized
@JsonInclude(NON_NULL)
@RequiredArgsConstructor
public class PharmaOrg {
  String name;
  PharmaOrgRoles role;
  String crpSharedCollectionName;
  String implicitCollectionName;
}
