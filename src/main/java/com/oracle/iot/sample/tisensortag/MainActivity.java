package com.oracle.iot.sample.tisensortag;

import android.Manifest;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.util.SparseArray;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.Window;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.CompoundButton;
import android.widget.ListView;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import static com.oracle.iot.sample.tisensortag.UIMessageConstants.DEVICE_AVAILABLE;
import static com.oracle.iot.sample.tisensortag.UIMessageConstants.DEVICE_CONNECTED;
import static com.oracle.iot.sample.tisensortag.UIMessageConstants.MSG_CLEAR;
import static com.oracle.iot.sample.tisensortag.UIMessageConstants.MSG_DISMISS;
import static com.oracle.iot.sample.tisensortag.UIMessageConstants.MSG_EXCEPTION;
import static com.oracle.iot.sample.tisensortag.UIMessageConstants.MSG_HUMIDITY;
import static com.oracle.iot.sample.tisensortag.UIMessageConstants.MSG_LIGHT;
import static com.oracle.iot.sample.tisensortag.UIMessageConstants.MSG_MOVEMENT;
import static com.oracle.iot.sample.tisensortag.UIMessageConstants.MSG_PRESSURE;
import static com.oracle.iot.sample.tisensortag.UIMessageConstants.MSG_PROGRESS;
import static com.oracle.iot.sample.tisensortag.UIMessageConstants.MSG_TEMPERATURE;
import static com.oracle.iot.sample.tisensortag.UIMessageConstants.MSG_XDK;
import static com.oracle.iot.sample.tisensortag.UIMessageConstants.SENSOR_CHARACTERISTIC_AVAILABLE;
import static com.oracle.iot.sample.tisensortag.UIMessageConstants.UI_MESSAGE;


public class MainActivity extends AppCompatActivity {

    private static final String TAG = "BluetoothGattActivity";
    private final int REQUEST_ENABLE_BT = 111;
    private final int REQUEST_LOCATION = 222;

    private ScheduledExecutorService scheduleTaskExecutor;

    private ProgressDialog mProgress;
    private TextView mTemperature, mHumidity, mPressure, mLight, mMovement, mTilt;
    private ListView mSensorListView;
    private ArrayAdapter<String> mSensorArrayAdapter;
    private List<String> mSensorsList;
    private List<Integer> mSensorHashCodeList;

    private boolean mProvCompletedState;
    private boolean mSensorConnectedState = false;

    private OracleIoTComm oracleIoTCloudConnector;
    private BleScanCallback mBLEScanCallback;
    private BleGattCallback mBLEGattCallback;
    private BluetoothAdapter mBluetoothAdapter;
    private SparseArray<BluetoothDevice> mDevices;
    private BluetoothGatt mConnectedGatt;

    private SensorDevice mSensorDevice;

    private Handler mHandler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
          }
    };

    private final BroadcastReceiver mBleUpdateReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            //BluetoothGattCharacteristic characteristic;
            byte [] characteristicValue;
            BluetoothDevice device;
            final String action = intent.getAction();
            Bundle extras = intent.getExtras();
            switch(action){
                case SENSOR_CHARACTERISTIC_AVAILABLE:
                    int characteristicType = extras.getInt("type");
                    characteristicValue = extras.getByteArray("characteristicValue");
                    switch (characteristicType){
                        case MSG_HUMIDITY:
                            if (characteristicValue == null) {
                                Log.w(TAG, "Error obtaining humidity value");
                                return;
                            }
                            updateHumidityValues(characteristicValue);
                            break;
                        case MSG_PRESSURE:
                            if (characteristicValue == null) {
                                Log.w(TAG, "Error obtaining pressure value");
                                return;
                            }
                            updatePressureValues(characteristicValue);
                            break;
                        case MSG_TEMPERATURE:
                            if (characteristicValue == null) {
                                Log.w(TAG, "Error obtaining temperature value");
                                return;
                            }
                            updateTemperatureValues(characteristicValue);
                            break;
                        case MSG_LIGHT:
                            if (characteristicValue == null) {
                                Log.w(TAG, "Error obtaining light value");
                                return;
                            }
                            updateLightValues(characteristicValue);
                            break;
                        case MSG_MOVEMENT:
                            if (characteristicValue == null) {
                                Log.w(TAG, "Error obtaining movement value");
                                return;
                            }
                            updateMovementValues(characteristicValue);
                            break;
                        case MSG_XDK:
                            if (characteristicValue == null) {
                                Log.w(TAG, "Error obtaining Bosch XDK values");
                                return;
                            }
                            updateBoschXDKValues(characteristicValue);
                            break;
                    }
                case UI_MESSAGE:
                    int messageType = extras.getInt("type");
                    String messageString = extras.getString("message");
                    switch (messageType){
                        case MSG_CLEAR:
                            clearDisplayValues();
                            break;
                        case MSG_DISMISS:
                            mProgress.hide();
                            //mSensorConnectedState = true;
                            showSensortags();
                            break;
                        case MSG_PROGRESS:
                            mProgress.setMessage(messageString);
                            if (!mProgress.isShowing()) {
                                mProgress.show();
                            }
                            break;
                        case MSG_EXCEPTION:
                            showExceptionAlert(messageString);
                            break;
                    }
                    break;
                case DEVICE_AVAILABLE:
                    String deviceName = extras.getString("name");
                    device = extras.getParcelable("device");
                    mDevices.put(device.hashCode(), device);
                    showSensortags();
                    break;
                case DEVICE_CONNECTED:
                    mSensorConnectedState = true;
                    break;
            }
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {

        requestWindowFeature(Window.FEATURE_INDETERMINATE_PROGRESS);

        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_main);
        Toolbar actionToolbar = (Toolbar) findViewById(R.id.action_toolbar);
        actionToolbar.setLogo(R.drawable.am_icon);
        actionToolbar.setContentInsetStartWithNavigation(0);
        setSupportActionBar(actionToolbar);

        getWindow().getDecorView().setBackgroundColor(Color.LTGRAY);
        setProgressBarIndeterminate(true);

        /*
         * A progress dialog will be needed while the connection process is
         * taking place
         */
        mProgress = new ProgressDialog(this);
        mProgress.setIndeterminate(true);
        mProgress.setCancelable(false);

        /*
         * We are going to display the results in some text fields
         */
        mTemperature = (TextView) findViewById(R.id.text_temperature);
        mHumidity = (TextView) findViewById(R.id.text_humidity);
        mPressure = (TextView) findViewById(R.id.text_pressure);
        mLight = (TextView) findViewById(R.id.text_light);
        mMovement = (TextView) findViewById(R.id.text_movement);
        mTilt = (TextView) findViewById(R.id.text_tilt);

        /*
         * Initializing sensor list - which will be show the scanned devices
         */
        mSensorListView = (ListView) findViewById(R.id.sensorList);
        String[] emptyArray = new String[] {"Not Connected to any device"};
        mSensorsList = new ArrayList<String>(Arrays.asList(emptyArray));
        mSensorArrayAdapter = new ArrayAdapter<String>
                (this, android.R.layout.simple_list_item_1, mSensorsList);
        mSensorListView.setAdapter(mSensorArrayAdapter);
        Integer[] emptyCodeArray = new Integer[] {};
        mSensorHashCodeList = new ArrayList<Integer>(Arrays.asList(emptyCodeArray));
        mSensorListView.setOnItemClickListener(new AdapterView.OnItemClickListener() {

            @Override
            public void onItemClick(AdapterView<?> parent, View view,
                                    int position, long id) {

                //First, disconnect any connected devices
                if(mSensorConnectedState){
                    if(mConnectedGatt != null){
                        mConnectedGatt.close();
                    }
                }

                int itemPosition = position;

                //Obtain the discovered device to connect with
                BluetoothDevice device = mDevices.get(mSensorHashCodeList.get(itemPosition));
                Log.i(TAG, "Connecting to " + device.getName());

                mSensorDevice = SensorDeviceFactory.getSensorDevice(device.getName());
                mBLEGattCallback = SensorDeviceFactory.getBleGattCallback(device.getName(), MainActivity.this);
                mBLEGattCallback.setSensorDevice(mSensorDevice);
                oracleIoTCloudConnector.setSensorDevice(mSensorDevice);

                /*
                 * Make a connection with the device using the special LE-specific
                 * connectGatt() method, passing in a callback for GATT events
                 */
                mConnectedGatt = device.connectGatt(MainActivity.this, false, mBLEGattCallback);

                //Display progress UI
                final Intent intent = new Intent(UI_MESSAGE);
                intent.putExtra("type", MSG_PROGRESS);
                intent.putExtra("message", "Connecting to "+ device.getName()+"...");
                LocalBroadcastManager.getInstance(MainActivity.this).sendBroadcast(intent);
            }

        });

        //Check if the provisioning is already done.
        SharedPreferences pref = getApplicationContext().getSharedPreferences(
                getString(R.string.preference_file_key), Context.MODE_PRIVATE);
        String bksPath = pref.getString(getString(R.string.ta_file_path), "");

        mProvCompletedState = (getIntent().getBooleanExtra("prov_completed", false)) || !(bksPath.equals(""));

        //Check app permissions

        if(ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED &&
                ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION,
                    Manifest.permission.ACCESS_COARSE_LOCATION}, REQUEST_LOCATION);
        }


        //An array to hold the list of scanned devices
        mDevices = new SparseArray<BluetoothDevice>();

        BluetoothManager manager = (BluetoothManager) getSystemService(BLUETOOTH_SERVICE);
        mBluetoothAdapter = manager.getAdapter();
        mBLEScanCallback = new BleScanCallback(this);

        //This is the scheduled task for sending data to IoT Cloud
        oracleIoTCloudConnector = new OracleIoTComm(this);

        //A button used to send sensor data to IoT Cloud
        Switch connectIoTCloud = (Switch) findViewById(R.id.switch_connectIoTCloud);
        connectIoTCloud.setVisibility(View.INVISIBLE);
        connectIoTCloud.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                if (isChecked){
                    if(scheduleTaskExecutor == null || scheduleTaskExecutor.isTerminated())
                        scheduleTaskExecutor = Executors.newScheduledThreadPool(5);

                    //Schedule this task to run every 10 seconds (or however long you want)
                    scheduleTaskExecutor.scheduleAtFixedRate(new Runnable() {
                        @Override
                        public void run() {

                            oracleIoTCloudConnector.sendDeviceMessagesToCloud();

                        }
                    }, 0, 5, TimeUnit.SECONDS);

                }
                else{
                    if(!scheduleTaskExecutor.isShutdown())
                        scheduleTaskExecutor.shutdown();
                }
            }
        });
    }

    @Override
    protected void onResume() {
        super.onResume();

        //registerReceiver(mBleUpdateReceiver, makeBLEUpdateIntentFilter());

        LocalBroadcastManager.getInstance(this).registerReceiver(mBleUpdateReceiver, makeBLEUpdateIntentFilter());

        SharedPreferences pref = getApplicationContext().getSharedPreferences(
                getString(R.string.preference_file_key), Context.MODE_PRIVATE);
        String bksPath = pref.getString(getString(R.string.ta_file_path), "");

        mProvCompletedState = (getIntent().getBooleanExtra("prov_completed", false)) || !(bksPath.equals(""));

        /*
         * We need to enforce that Bluetooth is first enabled, and take the
         * user to settings to enable it if they have not done so.
         */

        if (mBluetoothAdapter == null || !mBluetoothAdapter.isEnabled()) {
            //Bluetooth is disabled
            Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            startActivity(enableBtIntent);
            startActivityForResult(enableBtIntent, REQUEST_ENABLE_BT);
            return;
        }


        /*
         * Check for Bluetooth LE Support.  In production, our manifest entry will keep this
         * from installing on these devices, but this will allow test devices or other
         * sideloads to report whether or not the feature exists.
         */
        if (!getPackageManager().hasSystemFeature(PackageManager.FEATURE_BLUETOOTH_LE)) {
            Toast.makeText(this, "No LE Support.", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

    }

    @Override
    protected void onPause() {
        super.onPause();
        //Make sure dialog is hidden
        mProgress.dismiss();
        //Cancel any scans in progress
        mHandler.removeCallbacks(mStopRunnable);
        mHandler.removeCallbacks(mStartRunnable);
        mBluetoothAdapter.stopLeScan(mBLEScanCallback);
        //unregisterReceiver(mBleUpdateReceiver);
        LocalBroadcastManager.getInstance(this).unregisterReceiver(mBleUpdateReceiver);
    }

    @Override
    protected void onStop() {
        super.onStop();
        //Disconnect from any active tag connection
        if (mConnectedGatt != null) {
            mConnectedGatt.disconnect();
            mConnectedGatt = null;
        }
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        setIntent(intent);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Add the "find" option to the menu
        getMenuInflater().inflate(R.menu.main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.action_find:
                mDevices.clear();
                startScan();
                return true;
            case R.id.provision_settings:
                Intent anIntent = new Intent(MainActivity.this, UserInputActivity.class);
                startActivity(anIntent);
                return true;
            default:
                 return super.onOptionsItemSelected(item);
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == REQUEST_ENABLE_BT) {
            if (resultCode == Activity.RESULT_CANCELED) {
                //Bluetooth not enabled. Exit the application
                finish();
                return;
            }
        }
        super.onActivityResult(requestCode, resultCode, data);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode,
                                           String permissions[], int[] grantResults) {
        switch (requestCode) {
            case REQUEST_LOCATION: {
                // If request is cancelled, the result arrays are empty.
                if (grantResults.length > 0
                        && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                } else {
                    // permission denied
                    finish();
                    return;
                }
                return;
            }
        }
    }

    private void clearDisplayValues() {
        mTemperature.setText("---");
        mHumidity.setText("---");
        mPressure.setText("---");
        mLight.setText("---");
        mMovement.setText("---");
        mTilt.setText("---");
    }


    private Runnable mStopRunnable = new Runnable() {
        @Override
        public void run() {
            stopScan();
        }
    };
    private Runnable mStartRunnable = new Runnable() {
        @Override
        public void run() {
            startScan();
        }
    };

    private void startScan() {
        mBluetoothAdapter.startLeScan(mBLEScanCallback);
        setProgressBarIndeterminateVisibility(true);
        mHandler.postDelayed(mStopRunnable, 2500);
    }

    private void stopScan() {
        mBluetoothAdapter.stopLeScan(mBLEScanCallback);
        setProgressBarIndeterminateVisibility(false);
    }

    private void showSensortags () {

        //Add any device elements we've discovered to the list
        if(mSensorConnectedState && mProvCompletedState){
            //If already connected then enable 'Connect to Cloud' button
            findViewById(R.id.switch_connectIoTCloud).setVisibility(View.VISIBLE);
        }
        else{
            mSensorsList.clear();
            mSensorHashCodeList.clear();
            for (int i=0; i < mDevices.size(); i++) {
                BluetoothDevice device = mDevices.valueAt(i);
                mSensorsList.add(device.getName() + " Device");
                mSensorHashCodeList.add(mDevices.keyAt(i));
            }
            mSensorArrayAdapter.notifyDataSetChanged();
        }

    }

    private static IntentFilter makeBLEUpdateIntentFilter() {
        final IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(UI_MESSAGE);
        intentFilter.addAction(DEVICE_AVAILABLE);
        intentFilter.addAction(DEVICE_CONNECTED);
        intentFilter.addAction(SENSOR_CHARACTERISTIC_AVAILABLE);
        return intentFilter;
    }

    private void showExceptionAlert(String msg){
        AlertDialog alertDialog = new AlertDialog.Builder(MainActivity.this).create();
        alertDialog.setTitle("Something is wrong");
        alertDialog.setIcon(R.drawable.error);
        alertDialog.setMessage(msg);
        alertDialog.setButton(AlertDialog.BUTTON_NEUTRAL, "OK",
                new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int which) {
                        dialog.dismiss();
                        Switch connectIoTCloud = (Switch) findViewById(R.id.switch_connectIoTCloud);
                        connectIoTCloud.setChecked(false);
                    }
                });
        alertDialog.show();
    }

    /* Methods to update the sensor value in UI */


    private void updateHumidityValues(byte[] characteristicValue) {
        double humidity = mSensorDevice.extractHumidity(characteristicValue);
        mHumidity.setText(String.format("%.0f%%", humidity));
    }

    private void updateTemperatureValues(byte[] characteristicValue) {
        double temperature = mSensorDevice.extractTemperature(characteristicValue);
        mTemperature.setText(String.format("%.0f\u00B0C", temperature));
    }

    private void updateLightValues(byte[] characteristicValue) {
        double light = mSensorDevice.extractLight(characteristicValue);
        mLight.setText(String.format("%.0f Lux", light));
    }

    private void updateMovementValues(byte[] characteristicValue) {
        double [] movement = mSensorDevice.extractMovement(characteristicValue);
        mMovement.setText(String.format("%.2f G", movement[0]));
        mTilt.setText(String.format("%.2f°", movement[1]));

        /*
         *  Magnetometer test code. Values not displayed in the UI yet.
         */
        //double magnetism = mSensorDevice.extractMagnetism(characteristicValue);
        //mMovement.setText(String.format("%.2f uT", magnetism));
    }

    private void updatePressureValues(byte[] characteristicValue) {
        double pressure = mSensorDevice.extractBarometer(characteristicValue);
        mPressure.setText(String.format("%.0f mBar", pressure));
    }

    private void updateBoschXDKValues (byte[] characteristicValue){
        updateMovementValues(characteristicValue);
        updateLightValues(characteristicValue);
        updatePressureValues(characteristicValue);
        updateTemperatureValues(characteristicValue);
        updateHumidityValues(characteristicValue);
    }

}
