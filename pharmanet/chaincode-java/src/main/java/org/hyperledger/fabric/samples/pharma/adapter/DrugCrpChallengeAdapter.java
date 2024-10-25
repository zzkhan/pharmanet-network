package org.hyperledger.fabric.samples.pharma.adapter;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.hyperledger.fabric.samples.pharma.model.DrugCrpChallenge;

import java.util.List;

public class DrugCrpChallengeAdapter {

  private static final ObjectMapper objectMapper = new ObjectMapper();

  static {
    objectMapper.setSerializationInclusion(JsonInclude.Include.NON_NULL);
  }

  public static String serialize(List<DrugCrpChallenge> assigneeChallenges) {
    try {
      return objectMapper.writeValueAsString(assigneeChallenges);
    } catch (JsonProcessingException e) {
      throw new RuntimeException(e);
    }
  }

  public static List<DrugCrpChallenge> fromJson(String json) {
    try {
      return objectMapper.readValue(json, new TypeReference<>() {
      });
    } catch (JsonProcessingException e) {
      throw new RuntimeException(e);
    }
  }
}
