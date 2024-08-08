package org.hyperledger.fabric.samples.pharma.model;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Builder;
import lombok.Data;
import lombok.ToString;
import lombok.extern.jackson.Jacksonized;
import org.hyperledger.fabric.contract.annotation.DataType;
import org.hyperledger.fabric.contract.annotation.Default;
import org.hyperledger.fabric.contract.annotation.Property;

import java.time.Instant;

import static com.fasterxml.jackson.annotation.JsonInclude.Include.NON_NULL;

@DataType
@Data
@Builder
@ToString
@Jacksonized
@JsonInclude(NON_NULL)
public class DrugPo {
  @Property()
  String poId;
  @Property()
  String drugName;
  @Property()
  int quantity;
  @Property()
  String buyer;
  @Property()
  String seller;

  @Property()
  @Builder.Default
  Instant created = Instant.now();
}
