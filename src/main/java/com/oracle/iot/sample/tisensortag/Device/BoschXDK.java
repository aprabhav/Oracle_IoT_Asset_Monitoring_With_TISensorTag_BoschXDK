package com.oracle.iot.sample.tisensortag;


import android.bluetooth.BluetoothGattCharacteristic;

import java.nio.ByteBuffer;
import java.nio.charset.Charset;
import java.util.UUID;

import static com.oracle.iot.sample.tisensortag.SensorDeviceFactory.BOSCH_XDK_NAME;
import static com.oracle.iot.sample.tisensortag.SensorDeviceFactory.TI_DEVICE_NAME;
import static java.lang.Math.pow;

public class BoschXDK implements SensorDevice {

    private static final UUID ALPWISE_SERVICE = UUID.fromString("00005301-0000-0041-4C50-574953450000");
    private static final UUID ALPWISE_DATA_CHAR = UUID.fromString("00005303-0000-0041-4C50-574953450000");
    private static final UUID ALPWISE_CONFIG_CHAR = UUID.fromString("00005302-0000-0041-4C50-574953450000");
    private static final UUID ALPWISE_PERIOD_CHAR = UUID.fromString("00005302-0000-0041-4C50-574953450000"); //This is a dummy. Not required for Bosch XDK

    /* Client Configuration Descriptor */
    private static final UUID CONFIG_DESCRIPTOR = UUID.fromString("00002902-0000-1000-8000-00805f9b34fb");

    private double temperatureSensorValue;
    private double humiditySensorValue;
    private double pressureSensorValue;
    private double lightSensorValue;
    private double [] netAccelerationSensorValue;
    private double magnetometerSensorValue;
    private double rapidFallValue;
    private boolean rapidFallAlert;
    private double previousAngle = 0;

    private static SensorInfo[] mSensorInfo = new SensorInfo[] {
            new SensorInfo("xdk", UIMessageConstants.MSG_XDK, ALPWISE_SERVICE, ALPWISE_CONFIG_CHAR, new byte[]{0x73,0x74,0x61,0x72,0x74}, ALPWISE_PERIOD_CHAR, new byte[]{0x00}, ALPWISE_DATA_CHAR),
      };


    public BoschXDK(){
        temperatureSensorValue = 0;
        humiditySensorValue = 0;
        pressureSensorValue = 0;
        lightSensorValue = 0;
        netAccelerationSensorValue = new double[2];
        magnetometerSensorValue = 0;
        rapidFallValue = 0;
        rapidFallAlert = false;
    }

    public String getDeviceName(){
        return BOSCH_XDK_NAME;
    }

    public SensorInfo[] getSensorInfo(){
        return mSensorInfo;
    }

    @Override
    public UUID getConfigDescriptor() {
        return CONFIG_DESCRIPTOR;
    }

    public double getTemperatureSensorValue() {
        return temperatureSensorValue;
    }

    public double getPressureSensorValue() {
        return pressureSensorValue;
    }

    public double getLightSensorValue() {
        return lightSensorValue;
    }

    public double getHumiditySensorValue() {
        return humiditySensorValue;
    }

    public double [] getNetAccelerationSensorValue () { return netAccelerationSensorValue;}

    public double getMagnetometerSensorValue () {return magnetometerSensorValue;}

    public boolean isRapidFallAlert () { return rapidFallAlert;}

    public double readAndClearRapidFallAlert () {
        rapidFallAlert = false;
        return rapidFallValue;
    }

    public double extractHumidity(byte[] c) {

        humiditySensorValue = getUnsignedValue(c, 12) * 1.0f;
        return humiditySensorValue;
    }

    public double extractTemperature(byte[] c) {

        temperatureSensorValue = getSignedValue(c, 10) * 1.0f;
        return temperatureSensorValue;
    }

    public double extractLight(byte[] c) {

        lightSensorValue = getUnsignedValue(c, 6) * 1.0f;
        return lightSensorValue;
    }

    public double extractBarometer(byte[] c) {

        pressureSensorValue = getUnsignedValue(c, 8) * 1.0f;
        return pressureSensorValue;

    }

    /*
     * This method returns both the NetAcceleration (netAccelerationSensorValue[0])
     * and Tilt (netAccelerationSensorValue[1])
     */
    public double [] extractMovement (byte[] c) {

        double x = getSignedValue(c, 0) * 1.0f;
        double y = getSignedValue(c, 2) * 1.0f;
        double z = getSignedValue(c, 4) * 1.0f;

        netAccelerationSensorValue [0] = Math.sqrt(Math.pow(x, 2) + Math.pow(y, 2) +  Math.pow(z, 2));
        if (netAccelerationSensorValue [0] < 0.8) {
            rapidFallAlert = true;
            rapidFallValue = netAccelerationSensorValue [0];
        }

        netAccelerationSensorValue[1] = Math.toDegrees(Math.asin(Math.min(Math.max(y, -1.0), 1.0)));
        /*
        double angleUsingAccel = Math.toDegrees(Math.asin(Math.min(Math.max(y, -1.0), 1.0)));
        double rmsGyroValue = getUnsignedValue(c, 14) * 1.0f;
        netAccelerationSensorValue[1] = computeTiltAngleUsingGyroAndAccel(angleUsingAccel, rmsGyroValue);
        */
        return netAccelerationSensorValue;
    }

    public double extractMagnetism (byte[] c) {
        return magnetometerSensorValue;
    }

    /**
     * These functions extract 16 bit two's complement values.
     **/
    private static Integer shortSignedAtOffset(BluetoothGattCharacteristic c, int offset) {
        Integer lowerByte = c.getIntValue(BluetoothGattCharacteristic.FORMAT_UINT8, offset);
        Integer upperByte = c.getIntValue(BluetoothGattCharacteristic.FORMAT_SINT8, offset + 1); // Note: interpret MSB as signed.

        return (upperByte << 8) + lowerByte;
    }

    public static Integer getSignedValue (byte[] characteristicValue, int offset)
    {
        Integer lowerByte = characteristicValue[offset] & 0xFF;
        Integer upperByte = unsignedToSigned((characteristicValue[offset + 1] & 0xFF) , 8);
        return (upperByte << 8) + lowerByte;
    }

    /**
     * Convert an unsigned integer value to a two's-complement encoded
     * signed value.
     */
    private static Integer unsignedToSigned(int unsigned, int size) {
        if ((unsigned & (1 << size-1)) != 0) {
            unsigned = -1 * ((1 << size-1) - (unsigned & ((1 << size-1) - 1)));
        }
        return unsigned;
    }

    private static Integer shortSignedAtOffsetFourBytes (BluetoothGattCharacteristic c, int offset) {
        Integer firstByte = c.getIntValue(BluetoothGattCharacteristic.FORMAT_UINT8, offset);
        Integer secondByte = c.getIntValue(BluetoothGattCharacteristic.FORMAT_UINT8, offset + 1);
        Integer thirdByte = c.getIntValue(BluetoothGattCharacteristic.FORMAT_UINT8, offset + 2);
        Integer FourthByte = c.getIntValue(BluetoothGattCharacteristic.FORMAT_SINT8, offset + 3); // Note: interpret MSB as signed.

        return (FourthByte << 24) + (thirdByte << 16) + (secondByte << 8) + firstByte;
    }

    private static Integer shortUnsignedAtOffset(BluetoothGattCharacteristic c, int offset) {
        Integer lowerByte = c.getIntValue(BluetoothGattCharacteristic.FORMAT_UINT8, offset);
        Integer upperByte = c.getIntValue(BluetoothGattCharacteristic.FORMAT_UINT8, offset + 1); // Note: interpret MSB as unsigned.

        return (upperByte << 8) + lowerByte;
    }


    public static Integer getUnsignedValue (byte[] characteristicValue, int offset)
    {
        Integer lowerByte = characteristicValue[offset] & 0xFF;
        Integer upperByte = characteristicValue[offset + 1] & 0xFF;
        return (upperByte << 8) + lowerByte;

    }

    private static Integer shortUnsignedThreeBytesAtOffset(BluetoothGattCharacteristic c, int offset) {
        Integer lowerByte = c.getIntValue(BluetoothGattCharacteristic.FORMAT_UINT8, offset);
        Integer middleByte = c.getIntValue(BluetoothGattCharacteristic.FORMAT_UINT8, offset + 1);
        Integer upperByte = c.getIntValue(BluetoothGattCharacteristic.FORMAT_UINT8, offset + 2); // Note: interpret MSB as unsigned.

        return (upperByte << 16) + (middleByte << 8) + lowerByte;

    }

    private static Integer shortUnsignedFourBytesAtOffset(BluetoothGattCharacteristic c, int offset) {
        Integer firstByte = c.getIntValue(BluetoothGattCharacteristic.FORMAT_UINT8, offset);
        Integer secondByte = c.getIntValue(BluetoothGattCharacteristic.FORMAT_UINT8, offset + 1);
        Integer thirdByte = c.getIntValue(BluetoothGattCharacteristic.FORMAT_UINT8, offset + 2);
        Integer fourthByte = c.getIntValue(BluetoothGattCharacteristic.FORMAT_UINT8, offset + 3);

        return (fourthByte << 24) + (thirdByte << 16) + (secondByte << 8) + firstByte;

    }

    /*
    private double computeTiltAngleUsingGyroAndAccel(double newAngle, double newRate) {

        double filterTerm0;
        double filterTerm1;
        double filterTerm2;
        double timeConstant;
        double filterAngle = previousAngle;
        double dt = 0.0086;

        timeConstant = 0.5; // default 1.0

        filterTerm0 = (newAngle - filterAngle) * timeConstant * timeConstant;
        filterTerm2 = filterTerm0 * dt + filterTerm2;
        filterTerm1 = filterTerm2 + ((newAngle - filterAngle) * 2 * timeConstant) + newRate;
        filterAngle = (filterTerm1 * dt) + filterAngle;

        previousAngle = filterAngle;
        return previousAngle;
    }
    */

}
