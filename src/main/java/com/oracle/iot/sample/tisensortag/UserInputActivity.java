/*
 * Copyright (c) 2016 Oracle and/or its affiliates.  All rights reserved.
 *
 * This software is dual-licensed to you under the MIT License (MIT) and
 * the Universal Permissive License (UPL).  See the LICENSE file in the root
 * directory for license terms.  You may choose either license, or both.
 */

package com.oracle.iot.sample.tisensortag;

import android.Manifest;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.os.Bundle;
import android.os.Environment;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.EditText;

import com.oracle.iot.sample.tisensortag.Util.FileManager;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;


public class UserInputActivity extends AppCompatActivity {
    private static final String ERROR_TAG = "IOT_ERROR";
    private static final String MSG_TAG = "IOT";
    // UI references.
    private EditText mSharedSecretView;
    private EditText mProvisioningFileView;
    private EditText mDeviceModelURNView;
    private View mProgressView;
    private View mUserInputFormView;

    private String mSharedSecret;
    private String mProvFileName;
    private String mDeviceModelURN;
    private File mProvFile;
    private Activity act;

    public static final int REQUEST_STORAGE=1;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        if(getIntent().getBooleanExtra("EXIT", false)){
            finish();
            System.exit(0);
        }
        super.onCreate(savedInstanceState);
        act = this;

        setContentView(R.layout.activity_user_input);
        Toolbar actionToolbar = (Toolbar) findViewById(R.id.action_toolbar);
        actionToolbar.setLogo(R.drawable.am_icon);
        setSupportActionBar(actionToolbar);
        getWindow().getDecorView().setBackgroundColor(Color.LTGRAY);

        if(ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED){
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE},
                    1);

        }
        mProvisioningFileView = (EditText)findViewById(R.id.provisioning_file);
        mSharedSecretView = (EditText) findViewById(R.id.shared_secret);
        mDeviceModelURNView = (EditText) findViewById(R.id.devicemodel_urn);


        if (!needUserInput()) {

            SharedPreferences pref = getApplicationContext().getSharedPreferences(
                    getString(R.string.preference_file_key), Context.MODE_PRIVATE);

            mProvisioningFileView.setText(pref.getString(getString(R.string.ta_file_path), ""));
            mSharedSecretView.setText(pref.getString(getString(R.string.ta_password), ""));
            mDeviceModelURNView.setText(pref.getString(getString(R.string.dm_urn), ""));
        }

        Button fileSelectionButton = (Button) findViewById(R.id.file_selection_button);
        fileSelectionButton.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View view) {

                new FileManager(act).setFileListener(new FileManager.FileSelectedListener() {
                    @Override
                    public void fileSelected(final File file) {
                        mProvisioningFileView.setText(file.toString());
                    }
                }).showDialog();
            }
        });

        Button provButton = (Button) findViewById(R.id.provision_button);
        provButton.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View view) {
                validateAndStoreParameters();
            }
        });

        Button cancelButton = (Button) findViewById(R.id.cancel_button);
        cancelButton.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View view) {
                cancelAndReturn();
            }
        });

        Button resetButton = (Button) findViewById(R.id.reset_button);
        resetButton.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View view) {
                resetApp();
            }
        });

        mUserInputFormView = findViewById(R.id.input_form);
        mProgressView = findViewById(R.id.p_progress);
    }

    boolean needUserInput(){
        SharedPreferences pref = getApplicationContext().getSharedPreferences(
                getString(R.string.preference_file_key), Context.MODE_PRIVATE);
        SharedPreferences.Editor prefsEditor = pref.edit();
        String bksPath = pref.getString(getString(R.string.ta_file_path), "");
        boolean useProvidedBKS = bksPath.equals("");
        boolean bksProvided = isBKSProvided();
        if(!getIntent().getBooleanExtra("RESET", false)){
            // starting for the first time
            if(useProvidedBKS){
                if(!bksProvided){
                    prefsEditor.putBoolean(getString(R.string.use_provided_bks), false);
                    prefsEditor.apply();
                    return true;
                }else {
                    prefsEditor.putBoolean(getString(R.string.use_provided_bks), true);
                    prefsEditor.apply();
                    return false;
                }
            }else{
                prefsEditor.putBoolean(getString(R.string.use_provided_bks), false);
                prefsEditor.apply();

                File bksFile = new File(bksPath);
                if(bksFile.exists() && !bksFile.isDirectory()){
                    // already have a stored bks, use that one, no need to show user the input screen
                    return false;
                }
                return true;
            }
        }else{
            // remove last stored assets
            File[] files = getApplicationContext().getFilesDir().listFiles();
            for(File f : files){               
                f.delete();                
            }
        }
        return true;
    }

    private boolean isBKSProvided() {
        InputStream configInStream = null;
        boolean bksProvided = false;
        try {
            configInStream = (InputStream) getApplicationContext().getAssets().open("trustedAssets.properties");
            Properties config = new Properties();
            config.load(configInStream);
            String taStoreFileName = config.getProperty("oracle.iot.client.trustedAssetsStore","");
            if(!taStoreFileName.equals("")){
                InputStream is = (InputStream) getApplicationContext().getAssets().open(taStoreFileName);
                bksProvided = true;
                is.close();
            }
        } catch (Exception ex) {
            Log.d(ERROR_TAG, "BKS Not provided" + ex.toString());
        } finally {
            try {
                if (configInStream != null) {
                    configInStream.close();
                }
            } catch (IOException ex) {
            }
        }
        return bksProvided;
    }


    private void validateAndStoreParameters() {
        // Reset errors.
        mSharedSecretView.setError(null);
        mProvisioningFileView.setError(null);
        mDeviceModelURNView.setError(null);

        mSharedSecret = mSharedSecretView.getText().toString();
        mProvFileName = mProvisioningFileView.getText().toString();
        mDeviceModelURN = mDeviceModelURNView.getText().toString();

        mProvFile = new File(mProvFileName);
        boolean cancel = false;
        View focusView = null;

        // Check for a valid sharedSecret, if the user entered one.
        if (TextUtils.isEmpty(mSharedSecret)) {
            mSharedSecretView.setError(getString(R.string.error_field_required));
            focusView = mSharedSecretView;
            cancel = true;
        }

        if (TextUtils.isEmpty(mProvFileName)) {
            mProvisioningFileView.setError(getString(R.string.error_field_required));
            focusView = mProvisioningFileView;
            cancel = true;
        }

        if (TextUtils.isEmpty(mDeviceModelURN)) {
            mDeviceModelURNView.setError(getString(R.string.error_field_required));
            focusView = mDeviceModelURNView;
            cancel = true;
        }

        if (!mProvFile.exists()) {
            mProvisioningFileView.setError("File does not exist");
            focusView = mProvisioningFileView;
            cancel = true;
        }

        if (cancel) {
            // There was an error; focus the first
            // form field with an error.
            focusView.requestFocus();
        } else {
            // copy the file into local directory
            String destinationFileName = mProvFileName.substring(mProvFileName.lastIndexOf("/")+1);
            mProvFileName = getApplicationContext().getFilesDir().getPath() + "/" + destinationFileName;
            SharedPreferences pref = getApplicationContext().getSharedPreferences(
                    getString(R.string.preference_file_key), Context.MODE_PRIVATE);
            SharedPreferences.Editor prefsEditor = pref.edit();
            prefsEditor.putString(getString(R.string.ta_password), mSharedSecret);
            prefsEditor.putString(getString(R.string.ta_file_path), mProvFileName);
            prefsEditor.putString(getString(R.string.dm_urn), mDeviceModelURN);
            prefsEditor.putBoolean(getString(R.string.use_provided_bks), false);
            if(mProvFile.getPath().contains("Download"))
                prefsEditor.putBoolean(getString(R.string.from_download), true);
            else
                prefsEditor.putBoolean(getString(R.string.from_download), false);
            prefsEditor.apply();
            FileManager.copyFile(mProvFile, new File(mProvFileName));
            Intent anIntent = new Intent(UserInputActivity.this, MainActivity.class);
            anIntent.putExtra("prov_completed",true);
            anIntent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
            startActivity(anIntent);
        }
    }

    private void cancelAndReturn(){

        if(!needUserInput()){
            // properties have already been set. go to main activity
            Intent anIntent = new Intent(UserInputActivity.this, MainActivity.class);
            anIntent.putExtra("prov_completed",true);
            anIntent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
            startActivity(anIntent);
            return;
        }

    }

    void resetApp() {
        SharedPreferences sharedPref = getApplicationContext().getSharedPreferences(
                getString(R.string.preference_file_key), Context.MODE_PRIVATE);
        if (sharedPref.getBoolean(getString(R.string.from_download), true)) {
            // backup the file in download folder and then reset card
            String sourceName = sharedPref.getString(getString(R.string.ta_file_path), "");
            String destName = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS).getAbsolutePath() + "/" + sourceName.substring(sourceName.lastIndexOf("/") + 1);
            FileManager.copyFile(new File(sourceName), new File(destName));
            resetAppData();
        } else {
            if (Environment.MEDIA_MOUNTED.equals(Environment.getExternalStorageState())) {
                // backup the file on sd card and then reset card
                String sourceName = sharedPref.getString(getString(R.string.ta_file_path), "");
                String destName = Environment.getExternalStorageDirectory().getAbsolutePath() + "/" + sourceName.substring(sourceName.lastIndexOf("/") + 1);
                FileManager.copyFile(new File(sourceName), new File(destName));
                resetAppData();
            } else {
                AlertDialog dialog;
                AlertDialog.Builder builder = new AlertDialog.Builder(UserInputActivity.this);
                builder = builder.setMessage("Application provisioining information will be lost and cannot be recovered. Continue?");
                builder = builder.setPositiveButton("Yes", new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {
                        // User clicked Try again button
                        dialog.dismiss();
                        resetAppData();
                    }
                });
                builder = builder.setNegativeButton("No", new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {
                        // User cancelled the dialog
                        dialog.dismiss();
                    }
                });
                dialog = builder.create();
                dialog.show();
            }
        }
    }
    
    void resetAppData() {
        SharedPreferences sharedPref = getApplicationContext().getSharedPreferences(
                getString(R.string.preference_file_key), Context.MODE_PRIVATE);
        SharedPreferences.Editor prefsEditor = sharedPref.edit();
        prefsEditor.putString(getString(R.string.ta_file_path), getApplicationContext().getFilesDir().getPath());
        prefsEditor.apply();
        prefsEditor.commit();
    }



    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {

        if (requestCode == REQUEST_STORAGE) {
            if (grantResults[0] != PackageManager.PERMISSION_GRANTED) {
				Log.d(ERROR_TAG, "Storage permission was not granted");
            }
        }
    }
}
