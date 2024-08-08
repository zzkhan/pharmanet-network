package org.hyperledger.fabric.samples.pharma.adapter;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.hyperledger.fabric.samples.pharma.model.DrugCrpVerificationOutcome;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class DrugCrpVerificationAdapter {
  private static final ObjectMapper objectMapper = new ObjectMapper();

  static {
    objectMapper.setSerializationInclusion(JsonInclude.Include.NON_NULL);
    objectMapper.registerModule(new JavaTimeModule());
  }
  public static String serialise(Set<DrugCrpVerificationOutcome> outcome) {
    try {
      return objectMapper.writeValueAsString(outcome);
    } catch (JsonProcessingException e) {
      throw new RuntimeException(e);
    }
  }

  public static String serialise(DrugCrpVerificationOutcome outcome) {
    try {
      return objectMapper.writeValueAsString(outcome);
    } catch (JsonProcessingException e) {
      throw new RuntimeException(e);
    }
  }

  public static DrugCrpVerificationOutcome deserialise(String outcomesString) {
    try {
      return objectMapper.readValue(outcomesString, DrugCrpVerificationOutcome.class);
    } catch (JsonProcessingException e) {
      throw new RuntimeException(e);
    }
  }
}
