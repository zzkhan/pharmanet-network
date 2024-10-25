package org.hyperledger.fabric.samples.pharma.adapter;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.hyperledger.fabric.samples.pharma.model.Drug;
import org.hyperledger.fabric.samples.pharma.model.PharmaOrg;

public class PharmaOrgAdapter {
  private static final ObjectMapper objectMapper = new ObjectMapper();

  static {
    objectMapper.registerModule(new JavaTimeModule());
    objectMapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
    objectMapper.setSerializationInclusion(JsonInclude.Include.NON_NULL);
  }

  public static PharmaOrg fromJson(String assetJSON) {
    try {
      return objectMapper.readValue(assetJSON, PharmaOrg.class);
    } catch (JsonProcessingException e) {
      throw new RuntimeException(e);
    }
  }

  public static String serialize(PharmaOrg org) {
    try {
      return objectMapper.writeValueAsString(org);
    } catch (JsonProcessingException e) {
      throw new RuntimeException(e);
    }
  }
}
