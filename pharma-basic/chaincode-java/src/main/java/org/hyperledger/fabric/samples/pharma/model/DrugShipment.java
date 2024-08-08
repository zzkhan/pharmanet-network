package org.hyperledger.fabric.samples.pharma.model;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Builder;
import lombok.Data;
import lombok.ToString;
import lombok.extern.jackson.Jacksonized;
import org.hyperledger.fabric.contract.annotation.DataType;
import org.hyperledger.fabric.contract.annotation.Property;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

import static com.fasterxml.jackson.annotation.JsonInclude.Include.NON_NULL;

@DataType
@Data
@Builder
@ToString
@Jacksonized
@JsonInclude(NON_NULL)
public class DrugShipment {

  @Property()
  String id;

  @Property()
  String seller;

  @Property()
  String buyer;

  @Property()
  String transporter;

  @Property()
  String drugName;

  @Property()
  @Builder.Default
  List<String> lineItems = new ArrayList<>();

  @Property()
  @Builder.Default
  DrugShipmentStatus status = DrugShipmentStatus.IN_TRANSIT;

  @Property()
  Instant created;

  @Property()
  Instant updated;




}
