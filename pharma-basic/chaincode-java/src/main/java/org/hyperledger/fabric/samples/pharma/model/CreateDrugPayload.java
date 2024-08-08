/*
 * SPDX-License-Identifier: Apache-2.0
 */

package org.hyperledger.fabric.samples.pharma.model;

import lombok.Builder;
import lombok.ToString;
import lombok.Value;
import lombok.extern.jackson.Jacksonized;
import org.hyperledger.fabric.contract.annotation.DataType;
import org.hyperledger.fabric.contract.annotation.Property;

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
    String mfgDate;
}
