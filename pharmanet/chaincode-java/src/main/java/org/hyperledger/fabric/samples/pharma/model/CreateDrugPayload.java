/*
 * SPDX-License-Identifier: Apache-2.0
 */

package org.hyperledger.fabric.samples.pharma.model;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Builder;
import lombok.ToString;
import lombok.Value;
import lombok.extern.jackson.Jacksonized;
import org.hyperledger.fabric.contract.annotation.DataType;
import org.hyperledger.fabric.contract.annotation.Property;

import java.time.LocalDateTime;

@DataType
@Jacksonized
@Builder
@Value
@ToString
public class CreateDrugPayload {
    @Property()
    String drugName;

    @Property()
    String tagId;

    @Property()
    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    LocalDateTime manufactureDate;

    @Property()
    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    LocalDateTime expiryDate;
}
