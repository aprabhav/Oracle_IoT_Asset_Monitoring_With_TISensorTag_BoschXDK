package com.oracle.iot.sample.tisensortag.Service;

import android.app.Notification;
import android.app.PendingIntent;
import android.arch.lifecycle.LifecycleService;
import android.arch.lifecycle.Observer;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.location.Location;
import android.os.Binder;
import android.os.IBinder;
import android.support.annotation.Nullable;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v7.app.NotificationCompat;

import com.oracle.iot.sample.tisensortag.Bluetooth.BleGattCallback;
import com.oracle.iot.sample.tisensortag.Bluetooth.BleScanCallback;
import com.oracle.iot.sample.tisensortag.Device.SensorDevice;
import com.oracle.iot.sample.tisensortag.Device.SensorDeviceFactory;
import com.oracle.iot.sample.tisensortag.MainActivity;
import com.oracle.iot.sample.tisensortag.R;
import com.oracle.iot.sample.tisensortag.Util.BroadcastUtil;
import com.oracle.iot.sample.tisensortag.Util.Constants;

import java.util.Properties;
import java.util.Timer;
import java.util.TimerTask;

public class SensorDeviceTrackerService extends LifecycleService {

    private Timer timer;
    private TimerTask timerTask;
    public static boolean IS_SERVICE_RUNNING = false;
    private OracleIoTCloudPublisher mOracleIoTCloudPublisher;
    private BleScanCallback mBLEScanCallback;
    private BleGattCallback mBLEGattCallback;
    private BluetoothAdapter mBluetoothAdapter;
    private BluetoothGatt mConnectedGatt;
    private SensorDevice mSensorDevice;
    private BluetoothDevice mBluetoothDevice;
    private Location mLocation;
    private GPSData mGPSData;
    private boolean mCloudConnect = false;

    private IBinder serviceBinder = new ServiceBinder();
    public class ServiceBinder extends Binder {
        public SensorDeviceTrackerService getService() {
            return SensorDeviceTrackerService.this;
        }
    }

    public SensorDeviceTrackerService() {
        super();
    }
    public SensorDeviceTrackerService(Context applicationContext) {
        super();
    }

    @Override
    public void onCreate(){
        super.onCreate();
        mOracleIoTCloudPublisher = new OracleIoTCloudPublisher(this);
        setupSensorDataUpdates();
        setupLocationUpdates();
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        super.onBind(intent);
        return serviceBinder;
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        super.onStartCommand(intent, flags, startId);
        if (intent.getAction().equals(Constants.ACTION.STARTFOREGROUND_ACTION)) {
            IS_SERVICE_RUNNING = true;
            showForegroundServiceNotification();
            mBluetoothDevice = (BluetoothDevice) intent.getParcelableExtra("BluetoothDevice");
            setupBLECommunication();
            startTimerForIoTCloudSync();
        } else if (intent.getAction().equals(Constants.ACTION.STOPFOREGROUND_ACTION)) {
            stopForeground(true);
            IS_SERVICE_RUNNING = false;
            stopSelf();
        }
        return START_STICKY;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();

        //Disconnect from any active tag connection
        if (mConnectedGatt != null) {
            mConnectedGatt.disconnect();
            mConnectedGatt = null;
        }
        mOracleIoTCloudPublisher.cleanup();
        mGPSData.removeUpdates();
        stopTimerTask();
    }

    @Override
    public boolean onUnbind(Intent intent) {
        super.onUnbind(intent);
        return false;
    }

    public void setIoTCloudConnect(boolean cloudConnect){
        mCloudConnect = cloudConnect;
    }

    public BluetoothDevice getConnectedDevice(){
        return mBluetoothDevice;
    }

    public void onLocationChanged(Location newLoc) {
        mLocation = newLoc;
    }
    public void onSensorValuesChanged() {
        if (mSensorDevice.getNetAccelerationSensorValue() < 0.8) {
            mOracleIoTCloudPublisher.sendDeviceAlertMessagesToCloud(Constants.FALL_ALERT_URN,
                    mSensorDevice.getNetAccelerationSensorValue());
        }

        if (mSensorDevice.getTiltSensorValue() < 70) {
            mOracleIoTCloudPublisher.sendDeviceAlertMessagesToCloud(Constants.TILT_ALERT_URN,
                    mSensorDevice.getTiltSensorValue());
        }
    }

    private void startTimerForIoTCloudSync() {
        //set a new Timer
        timer = new Timer();

        //initialize the TimerTask's job
        initializeTimerTask();

        //schedule the timer, to wake up every 5 second
        //int freqSeconds = PrefUtils.getIoTDataMessageFrequency(getApplicationContext());
        timer.schedule(timerTask, 2000, 5 * 1000);

    }

    private void initializeTimerTask(){
        timerTask = new TimerTask() {
            boolean isSetupIoTCloud = false;
            public void run() {
                if(mCloudConnect){
                    if(!isSetupIoTCloud){
                        isSetupIoTCloud = mOracleIoTCloudPublisher.setupIoTCloudConnect();
                    }
                    Properties iotMessageValues = new Properties();
                    iotMessageValues.put(Constants.TEMPERATURE_ATTRIBUTE, String.valueOf(mSensorDevice.getTemperatureSensorValue()));
                    iotMessageValues.put(Constants.HUMIDITY_ATTRIBUTE, String.valueOf(mSensorDevice.getHumiditySensorValue()));
                    iotMessageValues.put(Constants.LIGHT_ATTRIBUTE, String.valueOf(mSensorDevice.getLightSensorValue()));
                    iotMessageValues.put(Constants.PRESSURE_ATTRIBUTE, String.valueOf(mSensorDevice.getPressureSensorValue()));
                    iotMessageValues.put(Constants.NET_ACCELERATION_ATTRIBUTE, String.valueOf(mSensorDevice.getNetAccelerationSensorValue()));
                    iotMessageValues.put(Constants.X_AXIS_TILT_ATTRIBUTE, String.valueOf(mSensorDevice.getTiltSensorValue()));
                    iotMessageValues.put(Constants.LONGITUDE_ATTRIBUTE, String.valueOf(mLocation.getLongitude()));
                    iotMessageValues.put(Constants.LATITUDE_ATTRIBUTE, String.valueOf(mLocation.getLatitude()));
                    mOracleIoTCloudPublisher.sendDeviceMessagesToCloud(iotMessageValues);
                }
            }
        };
    }

    private void stopTimerTask() {
        //stop the timer, if it's not already null
        if (timer != null) {
            timer.cancel();
            timer = null;
        }
    }

    private void setupLocationUpdates() {
        mGPSData = new GPSData(this);
        mGPSData.getLocation().observe(this, new Observer<Location>() {
            @Override
            public void onChanged(@Nullable Location location) {
                onLocationChanged(location);
            }
        });
    }

    private void setupSensorDataUpdates(){
        LocalBroadcastManager.getInstance(this).registerReceiver(
                new BroadcastReceiver() {
                    @Override
                    public void onReceive(Context context, Intent intent) {
                        onSensorValuesChanged();
                    }
                }, new IntentFilter(Constants.SENSOR_VALUES_CHANGED)
        );
    }

    private void setupBLECommunication(){
        mSensorDevice = SensorDeviceFactory.getSensorDevice(mBluetoothDevice.getName());
        mBLEGattCallback = SensorDeviceFactory.getBleGattCallback(mBluetoothDevice.getName(), this);
        mBLEGattCallback.setSensorDevice(mSensorDevice);

        /*
         * Make a connection with the device using the special LE-specific
         * connectGatt() method, passing in a callback for GATT events
         */
        mConnectedGatt = mBluetoothDevice.connectGatt(this, false, mBLEGattCallback);
        BroadcastUtil.broadcastUpdate(this, Constants.UI_MESSAGE, Constants.MSG_PROGRESS, "Connecting to "+ mBluetoothDevice.getName()+"...");
    }

    private void showForegroundServiceNotification() {
        Intent notificationIntent = new Intent(this, MainActivity.class);
        notificationIntent.setAction(Constants.ACTION.MAIN_ACTION);
        notificationIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK
                | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        PendingIntent pendingIntent = PendingIntent.getActivity(this, 0,
                notificationIntent, 0);

        Intent stopIntent = new Intent(this, SensorDeviceTrackerService.class);
        stopIntent.setAction(Constants.ACTION.STOPFOREGROUND_ACTION);
        PendingIntent pStopIntent = PendingIntent.getService(this, 0,
                stopIntent, 0);

        Bitmap icon = BitmapFactory.decodeResource(getResources(),
                R.drawable.am_icon);

        Notification notification = new NotificationCompat.Builder(this)
                .setContentTitle("Asset Gateway App")
                .setTicker("Asset Gateway App")
                .setContentText("Asset Gateway Service is Running")
                .setSmallIcon(R.drawable.am_icon)
                .setContentIntent(pendingIntent)
                .setOngoing(true)
                .addAction(android.R.drawable.ic_lock_power_off, "Stop Service",
                        pStopIntent).build();

        startForeground(Constants.NOTIFICATION_ID.FOREGROUND_SERVICE, notification);

    }
}
