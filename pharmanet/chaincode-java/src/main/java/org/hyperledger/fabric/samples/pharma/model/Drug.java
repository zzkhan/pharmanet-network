package org.hyperledger.fabric.samples.pharma.model;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Builder;
import lombok.Data;
import lombok.ToString;
import lombok.extern.jackson.Jacksonized;
import org.hyperledger.fabric.contract.annotation.DataType;
import org.hyperledger.fabric.contract.annotation.Property;

import java.time.LocalDateTime;

import static com.fasterxml.jackson.annotation.JsonInclude.Include.NON_NULL;
import static org.hyperledger.fabric.samples.pharma.model.DrugStatus.FOR_SALE;


@DataType
@Data
@Builder
@ToString(onlyExplicitlyIncluded = true, includeFieldNames = false)
@Jacksonized
@JsonInclude(NON_NULL)
public class Drug {

  @Property()
  @ToString.Include
  String tagId;

  @Property()
  @ToString.Include
  String name;

  @Property()
  @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
  LocalDateTime manufactureDate;

  @Property()
  @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
  LocalDateTime expiryDate;

  @Property()
  String manufacturer;

  @Property()
  @Builder.Default
  String owner = null;

  @Property()
  @Builder.Default
  byte[] crpHash = null;

  @Property()
  @Builder.Default
  DrugStatus status = FOR_SALE;

  @Property()
  @Builder.Default
  LocalDateTime updatedDate = LocalDateTime.now();
}

