package org.hyperledger.fabric.samples.pharma.adapter;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import lombok.extern.slf4j.Slf4j;
import org.hyperledger.fabric.samples.pharma.model.DrugSaleRecord;

import java.io.IOException;

@Slf4j
public class DrugSaleRecordAdapter {
  private static final ObjectMapper objectMapper = new ObjectMapper();

  static {
    objectMapper.setSerializationInclusion(JsonInclude.Include.NON_NULL);
    objectMapper.registerModule(new JavaTimeModule());
    objectMapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
  }

  public static DrugSaleRecord fromJson(String drugSale) {
    try {
      DrugSaleRecord drugSaleRecordFromString = objectMapper.readValue(drugSale, DrugSaleRecord.class);
      log.info("drugSaleFromString {}", drugSaleRecordFromString);
      return drugSaleRecordFromString;
    } catch (JsonProcessingException e) {
      System.out.println("Error while deserialising drugSale: " + drugSale);
      throw new RuntimeException(e);
    }
  }

  public static DrugSaleRecord fromBytes(byte[] drugSaleBytes) {
    try {
      return objectMapper.readValue(drugSaleBytes, DrugSaleRecord.class);
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
  }

  public static String serialize(DrugSaleRecord drugSaleRecord) {
    try {
      String drugSaleAsString = objectMapper.writeValueAsString(drugSaleRecord);
      log.info("drugSaleAsString {}", drugSaleAsString);
      return drugSaleAsString;
    } catch (JsonProcessingException e) {
      throw new RuntimeException(e);
    }
  }
}
