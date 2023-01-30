package com.app.androidtvremote;

import android.bluetooth.BluetoothHidDevice;
import android.bluetooth.BluetoothHidDeviceAppQosSettings;
import android.bluetooth.BluetoothHidDeviceAppSdpSettings;
import android.os.ParcelUuid;


public class Constants {

    public static final ParcelUuid HOGP_UUID = ParcelUuid.fromString("00001812-0000-1000-8000-00805f9b34fb");
    public static final ParcelUuid HID_UUID = ParcelUuid.fromString("00001124-0000-1000-8000-00805f9b34fb");
    public static final ParcelUuid DIS_UUID = ParcelUuid.fromString("0000180A-0000-1000-8000-00805F9B34FB");
    public static final ParcelUuid BAS_UUID = ParcelUuid.fromString("0000180F-0000-1000-8000-00805F9B34FB");
    public static final byte ID_KEYBOARD = 1;
    public static final byte ID_REMOTE_CONTROL = 2;
    public static final byte ID_MOUSE = 3;
    private static final byte[] HID_REPORT_DESC = {
            // Keyboard
            (byte) 0x05, (byte) 0x01,
            (byte) 0x09, (byte) 0x06,
            (byte) 0xA1, (byte) 0x01,
            (byte) 0x85, ID_KEYBOARD,
            (byte) 0x05, (byte) 0x07,
            (byte) 0x19, (byte) 0xE0,
            (byte) 0x29, (byte) 0xE7,
            (byte) 0x15, (byte) 0x00,
            (byte) 0x25, (byte) 0x01,
            (byte) 0x75, (byte) 0x01,
            (byte) 0x95, (byte) 0x08,
            (byte) 0x81, (byte) 0x02,

            // Keyboard Key
            (byte) 0x75, (byte) 0x08,
            (byte) 0x95, (byte) 0x01,
            (byte) 0x15, (byte) 0x00,
            (byte) 0x26, (byte) 0xFF, (byte) 0x00,
            (byte) 0x05, (byte) 0x07,
            (byte) 0x19, (byte) 0x00,
            (byte) 0x29, (byte) 0xFF,
            (byte) 0x81, (byte) 0x00,
            (byte) 0xC0,

            // Remote control
            (byte) 0x05, (byte) 0x0c,
            (byte) 0x09, (byte) 0x01,
            (byte) 0xa1, (byte) 0x01,
            (byte) 0x85, ID_REMOTE_CONTROL,
            (byte) 0x19, (byte) 0x00,
            (byte) 0x2a, (byte) 0xff, (byte) 0x03,
            (byte) 0x75, (byte) 0x0a,
            (byte) 0x95, (byte) 0x01,
            (byte) 0x15, (byte) 0x00,
            (byte) 0x26, (byte) 0xff, (byte) 0x03,
            (byte) 0x81, (byte) 0x00,
            (byte) 0xc0,

            // Mouse
            (byte) 0x05, (byte) 0x01,
            (byte) 0x09, (byte) 0x02,
            (byte) 0xA1, (byte) 0x01,
            (byte) 0x85, ID_MOUSE,
            (byte) 0x09, (byte) 0x01,
            (byte) 0xA1, (byte) 0x00,
            (byte) 0x05, (byte) 0x09,
            (byte) 0x19, (byte) 0x01,
            (byte) 0x29, (byte) 0x03,
            (byte) 0x15, (byte) 0x00,
            (byte) 0x25, (byte) 0x01,
            (byte) 0x75, (byte) 0x01,
            (byte) 0x95, (byte) 0x03,
            (byte) 0x81, (byte) 0x02,
            (byte) 0x75, (byte) 0x05,
            (byte) 0x95, (byte) 0x01,
            (byte) 0x81, (byte) 0x01,
            (byte) 0x05, (byte) 0x01,
            (byte) 0x09, (byte) 0x30,
            (byte) 0x09, (byte) 0x31,
            (byte) 0x09, (byte) 0x38,
            (byte) 0x15, (byte) 0x81,
            (byte) 0x25, (byte) 0x7F,
            (byte) 0x75, (byte) 0x08,
            (byte) 0x95, (byte) 0x03,
            (byte) 0x81, (byte) 0x06,
            (byte) 0xC0,
            (byte) 0xC0,



    };

    private static final String SDP_NAME = "BTRemote";
    private static final String SDP_DESCRIPTION = "BTRemote";
    private static final String SDP_PROVIDER = "AAM";
    private static final int QOS_TOKEN_RATE = 800;
    private static final int QOS_TOKEN_BUCKET_SIZE = 9;
    private static final int QOS_PEAK_BANDWIDTH = 0;
    private static final int QOS_LATENCY = 11250;

    public static final BluetoothHidDeviceAppSdpSettings SDP_RECORD =
            new BluetoothHidDeviceAppSdpSettings(
                    Constants.SDP_NAME,
                    Constants.SDP_DESCRIPTION,
                    Constants.SDP_PROVIDER,
                    BluetoothHidDevice.SUBCLASS2_UNCATEGORIZED,
                    Constants.HID_REPORT_DESC);

    public static final BluetoothHidDeviceAppQosSettings QOS_OUT =
            new BluetoothHidDeviceAppQosSettings(
                    BluetoothHidDeviceAppQosSettings.SERVICE_BEST_EFFORT,
                    Constants.QOS_TOKEN_RATE,
                    Constants.QOS_TOKEN_BUCKET_SIZE,
                    Constants.QOS_PEAK_BANDWIDTH,
                    Constants.QOS_LATENCY,
                    BluetoothHidDeviceAppQosSettings.MAX);
}
