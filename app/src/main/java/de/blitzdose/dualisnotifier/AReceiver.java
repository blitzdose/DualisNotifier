package de.blitzdose.dualisnotifier;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

public class AReceiver extends BroadcastReceiver {

    @Override
    public void onReceive(Context context, Intent intent) {
        MainActivity.setAlarmManager(context);
    }
}