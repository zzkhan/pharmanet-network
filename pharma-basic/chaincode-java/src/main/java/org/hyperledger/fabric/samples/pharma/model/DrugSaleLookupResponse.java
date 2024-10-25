package org.hyperledger.fabric.samples.pharma.model;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Builder;
import lombok.Data;
import lombok.ToString;
import lombok.extern.jackson.Jacksonized;
import org.hyperledger.fabric.contract.annotation.DataType;
import org.hyperledger.fabric.contract.annotation.Property;

import static com.fasterxml.jackson.annotation.JsonInclude.Include.NON_NULL;

@DataType
@Builder
@Data
@ToString(onlyExplicitlyIncluded = true, includeFieldNames = false)
@Jacksonized
@JsonInclude(NON_NULL)
public class DrugSaleLookupResponse {
  @Property
  @Builder.Default
  boolean drugSaleFound = false;
  @Property
  DrugSaleRecord drugSaleRecord;
}
