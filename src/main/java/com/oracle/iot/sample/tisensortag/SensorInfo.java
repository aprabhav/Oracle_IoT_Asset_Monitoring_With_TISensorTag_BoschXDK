package com.oracle.iot.sample.tisensortag;

import java.util.UUID;

public class SensorInfo {

    private String mSensorName;
    private int mMessageID;
    private UUID mServiceChar, mConfigChar, mPeriodChar, mDataChar;
    private byte [] mConfigValues;
    private byte [] mPeriodValues;

    public SensorInfo(String name, int msgID, UUID service, UUID config, byte[] configValues, UUID period, byte[] periodValues, UUID data) {
        mSensorName = name;
        mMessageID = msgID;
        mServiceChar = service;
        mConfigChar = config;
        mConfigValues = configValues;
        mPeriodChar = period;
        mPeriodValues = periodValues;
        mDataChar = data;
    }

    public String getSensorName(){
        return mSensorName;
    }

    public int getMessageID() {
        return mMessageID;
    }

    public UUID getDataChar() {
        return mDataChar;
    }

    public UUID getServiceChar() {
        return mServiceChar;
    }

    public UUID getConfigChar() {
        return mConfigChar;
    }

    public byte[] getConfigValues() {
        return mConfigValues;
    }

    public UUID getPeriodChar() { return mPeriodChar; }

    public byte[] getPeriodValues() { return mPeriodValues; }
}
