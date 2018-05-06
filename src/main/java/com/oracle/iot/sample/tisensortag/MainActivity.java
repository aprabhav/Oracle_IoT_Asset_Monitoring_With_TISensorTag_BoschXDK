package com.oracle.iot.sample.tisensortag;

import android.Manifest;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothManager;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
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

import com.oracle.iot.sample.tisensortag.Bluetooth.BleScanCallback;
import com.oracle.iot.sample.tisensortag.Service.SensorDeviceTrackerService;
import com.oracle.iot.sample.tisensortag.Util.Constants;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;

public class MainActivity extends AppCompatActivity {

    private static final String TAG = "BluetoothGattActivity";
    private final int REQUEST_ENABLE_BT = 111;
    private final int REQUEST_LOCATION = 222;

    private ProgressDialog mProgress;
    private TextView mTemperature, mHumidity, mPressure, mLight, mMovement, mTilt;
    private ListView mSensorListView;
    private ArrayAdapter<String> mSensorArrayAdapter;
    private List<String> mSensorsList;
    private List<Integer> mSensorHashCodeList;
    private boolean mProvCompletedState;
    private boolean mSensorConnectedState = false;
    private BleScanCallback mBLEScanCallback;
    private BluetoothAdapter mBluetoothAdapter;
    private SparseArray<BluetoothDevice> mDevices;
    SensorDeviceTrackerService mService = null;

    private Handler mHandler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
          }
    };

    private final BroadcastReceiver mBleUpdateReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            BluetoothDevice device;
            final String action = intent.getAction();
            Bundle extras = intent.getExtras();
            switch(action){
                case Constants.SENSOR_VALUES_CHANGED:
                    onSensorValuesChanged(extras.getDoubleArray(Constants.EXTRA_SENSORVALUES));
                    break;
                case Constants.UI_MESSAGE:
                    int messageType = extras.getInt("type");
                    String messageString = extras.getString("message");
                    switch (messageType){
                        case Constants.MSG_CLEAR:
                            mSensorConnectedState = false;
                            updateUI();
                            break;
                        case Constants.MSG_DISMISS:
                            mProgress.hide();
                            //mSensorConnectedState = true;
                            updateUI();
                            break;
                        case Constants.MSG_PROGRESS:
                            mProgress.setMessage(messageString);
                            if (!mProgress.isShowing()) {
                                mProgress.show();
                            }
                            break;
                        case Constants.MSG_EXCEPTION:
                            showExceptionAlert(messageString);
                            break;
                    }
                    break;
                case Constants.DEVICE_AVAILABLE:
                    String deviceName = extras.getString("name");
                    device = extras.getParcelable("device");
                    mDevices.put(device.hashCode(), device);
                    updateUI();
                    break;
                case Constants.DEVICE_CONNECTED:
                    mSensorConnectedState = true;
                    updateUI();
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

        initializeUI();
        if(SensorDeviceTrackerService.IS_SERVICE_RUNNING){
            if (mService == null) {
                Intent intentBind = new Intent(MainActivity.this, SensorDeviceTrackerService.class);
                bindService(intentBind, mServiceConnection, 0);
            }
            mSensorConnectedState = true;
        }

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
    }

    @Override
    protected void onResume() {
        super.onResume();
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
        if(mBluetoothAdapter != null)
            mBluetoothAdapter.stopLeScan(mBLEScanCallback);
        LocalBroadcastManager.getInstance(this).unregisterReceiver(mBleUpdateReceiver);
    }

    @Override
    protected void onStop() {
        super.onStop();
        if (mService != null) {
            unbindService(mServiceConnection);
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

    private void initializeUI(){
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
        mTemperature = findViewById(R.id.text_temperature);
        mHumidity = findViewById(R.id.text_humidity);
        mPressure = findViewById(R.id.text_pressure);
        mLight = findViewById(R.id.text_light);
        mMovement = findViewById(R.id.text_movement);
        mTilt = findViewById(R.id.text_tilt);

        /*
         * Initializing sensor list - which will be show the scanned devices
         */
        mSensorListView = findViewById(R.id.sensorList);
        String[] emptyArray = new String[] {"Not Connected to any device"};
        mSensorsList = new ArrayList<String>(Arrays.asList(emptyArray));
        mSensorArrayAdapter = new ArrayAdapter<String>
                (this, android.R.layout.simple_list_item_1, mSensorsList);
        mSensorListView.setAdapter(mSensorArrayAdapter);
        Integer[] emptyCodeArray = new Integer[] {};
        mSensorHashCodeList = new ArrayList<Integer>(Arrays.asList(emptyCodeArray));
        mSensorListView.setOnItemClickListener(mOnListItemClickListener);

        //An array to hold the list of scanned devices
        mDevices = new SparseArray<BluetoothDevice>();

        BluetoothManager manager = (BluetoothManager) getSystemService(BLUETOOTH_SERVICE);
        mBluetoothAdapter = manager.getAdapter();
        mBLEScanCallback = new BleScanCallback(this);

        //A button used to send sensor data to IoT Cloud
        Switch connectIoTCloud = (Switch) findViewById(R.id.switch_connectIoTCloud);
        connectIoTCloud.setVisibility(View.INVISIBLE);
        connectIoTCloud.setOnCheckedChangeListener(mConnectToIoTCloudListener);
    }

    private final CompoundButton.OnCheckedChangeListener mConnectToIoTCloudListener = new CompoundButton.OnCheckedChangeListener(){
        @Override
        public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
            if(mService != null){
                mService.setIoTCloudConnect(isChecked);
            }
        }
    };

    private final AdapterView.OnItemClickListener mOnListItemClickListener = new AdapterView.OnItemClickListener(){

        @Override
        public void onItemClick(AdapterView<?> parent, View view,
                                int position, long id) {

            int itemPosition = position;

            //Obtain the discovered device to connect with
            BluetoothDevice device = mDevices.get(mSensorHashCodeList.get(itemPosition));
            Log.i(TAG, "Connecting to " + device.getName());

            Intent service = new Intent(MainActivity.this, SensorDeviceTrackerService.class);
            if (!SensorDeviceTrackerService.IS_SERVICE_RUNNING) {
                service.setAction(Constants.ACTION.STARTFOREGROUND_ACTION);
                service.putExtra("BluetoothDevice", device);
                SensorDeviceTrackerService.IS_SERVICE_RUNNING = true;
                //button.setText("Stop Service");
            } else {
                service.setAction(Constants.ACTION.STOPFOREGROUND_ACTION);
                SensorDeviceTrackerService.IS_SERVICE_RUNNING = false;
                mSensorConnectedState = false;
                updateUI();
                //button.setText("Start Service");
            }
            startService(service);

            if (mService == null) {
                Intent intentBind = new Intent(MainActivity.this, SensorDeviceTrackerService.class);
                bindService(intentBind, mServiceConnection, 0);
            }
        }
    };

    private ServiceConnection mServiceConnection = new ServiceConnection() {
        public void onServiceConnected(ComponentName className, IBinder service) {
            mService = ((SensorDeviceTrackerService.ServiceBinder)service).getService();
            BluetoothDevice device = mService.getConnectedDevice();
            mDevices.put(device.hashCode(), device);
            mSensorConnectedState = true;
            updateUI();
        }

        public void onServiceDisconnected(ComponentName className) {
            mService = null;
            mSensorConnectedState = false;
            mDevices.clear();
            updateUI();
        }
    };

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

    private void updateUI() {

        //Add any device elements we've discovered to the list
        if(mSensorConnectedState && mProvCompletedState){
            //If already connected then enable 'Connect to Cloud' button
            findViewById(R.id.switch_connectIoTCloud).setVisibility(View.VISIBLE);
        }
        else{
            clearDisplayValues();
            findViewById(R.id.switch_connectIoTCloud).setVisibility(View.INVISIBLE);
        }

        mSensorsList.clear();
        mSensorHashCodeList.clear();
        for (int i=0; i < mDevices.size(); i++) {
            BluetoothDevice device = mDevices.valueAt(i);
            mSensorsList.add(device.getName() + " Device");
            mSensorHashCodeList.add(mDevices.keyAt(i));
        }
        mSensorArrayAdapter.notifyDataSetChanged();
    }

    private static IntentFilter makeBLEUpdateIntentFilter() {
        final IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(Constants.UI_MESSAGE);
        intentFilter.addAction(Constants.DEVICE_AVAILABLE);
        intentFilter.addAction(Constants.DEVICE_CONNECTED);
        intentFilter.addAction(Constants.SENSOR_VALUES_CHANGED);
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

    /* Method to update the sensor value in UI */

    private void onSensorValuesChanged(double[] sensorValues){
        mTemperature.setText(String.format(Locale.US, "%.0f\u00B0C", sensorValues[0]));
        mHumidity.setText(String.format(Locale.US,"%.0f%%", sensorValues[1]));
        mLight.setText(String.format(Locale.US,"%.0f Lux", sensorValues[2]));
        mPressure.setText(String.format(Locale.US,"%.0f mBar", sensorValues[3]));
        mMovement.setText(String.format(Locale.US,"%.2f G", sensorValues[4]));
        mTilt.setText(String.format(Locale.US,"%.2f°", sensorValues[5]));
    }
}
