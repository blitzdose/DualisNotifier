package de.blitzdose.dualisnotifier;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

import java.util.Calendar;

public class AReceiver extends BroadcastReceiver {

    @Override
    public void onReceive(Context context, Intent intent) {
        if (intent.getAction() != null && intent.getAction().equals(Intent.ACTION_BOOT_COMPLETED)) {
            Intent liveIntent = new Intent(context, AReceiver.class);
            PendingIntent recurring = PendingIntent.getBroadcast(context, 0, liveIntent, PendingIntent.FLAG_CANCEL_CURRENT);
            AlarmManager alarmManager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
            Calendar updateTime = Calendar.getInstance();
            alarmManager.setRepeating(AlarmManager.RTC_WAKEUP, updateTime.getTimeInMillis(), 1 * 60 * 1000, recurring);
        }
        try {
            Intent intent2 = new Intent(context, BackgroundService.class);
            context.startService(intent2);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}