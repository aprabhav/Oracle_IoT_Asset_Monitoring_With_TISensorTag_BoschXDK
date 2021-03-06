package com.oracle.iot.sample.tisensortag.Device;

import android.content.Context;

import com.oracle.iot.sample.tisensortag.Bluetooth.BleGattCallback;
import com.oracle.iot.sample.tisensortag.Bluetooth.BoschXDKGattCallback;
import com.oracle.iot.sample.tisensortag.Bluetooth.TISensorTagGattCallback;
import com.oracle.iot.sample.tisensortag.Device.BoschXDK;
import com.oracle.iot.sample.tisensortag.Device.SensorDevice;
import com.oracle.iot.sample.tisensortag.Device.TISensorTag;

public class SensorDeviceFactory {

    public static final String TI_DEVICE_NAME = "CC2650 SensorTag";
    public static final String BOSCH_XDK_NAME = "ORACLE_IOT_XDK";
    //public static final String BOSCH_XDK_NAME = "BCDS_Virtual_Sensor";


    public static SensorDevice getSensorDevice(String deviceName){
        if (deviceName == null){
            return null;
        }
        if (deviceName.equals(TI_DEVICE_NAME)){
            return new TISensorTag();
        } else if(deviceName.equals(BOSCH_XDK_NAME)){
            return new BoschXDK();
        }
            return null;
    }

    public static BleGattCallback getBleGattCallback(String deviceName, Context context){
        if (deviceName == null){
            return null;
        }
        if (deviceName.compareTo(TI_DEVICE_NAME) == 0){
            return new TISensorTagGattCallback(context);
        } else if(deviceName.compareTo(BOSCH_XDK_NAME) == 0){
            return new BoschXDKGattCallback(context);
        }
        return null;
    }
}
