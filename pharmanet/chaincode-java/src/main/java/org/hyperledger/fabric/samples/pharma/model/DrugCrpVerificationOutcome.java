package org.hyperledger.fabric.samples.pharma.model;

import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;
import lombok.extern.jackson.Jacksonized;
import org.hyperledger.fabric.contract.annotation.DataType;
import org.hyperledger.fabric.contract.annotation.Property;

import java.time.Instant;

@DataType
@Data
@Builder
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
@Jacksonized
@ToString(onlyExplicitlyIncluded = true, includeFieldNames = false)
public class DrugCrpVerificationOutcome {

  @Property
  @EqualsAndHashCode.Include
  @ToString.Include
  String tagId;

  @Property
  @EqualsAndHashCode.Include
  @ToString.Include
  String verifierOrg;

  @Property
  @ToString.Include
  byte[] assignedCrpsHash;

  @Property
  @ToString.Include
  byte[] verificationCrpsHash;

  @Property
  Instant created;

  @Property
  Instant updated;

  @Property
  @Builder.Default
  @ToString.Include
  DrugCrpVerificationOutcomeStatus status = DrugCrpVerificationOutcomeStatus.PENDING;
}
