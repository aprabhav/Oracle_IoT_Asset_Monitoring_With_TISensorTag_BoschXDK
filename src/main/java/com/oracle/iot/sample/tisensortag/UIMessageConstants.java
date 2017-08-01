package com.oracle.iot.sample.tisensortag;


public class UIMessageConstants {
    public static final int MSG_HUMIDITY = 101;
    public static final int MSG_PRESSURE = 102;
    public static final int MSG_TEMPERATURE = 103;
    public static final int MSG_LIGHT = 104;
    public static final int MSG_MOVEMENT = 105;

    public static final int MSG_XDK = 201;

    public static final int MSG_PROGRESS = 301;
    public static final int MSG_DISMISS = 302;
    public static final int MSG_CLEAR = 303;
    public static final int MSG_EXCEPTION = 304;

    public final static String SENSOR_CHARACTERISTIC_AVAILABLE =
            "com.oracle.iot.sample.tisensortag.bluetooth.le.ACTION_DATA_AVAILABLE";
    public final static String DEVICE_AVAILABLE =
            "com.oracle.iot.sample.tisensortag.bluetooth.le.DEVICE_AVAILABLE";
    public final static String DEVICE_CONNECTED =
            "com.oracle.iot.sample.tisensortag.bluetooth.le.DEVICE_CONNECTED";
    public final static String UI_MESSAGE =
            "com.oracle.iot.sample.tisensortag.UI_MESSAGE";
}
