package org.hyperledger.fabric.samples.pharma.helper;

import org.hyperledger.fabric.contract.Context;

public class EventHelper {
  public static void sendEvent(Context ctx, String eventType, byte[] eventData) {
    ctx.getStub().setEvent(eventType, eventData);
  }
}
