package com.oracle.iot.sample.tisensortag;


import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCallback;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattDescriptor;

public abstract class BleGattCallback extends BluetoothGattCallback{

    abstract void setSensorDevice (SensorDevice device);

    @Override
    abstract public void onConnectionStateChange(BluetoothGatt gatt, int status, int newState);

    @Override
    abstract public void onServicesDiscovered(BluetoothGatt gatt, int status);

    @Override
    abstract public void onCharacteristicRead(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic, int status);

    @Override
    abstract public void onCharacteristicWrite(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic, int status);

    @Override
    abstract public void onCharacteristicChanged(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic);

    @Override
    abstract public void onDescriptorWrite(BluetoothGatt gatt, BluetoothGattDescriptor descriptor, int status);

    @Override
    abstract public void onReadRemoteRssi(BluetoothGatt gatt, int rssi, int status);
}
