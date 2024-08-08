package org.hyperledger.fabric.samples.pharma.adapter;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.hyperledger.fabric.samples.pharma.model.DrugPo;

public class DrugPoAdapter {
  private static final ObjectMapper objectMapper = new ObjectMapper();

  static {
    objectMapper.setSerializationInclusion(JsonInclude.Include.NON_NULL);
    objectMapper.registerModule(new JavaTimeModule());
  }
  public static String serialise(DrugPo drugPo) {
    try {
      return objectMapper.writeValueAsString(drugPo);
    } catch (JsonProcessingException e) {
      throw new RuntimeException(e);
    }
  }

  public static DrugPo deserialise(String json) {
    try {
      return objectMapper.readValue(json, DrugPo.class);
    } catch (JsonProcessingException e) {
      throw new RuntimeException(e);
    }
  }

  public static byte[] toBytes(DrugPo drugPo) {
    try {
      return objectMapper.writeValueAsBytes(drugPo);
    } catch (JsonProcessingException e) {
      throw new RuntimeException(e);
    }
  }
}
