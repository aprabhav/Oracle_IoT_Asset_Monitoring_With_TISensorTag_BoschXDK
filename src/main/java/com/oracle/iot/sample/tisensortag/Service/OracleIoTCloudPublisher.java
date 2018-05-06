package com.oracle.iot.sample.tisensortag.Service;

import android.content.Context;
import android.content.SharedPreferences;

import com.oracle.iot.sample.tisensortag.Util.BroadcastUtil;
import com.oracle.iot.sample.tisensortag.R;
import com.oracle.iot.sample.tisensortag.Util.Constants;

import java.io.IOException;
import java.util.Properties;

import oracle.iot.client.*;
import oracle.iot.client.device.*;
import oracle.iot.client.device.DirectlyConnectedDevice;

public class OracleIoTCloudPublisher {

    private Double mLongitude = 77.669628;
    private Double mLatitude = 12.928662;

    private DirectlyConnectedDevice mDCD;
    private DeviceModel dcdModel;
    private VirtualDevice virtualDevice;
    private Context mainContext;
    private SharedPreferences mSharedPreferences;
    private String mDeviceModelURN;


    public OracleIoTCloudPublisher(Context context){
        mainContext = context;
        mSharedPreferences = mainContext.getSharedPreferences(
                mainContext.getString(R.string.preference_file_key), Context.MODE_PRIVATE);
    }

    public boolean setupIoTCloudConnect(){
        boolean result = false;
        try{
            if (mDCD == null) {

                mDeviceModelURN = mSharedPreferences.getString(mainContext.getString(R.string.dm_urn),"");

                // Create the directly-connected device instance
                if(mSharedPreferences.getBoolean(mainContext.getString(R.string.use_provided_bks), true)){
                    mDCD = new DirectlyConnectedDevice(mainContext);
                }else{
                    mDCD = new DirectlyConnectedDevice(mSharedPreferences.getString(mainContext.getString(R.string.ta_file_path),""), mSharedPreferences.getString(mainContext.getString(R.string.ta_password),""));
                }

                if (!mDCD.isActivated()) {
                    mDCD.activate(mDeviceModelURN);
                }
                dcdModel = mDCD.getDeviceModel(mDeviceModelURN);
                // Set up a virtual device based on our device model
                virtualDevice = mDCD.createVirtualDevice(mDCD.getEndpointId(), dcdModel);
                result = true;
            }

            if (mDCD != null && !mDCD.isActivated()) {
                mDCD.activate(mDeviceModelURN);
                result = true;
            }
            return result;
        } catch (IOException dse) {
            dse.printStackTrace();
            BroadcastUtil.broadcastUpdate(mainContext, Constants.UI_MESSAGE, Constants.MSG_EXCEPTION, dse.getMessage());
            return result;
        }catch (Exception dse) {
            dse.printStackTrace();
            BroadcastUtil.broadcastUpdate(mainContext, Constants.UI_MESSAGE, Constants.MSG_EXCEPTION, dse.getMessage());
            return result;
        }
    }

    public void sendDeviceMessagesToCloud(Properties messageValues){
        try {
            // Update an attribute on our virtual device.
            // This will result in a message being sent to the cloud service with the updated attribute value
            double netAccelerationValue = Double.parseDouble(messageValues.getProperty(Constants.NET_ACCELERATION_ATTRIBUTE));

            virtualDevice.update()
                    .set(Constants.TEMPERATURE_ATTRIBUTE, Double.parseDouble(messageValues.getProperty(Constants.TEMPERATURE_ATTRIBUTE)))
                    .set(Constants.HUMIDITY_ATTRIBUTE, Double.parseDouble(messageValues.getProperty(Constants.HUMIDITY_ATTRIBUTE)))
                    .set(Constants.LIGHT_ATTRIBUTE, Double.parseDouble(messageValues.getProperty(Constants.LIGHT_ATTRIBUTE)))
                    .set(Constants.PRESSURE_ATTRIBUTE, Double.parseDouble(messageValues.getProperty(Constants.PRESSURE_ATTRIBUTE)))
                    .set(Constants.NET_ACCELERATION_ATTRIBUTE, netAccelerationValue)
                    .set(Constants.X_AXIS_TILT_ATTRIBUTE, Double.parseDouble(messageValues.getProperty(Constants.X_AXIS_TILT_ATTRIBUTE)))
                    .set(Constants.LONGITUDE_ATTRIBUTE, Double.parseDouble(messageValues.getProperty(Constants.LONGITUDE_ATTRIBUTE)))
                    .set(Constants.LATITUDE_ATTRIBUTE, Double.parseDouble(messageValues.getProperty(Constants.LATITUDE_ATTRIBUTE)))
                    .set(Constants.ALTITUDE_ATTRIBUTE, 10)
                    .finish();
        }catch (Exception dse) {
            dse.printStackTrace();
            BroadcastUtil.broadcastUpdate(mainContext, Constants.UI_MESSAGE, Constants.MSG_EXCEPTION, dse.getMessage());
        }
    }

    public void sendDeviceAlertMessagesToCloud(String alertType, double alertValue){
        try {
            if(virtualDevice != null){
                Alert alert;
                switch (alertType) {
                    case Constants.FALL_ALERT_URN:
                        alert = virtualDevice.createAlert(Constants.FALL_ALERT_URN);
                        alert.set(Constants.FALL_ALERT_FIELD, alertValue);
                        alert.raise();
                        break;
                    case Constants.TILT_ALERT_URN:
                        alert = virtualDevice.createAlert(Constants.TILT_ALERT_URN);
                        alert.set(Constants.TILT_ALERT_FIELD, alertValue);
                        alert.raise();
                        break;
                }
            }
        }catch (Exception dse) {
            dse.printStackTrace();
            BroadcastUtil.broadcastUpdate(mainContext, Constants.UI_MESSAGE, Constants.MSG_EXCEPTION, dse.getMessage());
        }
    }

    public void cleanup() {
        try {
            if(mDCD != null)
                mDCD.close();
        } catch (Exception dse) {
            dse.printStackTrace();
            BroadcastUtil.broadcastUpdate(mainContext, Constants.UI_MESSAGE, Constants.MSG_EXCEPTION, dse.getMessage());
        }
    }
}
