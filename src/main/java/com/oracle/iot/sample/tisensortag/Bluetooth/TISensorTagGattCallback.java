package com.oracle.iot.sample.tisensortag.Bluetooth;

import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattDescriptor;
import android.bluetooth.BluetoothProfile;
import android.content.Context;
import android.util.Log;

import com.oracle.iot.sample.tisensortag.Util.BroadcastUtil;
import com.oracle.iot.sample.tisensortag.Device.SensorDevice;
import com.oracle.iot.sample.tisensortag.Device.SensorInfo;
import com.oracle.iot.sample.tisensortag.Util.Constants;

import static android.content.ContentValues.TAG;
import static com.oracle.iot.sample.tisensortag.Util.Constants.DEVICE_CONNECTED;
import static com.oracle.iot.sample.tisensortag.Util.Constants.MSG_PROGRESS;
import static com.oracle.iot.sample.tisensortag.Util.Constants.UI_MESSAGE;

public class TISensorTagGattCallback extends BleGattCallback {

    private Context mContext;
    private SensorDevice mSensorDevice;


    public TISensorTagGattCallback(Context context){
        mContext = context;
    }

    /* State Machine Tracking */
    private int mState = 0;

    private void reset() { mState = 0; }

    private void advance() { mState++; }

    /*
     * Send an enable command to each sensor by writing a configuration
     * characteristic.  This is specific to the SensorTag to keep power
     * low by disabling sensors you aren't using.
     */

    private void enableNextSensor(BluetoothGatt gatt){
        BluetoothGattCharacteristic characteristic;

        if (mState < mSensorDevice.getSensorInfo().length) {
            Log.d(TAG, "Enabling" + " " + mSensorDevice.getSensorInfo()[mState].getSensorName());
            characteristic = gatt.getService(mSensorDevice.getSensorInfo()[mState].getServiceChar())
                    .getCharacteristic(mSensorDevice.getSensorInfo()[mState].getConfigChar());

            characteristic.setValue(mSensorDevice.getSensorInfo()[mState].getConfigValues());

            if(characteristic != null) {
                gatt.writeCharacteristic(characteristic);
            }
        }
        else {
            BroadcastUtil.broadcastUpdate(mContext, UI_MESSAGE, Constants.MSG_DISMISS, "");
            Log.i(TAG, "All Sensors Enabled");
        }
    }

    /*
     * Read the data characteristic's value for each sensor explicitly
     */
    private void readNextSensor(BluetoothGatt gatt){
        BluetoothGattCharacteristic characteristic;

        if (mState < mSensorDevice.getSensorInfo().length) {
            Log.d(TAG, "Reading" + " " + mSensorDevice.getSensorInfo()[mState].getSensorName());
            characteristic = gatt.getService(mSensorDevice.getSensorInfo()[mState].getServiceChar())
                    .getCharacteristic(mSensorDevice.getSensorInfo()[mState].getDataChar());

            if(characteristic != null) {
                gatt.readCharacteristic(characteristic);
            }

        }
        else{
            BroadcastUtil.broadcastUpdate(mContext, UI_MESSAGE, Constants.MSG_DISMISS, "");
            Log.i(TAG, "All Sensors Read");
        }
    }

    /*
     * Enable notification of changes on the data characteristic for each sensor
     * by writing the ENABLE_NOTIFICATION_VALUE flag to that characteristic's
     * configuration descriptor.
     */

    private void setNotifyNextSensor(BluetoothGatt gatt) {
        BluetoothGattCharacteristic characteristic;

        if (mState < mSensorDevice.getSensorInfo().length){
            Log.d(TAG, "Set Notify" + " " + mSensorDevice.getSensorInfo()[mState].getSensorName());
            characteristic = gatt.getService(mSensorDevice.getSensorInfo()[mState].getServiceChar())
                    .getCharacteristic(mSensorDevice.getSensorInfo()[mState].getDataChar());


            if(characteristic != null) {
                //Enable local notifications
                gatt.setCharacteristicNotification(characteristic, true);
                //Enabled remote notifications
                BluetoothGattDescriptor desc = characteristic.getDescriptor(mSensorDevice.getConfigDescriptor());
                desc.setValue(BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE);
                gatt.writeDescriptor(desc);
            }
        }
        else{
            BroadcastUtil.broadcastUpdate(mContext, UI_MESSAGE, Constants.MSG_DISMISS, "");
            Log.i(TAG, "All Sensors Notified");
        }

    }

    @Override
    public void onConnectionStateChange(BluetoothGatt gatt, int status, int newState) {
        Log.d(TAG, "Connection State Change: "+status+" -> "+connectionState(newState));
        if (status == BluetoothGatt.GATT_SUCCESS && newState == BluetoothProfile.STATE_CONNECTED) {
            /*
             * Once successfully connected, we must next discover all the services on the
             * device before we can read and write their characteristics.
             */
            BroadcastUtil.broadcastUpdate(mContext, DEVICE_CONNECTED, gatt.getDevice());
            gatt.discoverServices();
            BroadcastUtil.broadcastUpdate(mContext, UI_MESSAGE, MSG_PROGRESS, "Discovering Services...");
        } else if (status == BluetoothGatt.GATT_SUCCESS && newState == BluetoothProfile.STATE_DISCONNECTED) {
            /*
             * If at any point we disconnect, send a message to clear the weather values
             * out of the UI
             */
            BroadcastUtil.broadcastUpdate(mContext, Constants.UI_MESSAGE, Constants.MSG_CLEAR, "");
        } else if (status != BluetoothGatt.GATT_SUCCESS) {
            /*
             * If there is a failure at any stage, simply disconnect
             */
            gatt.disconnect();
            BroadcastUtil.broadcastUpdate(mContext, Constants.UI_MESSAGE, Constants.MSG_CLEAR, "");
        }
    }

    @Override
    public void onServicesDiscovered(BluetoothGatt gatt, int status) {
        Log.d(TAG, "Services Discovered: "+ status);
        BroadcastUtil.broadcastUpdate(mContext, Constants.UI_MESSAGE, Constants.MSG_PROGRESS, "Enabling Sensors...");

        /*
         * With services discovered, we are going to reset our state machine and start
         * working through the sensors we need to enable
         */
        reset();
        enableNextSensor(gatt);
    }

    @Override
    public void onCharacteristicRead(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic, int status) {

        for (SensorInfo sensors : mSensorDevice.getSensorInfo()) {
            if (sensors.getDataChar().equals(characteristic.getUuid())) {
                //BroadcastUtil.broadcastUpdate(mContext, Constants.SENSOR_VALUES_CHANGED, characteristic, sensors.getMessageID());
                mSensorDevice.setSensorAttributeValue(sensors.getMessageID(), characteristic.getValue());
                BroadcastUtil.broadcastUpdate(mContext, Constants.SENSOR_VALUES_CHANGED, new double[]{
                        mSensorDevice.getTemperatureSensorValue(),
                        mSensorDevice.getHumiditySensorValue(),
                        mSensorDevice.getLightSensorValue(),
                        mSensorDevice.getPressureSensorValue(),
                        mSensorDevice.getNetAccelerationSensorValue(),
                        mSensorDevice.getTiltSensorValue()
                });
                setNotifyNextSensor(gatt);
            }
        }
    }

    @Override
    public void onCharacteristicWrite(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic, int status) {

        /*
         * If the enable flag was just written then we read the initial value but if the period
         * value was written then we are done with all the steps for a sensor and we advance
         * to the next sensor.
         */

        if(characteristic.getUuid().equals(mSensorDevice.getSensorInfo()[mState].getPeriodChar())){
            advance();
            enableNextSensor(gatt);
        } else
            readNextSensor(gatt);
    }

    @Override
    public void onCharacteristicChanged(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic) {
        /*
         * After notifications are enabled, all updates from the device on characteristic
         * value changes will be posted here.  Similar to read, we hand these up to the
         * UI thread to update the display.
         */
        for (SensorInfo sensors : mSensorDevice.getSensorInfo()) {
            if (sensors.getDataChar().equals(characteristic.getUuid())) {
                mSensorDevice.setSensorAttributeValue(sensors.getMessageID(), characteristic.getValue());
                BroadcastUtil.broadcastUpdate(mContext, Constants.SENSOR_VALUES_CHANGED, new double[]{
                        mSensorDevice.getTemperatureSensorValue(),
                        mSensorDevice.getHumiditySensorValue(),
                        mSensorDevice.getLightSensorValue(),
                        mSensorDevice.getPressureSensorValue(),
                        mSensorDevice.getNetAccelerationSensorValue(),
                        mSensorDevice.getTiltSensorValue()
                });
            }
        }
    }

    @Override
    public void onDescriptorWrite(BluetoothGatt gatt, BluetoothGattDescriptor descriptor, int status) {

        //Once notifications are enabled, we will set the update period/frequency on the sensor
        BluetoothGattCharacteristic characteristic;
        if (mState < mSensorDevice.getSensorInfo().length) {
            Log.d(TAG, "Setting Period value on" + " " + mSensorDevice.getSensorInfo()[mState].getSensorName());
            characteristic = gatt.getService(mSensorDevice.getSensorInfo()[mState].getServiceChar())
                    .getCharacteristic(mSensorDevice.getSensorInfo()[mState].getPeriodChar());

            characteristic.setValue(mSensorDevice.getSensorInfo()[mState].getPeriodValues());

            if(characteristic != null) {
                gatt.writeCharacteristic(characteristic);
            }
        }
        else {
            BroadcastUtil.broadcastUpdate(mContext, UI_MESSAGE, Constants.MSG_DISMISS, "");
            Log.i(TAG, "All Sensors Period Updated");
        }

    }

    @Override
    public void onReadRemoteRssi(BluetoothGatt gatt, int rssi, int status) {
        Log.d(TAG, "Remote RSSI: "+rssi);
    }

    @Override
    public void setSensorDevice(SensorDevice sensorDevice){
        mSensorDevice = sensorDevice;
    }

    private String connectionState(int status) {
        switch (status) {
            case BluetoothProfile.STATE_CONNECTED:
                return "Connected";
            case BluetoothProfile.STATE_DISCONNECTED:
                return "Disconnected";
            case BluetoothProfile.STATE_CONNECTING:
                return "Connecting";
            case BluetoothProfile.STATE_DISCONNECTING:
                return "Disconnecting";
            default:
                return String.valueOf(status);
        }
    }
}
