package com.app.androidtvremote;

public class RemoteControlReport {

    public static final byte[] reportData = new byte[2];

    public static byte[] getReport(int byte1, int byte2) {
        reportData[0] = (byte) byte2;
        reportData[1] = (byte) byte1;
        return reportData;
    }

}
