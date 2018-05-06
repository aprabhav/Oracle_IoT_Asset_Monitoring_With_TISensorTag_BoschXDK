package com.oracle.iot.sample.tisensortag.Device;


import android.app.Application;
import android.arch.lifecycle.LiveData;
import android.arch.lifecycle.MutableLiveData;
import android.bluetooth.BluetoothGattCharacteristic;
import android.content.Context;
import android.util.Log;

import com.oracle.iot.sample.tisensortag.MainActivity;
import com.oracle.iot.sample.tisensortag.Util.BroadcastUtil;
import com.oracle.iot.sample.tisensortag.Util.Constants;

import java.util.UUID;

import static android.content.ContentValues.TAG;
import static com.oracle.iot.sample.tisensortag.Device.SensorDeviceFactory.BOSCH_XDK_NAME;
import static com.oracle.iot.sample.tisensortag.Util.Constants.MSG_HUMIDITY;
import static com.oracle.iot.sample.tisensortag.Util.Constants.MSG_LIGHT;
import static com.oracle.iot.sample.tisensortag.Util.Constants.MSG_MOVEMENT;
import static com.oracle.iot.sample.tisensortag.Util.Constants.MSG_PRESSURE;
import static com.oracle.iot.sample.tisensortag.Util.Constants.MSG_TEMPERATURE;
import static com.oracle.iot.sample.tisensortag.Util.Constants.MSG_XDK;

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
    private double netAccelerationSensorValue;
    private double tiltSensorValue;
    private static SensorInfo[] mSensorInfo = new SensorInfo[] {
            new SensorInfo("xdk", Constants.MSG_XDK, ALPWISE_SERVICE, ALPWISE_CONFIG_CHAR, new byte[]{0x73,0x74,0x61,0x72,0x74}, ALPWISE_PERIOD_CHAR, new byte[]{0x00}, ALPWISE_DATA_CHAR),
      };


    public BoschXDK(){
        temperatureSensorValue = 0;
        humiditySensorValue = 0;
        pressureSensorValue = 0;
        lightSensorValue = 0;
        netAccelerationSensorValue = 0;
        tiltSensorValue = 0;
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

    public double getNetAccelerationSensorValue () {return netAccelerationSensorValue;}

    public double getTiltSensorValue () {return tiltSensorValue;}

    public void setSensorAttributeValue(final int characteristicType, final byte[] characteristicValue) {
        if(characteristicType == MSG_XDK){
            if (characteristicValue == null) {
                Log.w(TAG, "Error obtaining Bosch XDK sensor values");
            } else {
                extractHumidity(characteristicValue);
                extractBarometer(characteristicValue);
                extractLight(characteristicValue);
                extractTemperature(characteristicValue);
                extractMovement(characteristicValue);
            }
        }
    }

    private void extractHumidity(byte[] c) {
        humiditySensorValue = getUnsignedValue(c, 12) * 1.0f;
    }

    private void extractTemperature(byte[] c) {
        temperatureSensorValue = getSignedValue(c, 10) * 1.0f;
    }

    private void extractLight(byte[] c) {
        lightSensorValue = getUnsignedValue(c, 6) * 1.0f;
    }

    private void extractBarometer(byte[] c) {
        pressureSensorValue = getUnsignedValue(c, 8) * 1.0f;
    }

    /*
     * This method calculates both the NetAcceleration (netAccelerationSensorValue[0])
     * and Tilt (netAccelerationSensorValue[1])
     */
    private void extractMovement (byte[] c) {
        double x = getSignedValue(c, 0) * 1.0f;
        double y = getSignedValue(c, 2) * 1.0f;
        double z = getSignedValue(c, 4) * 1.0f;

        netAccelerationSensorValue = Math.sqrt(Math.pow(x, 2) + Math.pow(y, 2) +  Math.pow(z, 2));
        tiltSensorValue = Math.toDegrees(Math.asin(Math.min(Math.max(y, -1.0), 1.0)));
        /*
        double angleUsingAccel = Math.toDegrees(Math.asin(Math.min(Math.max(y, -1.0), 1.0)));
        double rmsGyroValue = getUnsignedValue(c, 14) * 1.0f;
        netAccelerationSensorValue[1] = computeTiltAngleUsingGyroAndAccel(angleUsingAccel, rmsGyroValue);
        */
    }

    public void extractMagnetism (byte[] c) {
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
