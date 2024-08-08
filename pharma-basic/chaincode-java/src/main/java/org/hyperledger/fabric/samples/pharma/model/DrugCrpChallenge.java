package org.hyperledger.fabric.samples.pharma.model;

import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Value;
import lombok.extern.jackson.Jacksonized;
import org.hyperledger.fabric.contract.annotation.DataType;
import org.hyperledger.fabric.contract.annotation.Property;

@DataType
@Value
@Builder
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
@Jacksonized
public class DrugCrpChallenge {

  @Property
  @EqualsAndHashCode.Include
  String value;
}
