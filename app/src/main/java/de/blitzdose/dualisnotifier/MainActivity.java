package de.blitzdose.dualisnotifier;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.app.job.JobInfo;
import android.app.job.JobScheduler;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;

import androidx.appcompat.app.AppCompatActivity;

import org.json.JSONObject;

import java.net.CookieHandler;
import java.net.CookieManager;
import java.net.HttpCookie;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;

public class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        Bundle bundle = getIntent().getExtras();
        String arguments = bundle.getString("arguments");
        String cookies = bundle.getString("cookies");
        List<HttpCookie> cookieArray = HttpCookie.parse(cookies);
        CookieManager cookieManager = new CookieManager();
        try {
            cookieManager.getCookieStore().add(new URI("dualis.dhbw.de"), cookieArray.get(0));
        } catch (URISyntaxException e) {
            e.printStackTrace();
        }

        DualisAPI dualisAPI = new DualisAPI();
        dualisAPI.setOnDataLoadedListener(new DualisAPI.DataLoadedListener() {
            @Override
            public void onDataLoaded(JSONObject data) {
                System.out.println(data.toString());
                setAlarmManager();

                ArrayList<String> items = new ArrayList<>();
                items.add("Option 1");
                items.add("Option 2");
                items.add("Option 3");

                ArrayAdapter arrayAdapter = new ArrayAdapter(MainActivity.this, R.layout.list_item, items);
                AutoCompleteTextView autoCompleteTextView = findViewById(R.id.autoComplete);
                autoCompleteTextView.setAdapter(arrayAdapter);
            }
        });
        System.out.println(arguments);
        System.out.println(((CookieManager) CookieHandler.getDefault()).getCookieStore().getCookies().get(0).toString());
        dualisAPI.makeRequest(this, arguments, cookieManager);

    }

    void setAlarmManager() {
        Intent liveIntent = new Intent(getApplicationContext(), AReceiver.class);
        PendingIntent recurring = PendingIntent.getBroadcast(getApplicationContext(), 0, liveIntent, PendingIntent.FLAG_CANCEL_CURRENT);
        AlarmManager alarmManager = (AlarmManager) getSystemService(Context.ALARM_SERVICE);
        Calendar updateTime = Calendar.getInstance();
        alarmManager.setRepeating(AlarmManager.RTC_WAKEUP, updateTime.getTimeInMillis(), 1 * 60 * 1000, recurring);
    }
}