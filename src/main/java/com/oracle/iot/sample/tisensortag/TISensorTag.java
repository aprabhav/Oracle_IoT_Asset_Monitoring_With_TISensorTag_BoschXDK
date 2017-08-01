package com.oracle.iot.sample.tisensortag;

import android.bluetooth.BluetoothGattCharacteristic;

import java.util.UUID;

import static com.oracle.iot.sample.tisensortag.SensorDeviceFactory.TI_DEVICE_NAME;
import static java.lang.Math.pow;


public class TISensorTag implements SensorDevice {

    private static final UUID TEMPERATURE_SERVICE = UUID.fromString("f000aa00-0451-4000-b000-000000000000");
    private static final UUID TEMPERATURE_DATA_CHAR = UUID.fromString("f000aa01-0451-4000-b000-000000000000");
    private static final UUID TEMPERATURE_CONFIG_CHAR = UUID.fromString("f000aa02-0451-4000-b000-000000000000");
    private static final UUID TEMPERATURE_PERIOD_CHAR = UUID.fromString("f000aa03-0451-4000-b000-000000000000");

    /* Humidity Service */
    private static final UUID HUMIDITY_SERVICE = UUID.fromString("f000aa20-0451-4000-b000-000000000000");
    private static final UUID HUMIDITY_DATA_CHAR = UUID.fromString("f000aa21-0451-4000-b000-000000000000");
    private static final UUID HUMIDITY_CONFIG_CHAR = UUID.fromString("f000aa22-0451-4000-b000-000000000000");
    private static final UUID HUMIDITY_PERIOD_CHAR = UUID.fromString("f000aa23-0451-4000-b000-000000000000");


    /* Luxometer Service */
    private static final UUID LUXOMETER_SERVICE = UUID.fromString("f000aa70-0451-4000-b000-000000000000");
    private static final UUID LUXOMETER_DATA_CHAR = UUID.fromString("f000aa71-0451-4000-b000-000000000000");
    private static final UUID LUXOMETER_CONFIG_CHAR = UUID.fromString("f000aa72-0451-4000-b000-000000000000");
    private static final UUID LUXOMETER_PERIOD_CHAR = UUID.fromString("f000aa73-0451-4000-b000-000000000000");


    /* Barometric Pressure Service */
    private static final UUID PRESSURE_SERVICE = UUID.fromString("f000aa40-0451-4000-b000-000000000000");
    private static final UUID PRESSURE_DATA_CHAR = UUID.fromString("f000aa41-0451-4000-b000-000000000000");
    private static final UUID PRESSURE_CONFIG_CHAR = UUID.fromString("f000aa42-0451-4000-b000-000000000000");
    private static final UUID PRESSURE_PERIOD_CHAR = UUID.fromString("f000aa44-0451-4000-b000-000000000000");


    /* Movement Service */
    public static final UUID MOVEMENT_SERVICE = UUID.fromString("f000aa80-0451-4000-b000-000000000000");
    public static final UUID MOVEMENT_DATA_CHAR = UUID.fromString("f000aa81-0451-4000-b000-000000000000");
    public static final UUID MOVEMENT_CONFIG_CHAR = UUID.fromString("f000aa82-0451-4000-b000-000000000000");
    public static final UUID MOVEMENT_PERIOD_CHAR = UUID.fromString("f000aa83-0451-4000-b000-000000000000");

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

    private static SensorInfo[] mSensorInfo = new SensorInfo[] {
            new SensorInfo("Temperature", UIMessageConstants.MSG_TEMPERATURE, TEMPERATURE_SERVICE, TEMPERATURE_CONFIG_CHAR, new byte[] {0x01}, TEMPERATURE_PERIOD_CHAR, new byte[] {0x64}, TEMPERATURE_DATA_CHAR),
            new SensorInfo("Humidity", UIMessageConstants.MSG_HUMIDITY, HUMIDITY_SERVICE, HUMIDITY_CONFIG_CHAR, new byte[] {0x01},HUMIDITY_PERIOD_CHAR, new byte[] {0x64}, HUMIDITY_DATA_CHAR),
            new SensorInfo("Luxometer", UIMessageConstants.MSG_LIGHT, LUXOMETER_SERVICE, LUXOMETER_CONFIG_CHAR, new byte[] {0x01}, LUXOMETER_PERIOD_CHAR, new byte[] {0x64}, LUXOMETER_DATA_CHAR),
            new SensorInfo("Pressure", UIMessageConstants.MSG_PRESSURE, PRESSURE_SERVICE, PRESSURE_CONFIG_CHAR, new byte[] {0x01}, PRESSURE_PERIOD_CHAR, new byte[] {0x64}, PRESSURE_DATA_CHAR),
            new SensorInfo("Movement", UIMessageConstants.MSG_MOVEMENT, MOVEMENT_SERVICE, MOVEMENT_CONFIG_CHAR, new byte[] {0x78,0x02}, MOVEMENT_PERIOD_CHAR, new byte[] {0x0A}, MOVEMENT_DATA_CHAR)
    };


    public TISensorTag(){
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
        return TI_DEVICE_NAME;
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
        /*
        int a = getUnsignedValue(c, 2);
        // bits [1..0] are status bits and need to be cleared
        a = a - (a % 4);
        humiditySensorValue = ((-6f) + 125f * (a / 65535f));
        */
        int rawHum = getUnsignedValue(c, 2);
        humiditySensorValue = ((double)rawHum / 65536)*100;
        return humiditySensorValue;
    }

    public double extractTemperature(byte[] c) {

        /*
        double ambient = shortUnsignedAtOffset(c, 2).doubleValue()/128.0;
        int target = shortSignedAtOffset(c, 0);
        target = target - (target % 4);
        temperatureSensorValue = target/100;
        return temperatureSensorValue;
        */

        /*
        double SCALE_LSB = 0.03125;
        int object_int = shortSignedAtOffset(c, 0) >> 2;
        temperatureSensorValue = object_int * SCALE_LSB;
        return temperatureSensorValue;
        */

        //temperatureSensorValue = getUnsignedValue(c, 2)/128.0;
        double SCALE_LSB = 0.03125;
        Integer it = (getUnsignedValue(c, 2)) >> 2;
        temperatureSensorValue = it * SCALE_LSB;
        return temperatureSensorValue;
    }

    public double extractLight(byte[] c) {

        int mantissa;
        int exponent;

        int sfloat = getUnsignedValue(c, 0);

        mantissa = sfloat & 0x0FFF;
        exponent = (sfloat >> 12) & 0xFF;

        double magnitude = pow(2.0f, exponent);
        double light = (mantissa * magnitude);

        lightSensorValue = light/100.0f;
        return lightSensorValue;

    }



    public double extractBarometer(byte[] c) {

        pressureSensorValue = shortUnsignedThreeBytesAtOffset(c, 3)/100; //Pressure in hPa or mBar
        return pressureSensorValue;
    }

    /*
     * This method returns both the NetAcceleration (netAccelerationSensorValue[0])
     * and Tilt (netAccelerationSensorValue[1])
     */
    public double [] extractMovement (byte[] c) {

        byte[] value = c;

        // Range 8G
        final float SCALE = (float) 4096.0;
        double x = ((((value[7]<<8) + value[6]) / SCALE) * -1);
        double y = ((value[9]<<8) + value[8])/SCALE;
        double z = ((((value[11]<<8) + value[10]) / SCALE)* -1);

        netAccelerationSensorValue [0] = Math.sqrt(Math.pow(x, 2) + Math.pow(y, 2) +  Math.pow(z, 2));
        if (netAccelerationSensorValue [0] < 0.8) {
            rapidFallAlert = true;
            rapidFallValue = netAccelerationSensorValue [0];
        }

        //netAccelerationSensorValue [1] = Math.atan(x/(Math.sqrt(Math.pow(y, 2) +  Math.pow(z, 2))));
        netAccelerationSensorValue[1] = Math.toDegrees(Math.asin(Math.min(Math.max(y, -1.0), 1.0)));

        return netAccelerationSensorValue;
    }

    public double extractMagnetism (byte[] c) {
        final float SCALE = (float) (32768 / 4912);

        double magX = getSignedValue(c, 12)/SCALE * 1.0f;
        double magY = getSignedValue(c, 14)/SCALE * 1.0f;
        double magZ = getSignedValue(c, 16)/SCALE * 1.0f;
        magnetometerSensorValue = Math.sqrt(Math.pow(magX, 2) + Math.pow(magY, 2) +  Math.pow(magZ, 2));
        return magnetometerSensorValue;
    }
    
    /**
     * Gyroscope, Magnetometer, IR temperature
     * all store 16 bit two's complement values in the awkward format
     * LSB MSB, which cannot be directly parsed as getIntValue(FORMAT_SINT16, offset)
     * because the bytes are stored in the "wrong" direction.
     *
     * Barometer stores value as 24 bit and hence needs special handling.
     *
     * This function extracts these 16 bit two's complement values.
     * */
    private static Integer shortSignedAtOffset(BluetoothGattCharacteristic c, int offset) {
        Integer lowerByte = c.getIntValue(BluetoothGattCharacteristic.FORMAT_UINT8, offset);
        Integer upperByte = c.getIntValue(BluetoothGattCharacteristic.FORMAT_SINT8, offset + 1); // Note: interpret MSB as signed.

        return (upperByte << 8) + lowerByte;
    }

    private static Integer shortUnsignedAtOffset(BluetoothGattCharacteristic c, int offset) {
        Integer lowerByte = c.getIntValue(BluetoothGattCharacteristic.FORMAT_UINT8, offset);
        Integer upperByte = c.getIntValue(BluetoothGattCharacteristic.FORMAT_UINT8, offset + 1); // Note: interpret MSB as unsigned.

        return (upperByte << 8) + lowerByte;
    }

    private static Integer shortUnsignedThreeBytesAtOffset(byte[] c, int offset) {
        /*
        Integer lowerByte = c.getIntValue(BluetoothGattCharacteristic.FORMAT_UINT8, offset);
        Integer middleByte = c.getIntValue(BluetoothGattCharacteristic.FORMAT_UINT8, offset + 1);
        Integer upperByte = c.getIntValue(BluetoothGattCharacteristic.FORMAT_UINT8, offset + 2); // Note: interpret MSB as unsigned
        */

        Integer lowerByte = c[offset] & 0xFF;
        Integer middleByte = c[offset + 1] & 0xFF;
        Integer upperByte = c[offset + 2] & 0xFF;

        return (upperByte << 16) + (middleByte << 8) + lowerByte;

    }

    private static Integer getSignedValue (byte[] characteristicValue, int offset)
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

    private static Integer getUnsignedValue (byte[] characteristicValue, int offset)
    {
        Integer lowerByte = characteristicValue[offset] & 0xFF;
        Integer upperByte = characteristicValue[offset + 1] & 0xFF;
        return (upperByte << 8) + lowerByte;

    }
}
