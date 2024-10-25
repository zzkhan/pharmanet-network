package org.hyperledger.fabric.samples.pharma.model;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;
import lombok.Value;
import lombok.extern.jackson.Jacksonized;
import org.hyperledger.fabric.contract.annotation.DataType;
import org.hyperledger.fabric.contract.annotation.Property;

import java.io.Serializable;

import static com.fasterxml.jackson.annotation.JsonInclude.Include.NON_NULL;

@DataType
@Data
@Builder
@ToString(onlyExplicitlyIncluded = true, includeFieldNames = false)
@Jacksonized
@JsonInclude(NON_NULL)
public class DrugCrp implements Serializable {

  @Property()
  @EqualsAndHashCode.Include
  @ToString.Include
  String challenge;

  @Property()
  @EqualsAndHashCode.Include
  @ToString.Include
  String response;
}
