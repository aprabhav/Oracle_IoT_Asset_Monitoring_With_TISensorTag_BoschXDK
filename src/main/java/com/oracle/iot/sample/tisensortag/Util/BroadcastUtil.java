package com.oracle.iot.sample.tisensortag.Util;

import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGattCharacteristic;
import android.content.Context;
import android.content.Intent;
import android.support.v4.content.LocalBroadcastManager;

public class BroadcastUtil {
    public static void broadcastUpdate(Context context, final String action,
                                       final BluetoothDevice device) {
        final Intent intent = new Intent(action);
        intent.putExtra("name", device.getName());
        intent.putExtra("device", device);
        LocalBroadcastManager.getInstance(context).sendBroadcast(intent);
    }

    public static void broadcastUpdate(Context context, final String action, final int messageType, final String message) {
        final Intent intent = new Intent(action);
        intent.putExtra("type", messageType);
        intent.putExtra("message", message);
        LocalBroadcastManager.getInstance(context).sendBroadcast(intent);
    }

    public static void broadcastUpdate(Context context, final String action, double[] values) {
        final Intent intent = new Intent(action);
        intent.putExtra(Constants.EXTRA_SENSORVALUES, values);
        LocalBroadcastManager.getInstance(context).sendBroadcast(intent);
    }
}
