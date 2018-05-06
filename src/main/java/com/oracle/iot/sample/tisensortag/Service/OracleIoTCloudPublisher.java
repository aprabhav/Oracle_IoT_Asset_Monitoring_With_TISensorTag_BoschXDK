package com.oracle.iot.sample.tisensortag;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.support.v4.content.LocalBroadcastManager;

import java.io.IOException;

import oracle.iot.client.*;
import oracle.iot.client.device.*;
import oracle.iot.client.device.DirectlyConnectedDevice;

import static com.oracle.iot.sample.tisensortag.UIMessageConstants.UI_MESSAGE;

public class OracleIoTComm {
    private static final String FALL_ALERT_URN = "urn:oracle:asset:ti:sensortag:cc2650:alert:fall";
    private static final String FALL_ALERT_FIELD = "fall";
    private static final String TILT_ALERT_URN = "urn:oracle:asset:ti:sensortag:cc2650:alert:tilt";
    private static final String TILT_ALERT_FIELD = "tilt";
    private static final String TEMPERATURE_ATTRIBUTE = "temperature";
    private static final String HUMIDITY_ATTRIBUTE = "humidity";
    private static final String LIGHT_ATTRIBUTE = "light";
    private static final String PRESSURE_ATTRIBUTE = "pressure";
    private static final String NET_ACCELERATION_ATTRIBUTE = "netacceleration";
    private static final String X_AXIS_TILT_ATTRIBUTE = "xaxistilt";
    private static final String LONGITUDE_ATTRIBUTE = "ora_longitude";
    private static final String LATITUDE_ATTRIBUTE = "ora_latitude";
    private static final String ALTITUDE_ATTRIBUTE = "ora_altitude";

    private Double mLongitude = 77.669628;
    private Double mLatitude = 12.928662;

    private SensorDevice mSensorDevice;
    private DirectlyConnectedDevice mDCD;
    private DeviceModel dcdModel;
    private VirtualDevice virtualDevice;
    private GPSTracker gpsTracker;
    private Context mainContext;
    private SharedPreferences mSharedPreferences;
    private String mDeviceModelURN;


    public OracleIoTComm(Context context){
        mainContext = context;
        gpsTracker = new GPSTracker(mainContext);
        mSharedPreferences = mainContext.getSharedPreferences(
                mainContext.getString(R.string.preference_file_key), Context.MODE_PRIVATE);
    }

    public void setSensorDevice(SensorDevice device){
        mSensorDevice = device;
    }

    public void sendDeviceMessagesToCloud(){
            try {


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
                }

                if (!mDCD.isActivated()) {
                    mDCD.activate(mDeviceModelURN);
                }
                // Update an attribute on our virtual device.
                // This will result in a message being sent to the cloud service with the updated attribute value

                mLatitude = gpsTracker.getLatitude();
                mLongitude = gpsTracker.getLongitude();

                double tempValue = 0;
                double humidityValue = 0;
                double lightValue = 0;
                double pressureValue = 0;
                double [] netAccelerationValue = new double [2];
                boolean fallAlert = false;

                if (mSensorDevice != null){
                    tempValue = mSensorDevice.getTemperatureSensorValue();
                    humidityValue = mSensorDevice.getHumiditySensorValue();
                    lightValue = mSensorDevice.getLightSensorValue();
                    pressureValue = mSensorDevice.getPressureSensorValue();
                    netAccelerationValue = mSensorDevice.getNetAccelerationSensorValue();
                    fallAlert = mSensorDevice.isRapidFallAlert();
                }

                virtualDevice.update()
                        .set(TEMPERATURE_ATTRIBUTE, tempValue)
                        .set(HUMIDITY_ATTRIBUTE, humidityValue)
                        .set(LIGHT_ATTRIBUTE, lightValue)
                        .set(PRESSURE_ATTRIBUTE, pressureValue)
                        .set(NET_ACCELERATION_ATTRIBUTE, netAccelerationValue [0])
                        .set(X_AXIS_TILT_ATTRIBUTE, netAccelerationValue[1])
                        .set(LONGITUDE_ATTRIBUTE, mLongitude)
                        .set(LATITUDE_ATTRIBUTE, mLatitude)
                        .set(ALTITUDE_ATTRIBUTE, 10)
                        .finish();
                if (fallAlert){
                    Alert alert = virtualDevice.createAlert(FALL_ALERT_URN);
                    alert.set(FALL_ALERT_FIELD, mSensorDevice.readAndClearRapidFallAlert());
                    alert.raise();
                }
                if(netAccelerationValue[1] < 70){
                    Alert alert = virtualDevice.createAlert(TILT_ALERT_URN);
                    alert.set(TILT_ALERT_FIELD, netAccelerationValue[1]);
                    alert.raise();
                }
            }catch (IOException dse) {
                dse.printStackTrace();
                broadcastUpdate(UI_MESSAGE, UIMessageConstants.MSG_EXCEPTION, dse.getMessage());
            }catch (Exception dse) {
                dse.printStackTrace();
                broadcastUpdate(UI_MESSAGE, UIMessageConstants.MSG_EXCEPTION, dse.getMessage());
            }
    }

    private void broadcastUpdate(final String action, final int messageType, final String message) {
        final Intent intent = new Intent(action);
        intent.putExtra("type", messageType);
        intent.putExtra("message", message);
        LocalBroadcastManager.getInstance(mainContext).sendBroadcast(intent);
    }
}
