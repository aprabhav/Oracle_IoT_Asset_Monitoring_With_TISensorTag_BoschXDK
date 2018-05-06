package com.oracle.iot.sample.tisensortag.Bluetooth;


import android.bluetooth.BluetoothGattCallback;

import com.oracle.iot.sample.tisensortag.Device.SensorDevice;

public abstract class BleGattCallback extends BluetoothGattCallback{

    public abstract void setSensorDevice (SensorDevice device);
}
