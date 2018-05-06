package com.oracle.iot.sample.tisensortag.Bluetooth;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.Context;
import android.util.Log;

import com.oracle.iot.sample.tisensortag.Util.BroadcastUtil;
import com.oracle.iot.sample.tisensortag.Device.SensorDeviceFactory;
import com.oracle.iot.sample.tisensortag.Util.Constants;

import static android.content.ContentValues.TAG;

public class BleScanCallback implements BluetoothAdapter.LeScanCallback {

    private Context mContext;


    public BleScanCallback (Context context){
        mContext = context;
    }

    @Override
    public void onLeScan(BluetoothDevice device, int rssi, byte[] scanRecord) {
        Log.i(TAG, "New LE Device: " + device.getName() + " @ " + rssi);

        /*
         * We are looking for specific BLE devices only, so validate the name
         * that each device reports before adding it to our collection
         */

        int deviceType = device.getType();
        String deviceName = device.getName();

        if (isSupportedDevice(deviceType, deviceName)){
            BroadcastUtil.broadcastUpdate(mContext, Constants.DEVICE_AVAILABLE, device);
        }
    }

    private boolean isSupportedDevice(int deviceType, String deviceName) {
        if(((deviceType == BluetoothDevice.DEVICE_TYPE_DUAL) || (deviceType == BluetoothDevice.DEVICE_TYPE_LE)) && deviceName != null){
            if ((deviceName.compareTo(SensorDeviceFactory.TI_DEVICE_NAME) == 0) || (deviceName.compareTo(SensorDeviceFactory.BOSCH_XDK_NAME) == 0)) {
                return true;
            }
        }
        return false;
    }
}
