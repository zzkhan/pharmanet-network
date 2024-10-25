package org.hyperledger.fabric.samples.pharma.adapter;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.hyperledger.fabric.samples.pharma.model.DrugPo;
import org.hyperledger.fabric.samples.pharma.model.DrugShipment;

public class DrugShipmentAdapter {
  private static final ObjectMapper objectMapper = new ObjectMapper();

  static {
    objectMapper.setSerializationInclusion(JsonInclude.Include.NON_NULL);
    objectMapper.registerModule(new JavaTimeModule());
    objectMapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
  }
  public static String serialise(DrugShipment drugShipment) {
    try {
      return objectMapper.writeValueAsString(drugShipment);
    } catch (JsonProcessingException e) {
      throw new RuntimeException(e);
    }
  }

  public static DrugShipment fromJson(String shipmentJson) {
    try {
      return objectMapper.readValue(shipmentJson, DrugShipment.class);
    } catch (JsonProcessingException e) {
      throw new RuntimeException(e);
    }
  }

  public static byte[] toBytes(DrugShipment shipment) {
    try {
      return objectMapper.writeValueAsBytes(shipment);
    } catch (JsonProcessingException e) {
      throw new RuntimeException(e);
    }
  }
}
