package com.oracle.iot.sample.tisensortag;


import android.bluetooth.BluetoothGattCharacteristic;

import java.util.UUID;

public interface SensorDevice {
    SensorInfo[] getSensorInfo();
    UUID getConfigDescriptor();

    double extractHumidity(byte[] characteristicValue);
    double extractTemperature(byte[] characteristicValue);
    double extractLight (byte[] characteristicValue);
    double [] extractMovement (byte[] characteristicValue);
    double extractBarometer (byte[] characteristicValue);
    double extractMagnetism (byte[] characteristicValue);

    double getTemperatureSensorValue();
    double getPressureSensorValue();
    double getLightSensorValue();
    double getHumiditySensorValue();
    double [] getNetAccelerationSensorValue ();
    double getMagnetometerSensorValue();
    boolean isRapidFallAlert ();
    double readAndClearRapidFallAlert ();
}
