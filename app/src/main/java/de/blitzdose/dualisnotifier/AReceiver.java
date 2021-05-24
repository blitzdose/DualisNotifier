package de.blitzdose.dualisnotifier;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

import androidx.preference.PreferenceManager;
import androidx.work.ExistingPeriodicWorkPolicy;
import androidx.work.OneTimeWorkRequest;
import androidx.work.PeriodicWorkRequest;
import androidx.work.WorkManager;

import java.util.concurrent.TimeUnit;

public class AReceiver extends BroadcastReceiver {

    WorkManager mWorkManager;

    @Override
    public void onReceive(Context context, Intent intent) {
        MainActivity.setAlarmManager(context);
        /*
        PeriodicWorkRequest.Builder myWorkBuilder =
                new PeriodicWorkRequest.Builder(BackgroundWorker.class,
                        PeriodicWorkRequest.MIN_PERIODIC_INTERVAL_MILLIS,
                        TimeUnit.MILLISECONDS);

        PeriodicWorkRequest myWork = myWorkBuilder.build();
        mWorkManager = WorkManager.getInstance(context);
        mWorkManager.enqueueUniquePeriodicWork("Counter", ExistingPeriodicWorkPolicy.KEEP, myWork);
        mWorkManager.enqueue(myWork);
        */
    }
}