package org.hyperledger.fabric.samples.pharma.event;

import lombok.Builder;
import lombok.Data;
import lombok.extern.jackson.Jacksonized;

@Data
@Builder
@Jacksonized
public class DrugVerificationSubmissionEvent extends PharmaEvent {
  String drugName;
  String tagId;
  String submitter;
}
