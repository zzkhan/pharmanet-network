package org.hyperledger.fabric.samples.pharma.adapter;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.hyperledger.fabric.samples.pharma.model.DrugCrp;

import java.io.IOException;
import java.util.List;

@Slf4j
public class DrugCrpAdapter {
  private static final ObjectMapper objectMapper = new ObjectMapper();

  static {
    objectMapper.setSerializationInclusion(JsonInclude.Include.NON_NULL);
  }

  public static List<DrugCrp> fromJson(String crpJSON) {
    try {
      log.error("Deserialising crps JSON: {}",crpJSON);
      return objectMapper.readValue(crpJSON, new TypeReference<>() {
      });
    } catch (JsonProcessingException e) {
      log.error("Error while deserialising crps JSON: {}",crpJSON,e);
      throw new RuntimeException(e);
    }
  }

  public static List<DrugCrp> fromBytes(byte[] drugCrpBytes) {
    try {
      return objectMapper.readValue(drugCrpBytes, new TypeReference<>() {
      });
    } catch (IOException e) {
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
