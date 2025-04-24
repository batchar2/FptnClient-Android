package org.fptn.vpn.utils;

import java.nio.ByteBuffer;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.Instant;

public class CustomSessionGenerator {

    private static final int FPTN_KEY_LENGTH = 4;

    public static byte[] getSessionId() {
        //todo: I don't understand how you generate key!!!

        byte[] sessionId = new byte[32];

        SecureRandom secureRandom = new SecureRandom();
        secureRandom.nextBytes(sessionId);

        long seconds = Instant.now().getEpochSecond();
        String fptnKey = getSHA1(String.valueOf(seconds)).substring(0, FPTN_KEY_LENGTH);

        byte[] bytes = fptnKey.getBytes();

        ByteBuffer byteBuffer = ByteBuffer.wrap(sessionId);
        byteBuffer.position(sessionId.length - bytes.length);
        byteBuffer.put(bytes, 0, bytes.length);
        return byteBuffer.array();
    }

    private static String getSHA1(String input) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-1");
            byte[] hashBytes = md.digest(input.getBytes());
            return bytesToHex(hashBytes); // Конвертация в HEX
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("SHA-1 algorithm not found", e);
        }
    }

    private static String bytesToHex(byte[] bytes) {
        StringBuilder hexString = new StringBuilder();
        for (byte b : bytes) {
            hexString.append(String.format("%02x", b));
        }
        return hexString.toString();
    }

    private static byte[] hexStringToBytes(String hex) {
        if (hex.length() % 2 != 0) {
            throw new IllegalArgumentException("Hex string must have even length");
        }
        byte[] bytes = new byte[hex.length() / 2];
        for (int i = 0; i < bytes.length; i++) {
            int index = i * 2;
            String hexPair = hex.substring(index, index + 2);
            bytes[i] = (byte) Integer.parseInt(hexPair, 16);
        }
        return bytes;
    }
}
