package org.hyperledger.fabric.samples.pharma.adapter;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.hyperledger.fabric.samples.pharma.model.Drug;

public class DrugAdapter {
  private static final ObjectMapper objectMapper = new ObjectMapper();

  static {
    objectMapper.setSerializationInclusion(JsonInclude.Include.NON_NULL);
  }

  public static Drug fromJson(String assetJSON) {
    try {
      return objectMapper.readValue(assetJSON, Drug.class);
    } catch (JsonProcessingException e) {
      throw new RuntimeException(e);
    }
  }

  public static String serialize(Drug drug) {
    try {
      return objectMapper.writeValueAsString(drug);
    } catch (JsonProcessingException e) {
      throw new RuntimeException(e);
    }
  }
}
