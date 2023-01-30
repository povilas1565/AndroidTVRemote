package com.app.androidtvremote;

public class KeyboardReport {

    public static final byte[] keyboardData = new byte[2];

    public static byte[] getReport(int modifier, int key) {
        keyboardData[0] = (byte) modifier;
        keyboardData[1] = (byte) key;
        return keyboardData;
    }

    public static String print(byte[] bytes) {
        StringBuilder sb = new StringBuilder();
        sb.append("[ ");
        for (byte b : bytes) {
            sb.append(String.format("0x%02X, ", b));
        }
        sb.append("]");
        return sb.toString();
    }

}
