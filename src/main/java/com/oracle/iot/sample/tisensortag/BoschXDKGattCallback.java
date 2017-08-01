package com.oracle.iot.sample.tisensortag;


import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattDescriptor;
import android.bluetooth.BluetoothProfile;
import android.content.Context;
import android.util.Log;

import static android.content.ContentValues.TAG;
import static com.oracle.iot.sample.tisensortag.UIMessageConstants.DEVICE_CONNECTED;
import static com.oracle.iot.sample.tisensortag.UIMessageConstants.MSG_PROGRESS;
import static com.oracle.iot.sample.tisensortag.UIMessageConstants.UI_MESSAGE;

public class BoschXDKGattCallback extends BleGattCallback {

    private SensorDevice mSensorDevice;
    private Context mContext;

    BoschXDKGattCallback(Context context){
        mContext = context;
    }

    /* State Machine Tracking */
    private int mState = 0;

    private void reset() { mState = 0; }

    private void advance() { mState++; }

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
            BroadcastUtil.broadcastUpdate(mContext, UI_MESSAGE, UIMessageConstants.MSG_DISMISS, "");
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
            BroadcastUtil.broadcastUpdate(mContext, UIMessageConstants.UI_MESSAGE, UIMessageConstants.MSG_CLEAR, "");
        } else if (status != BluetoothGatt.GATT_SUCCESS) {
            /*
             * If there is a failure at any stage, simply disconnect
             */
            gatt.disconnect();
        }
    }

    @Override
    public void onServicesDiscovered(BluetoothGatt gatt, int status) {
        Log.d(TAG, "Services Discovered: "+ status);
        BroadcastUtil.broadcastUpdate(mContext, UIMessageConstants.UI_MESSAGE, UIMessageConstants.MSG_PROGRESS, "Enabling Sensors...");
        reset();
        setNotifyNextSensor(gatt);
    }

    @Override
    public void onCharacteristicRead(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic, int status) {
    }

    @Override
    public void onCharacteristicWrite(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic, int status) {
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
                BroadcastUtil.broadcastUpdate(mContext, UIMessageConstants.SENSOR_CHARACTERISTIC_AVAILABLE, characteristic, sensors.getMessageID());
            }
        }

    }

    @Override
    public void onDescriptorWrite(BluetoothGatt gatt, BluetoothGattDescriptor descriptor, int status) {
        //Once notifications are enabled, we send a start comm

        BluetoothGattCharacteristic characteristic = gatt.getService(mSensorDevice.getSensorInfo()[mState].getServiceChar())
                .getCharacteristic(mSensorDevice.getSensorInfo()[mState].getConfigChar());

        characteristic.setValue(mSensorDevice.getSensorInfo()[mState].getConfigValues());

        if(characteristic != null) {
            gatt.writeCharacteristic(characteristic);
        }

        BroadcastUtil.broadcastUpdate(mContext, UI_MESSAGE, UIMessageConstants.MSG_DISMISS, "");
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
