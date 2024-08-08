package org.hyperledger.fabric.samples.pharma.adapter;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.hyperledger.fabric.samples.pharma.model.DrugCrp;

import java.io.IOException;
import java.util.List;

public class DrugCrpAdapter {
  private static final ObjectMapper objectMapper = new ObjectMapper();

  static {
    objectMapper.setSerializationInclusion(JsonInclude.Include.NON_NULL);
  }

  public static List<DrugCrp> fromJson(String crpJSON) {
    try {
      return objectMapper.readValue(crpJSON, new TypeReference<List<DrugCrp>>() {});
    } catch (JsonProcessingException e) {
      System.out.println("Error while deserialising crps JSON: " + crpJSON);
      throw new RuntimeException(e);
    }
  }

  public static List<DrugCrp> fromBytes(byte[] drugCrpBytes) {
    try {
      return objectMapper.readValue(drugCrpBytes, new TypeReference<List<DrugCrp>>() {});
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
  }

  public static byte[] toBytes(List<DrugCrp> drugCrpList) {
    try {
      return objectMapper.writeValueAsBytes(drugCrpList);
    } catch (JsonProcessingException e) {
      throw new RuntimeException(e);
    }
  }

  public static String serialize(List<DrugCrp> drugCrpList) {
    try {
      return objectMapper.writeValueAsString(drugCrpList);
    } catch (JsonProcessingException e) {
      throw new RuntimeException(e);
    }
  }
}
