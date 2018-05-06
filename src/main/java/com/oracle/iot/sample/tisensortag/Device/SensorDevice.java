package com.oracle.iot.sample.tisensortag.Device;

import java.util.UUID;

public interface SensorDevice {
    SensorInfo[] getSensorInfo();
    UUID getConfigDescriptor();
    void setSensorAttributeValue(final int characteristicType, final byte[] characteristicValue);
    double getTemperatureSensorValue();
    double getPressureSensorValue();
    double getLightSensorValue();
    double getHumiditySensorValue();
    double getNetAccelerationSensorValue();
    double getTiltSensorValue();
}
