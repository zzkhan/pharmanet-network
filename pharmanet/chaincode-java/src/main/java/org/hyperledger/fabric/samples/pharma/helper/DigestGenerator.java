package org.hyperledger.fabric.samples.pharma.helper;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

public class DigestGenerator {
  public static byte[] computeSHA256Hash(byte[] data) {
    try {
      MessageDigest digest = MessageDigest.getInstance("SHA-256");
      return digest.digest(data);
    } catch (NoSuchAlgorithmException e) {
      throw new RuntimeException("SHA-256 algorithm not found", e);
    }
  }
}
