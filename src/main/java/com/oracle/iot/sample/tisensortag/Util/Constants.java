package com.oracle.iot.sample.tisensortag.Util;


public class Constants {
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

    public final static String SENSOR_VALUES_CHANGED =
            "com.oracle.iot.sample.tisensortag.bluetooth.le.ACTION_DATA_AVAILABLE";
    public final static String DEVICE_AVAILABLE =
            "com.oracle.iot.sample.tisensortag.bluetooth.le.DEVICE_AVAILABLE";
    public final static String DEVICE_CONNECTED =
            "com.oracle.iot.sample.tisensortag.bluetooth.le.DEVICE_CONNECTED";
    public final static String UI_MESSAGE =
            "com.oracle.iot.sample.tisensortag.UI_MESSAGE";
    public static final String FALL_ALERT_URN = "urn:oracle:asset:ti:sensortag:cc2650:alert:fall";
    public static final String FALL_ALERT_FIELD = "fall";
    public static final String LIGHT_ATTRIBUTE = "light";
    public static final String TILT_ALERT_URN = "urn:oracle:asset:ti:sensortag:cc2650:alert:tilt";
    public static final String TILT_ALERT_FIELD = "tilt";
    public static final String TEMPERATURE_ATTRIBUTE = "temperature";
    public static final String HUMIDITY_ATTRIBUTE = "humidity";
    public static final String PRESSURE_ATTRIBUTE = "pressure";
    public static final String NET_ACCELERATION_ATTRIBUTE = "netacceleration";
    public static final String X_AXIS_TILT_ATTRIBUTE = "xaxistilt";
    public static final String LONGITUDE_ATTRIBUTE = "ora_longitude";
    public static final String LATITUDE_ATTRIBUTE = "ora_latitude";
    public static final String ALTITUDE_ATTRIBUTE = "ora_altitude";

    public static final String EXTRA_SENSORVALUES = "sensor_values";

    /**
     * The desired interval for location updates. Inexact. Updates may be more or less frequent.
     */
    public static final long UPDATE_INTERVAL_IN_MILLISECONDS = 10000;

    /**
     * The fastest rate for active location updates. Exact. Updates will never be more frequent
     * than this value.
     */
    public static final long FASTEST_UPDATE_INTERVAL_IN_MILLISECONDS =
            UPDATE_INTERVAL_IN_MILLISECONDS / 2;

    public interface ACTION {
        String MAIN_ACTION = "com.oracle.iot.sample.tisensortag.sensordevicetrackerservice.action.main";
        String STARTFOREGROUND_ACTION = "com.oracle.iot.sample.tisensortag.sensordevicetrackerservice.action.startforeground";
        String STOPFOREGROUND_ACTION = "com.oracle.iot.sample.tisensortag.sensordevicetrackerservice.action.stopforeground";
    }

    public interface NOTIFICATION_ID {
        int FOREGROUND_SERVICE = 10001;
    }
}
