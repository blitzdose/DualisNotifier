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
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.progressindicator.CircularProgressIndicator;

import org.json.JSONArray;
import org.json.JSONException;
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

    ConstraintLayout mainLayout;
    AutoCompleteTextView semesterDropdown;
    CircularProgressIndicator mainProgressIndicator;
    List<VorlesungModel> vorlesungModels = new ArrayList<>();
    VorlesungAdapter vorlesungAdapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        mainLayout = findViewById(R.id.main_layout);
        semesterDropdown = findViewById(R.id.autoComplete);
        mainProgressIndicator = findViewById(R.id.progress_main);

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
                try {
                    for (int i=0; i<data.getJSONArray("semester").length(); i++) {
                        JSONObject semester = data.getJSONArray("semester").getJSONObject(i);
                        items.add(semester.getString("name"));
                    }
                } catch (JSONException e) {
                    e.printStackTrace();
                }

                ArrayAdapter<String> arrayAdapter = new ArrayAdapter<>(MainActivity.this, R.layout.list_item, items);
                semesterDropdown.setAdapter(arrayAdapter);
                semesterDropdown.setText(items.get(0), false);


                vorlesungModels = new ArrayList<>();
                RecyclerView mRecyclerView = findViewById(R.id.recycler_view);
                try {
                    JSONArray vorlesungen = data.getJSONArray("semester").getJSONObject(0).getJSONArray("Vorlesungen");
                    for (int i = 0; i < vorlesungen.length(); i++) {
                        JSONObject vorlesung = vorlesungen.getJSONObject(i);
                        vorlesungModels.add(new VorlesungModel(vorlesung.getString("name"),
                                vorlesung.getJSONObject("pruefung").getString("thema"),
                                vorlesung.getJSONObject("pruefung").getString("note")));
                    }
                } catch (JSONException e) {
                    e.printStackTrace();
                }

                mRecyclerView.setHasFixedSize(true);
                LinearLayoutManager mLayoutManager = new LinearLayoutManager(MainActivity.this);
                mRecyclerView.setLayoutManager(mLayoutManager);
                vorlesungAdapter = new VorlesungAdapter(vorlesungModels, MainActivity.this);
                mRecyclerView.setAdapter(vorlesungAdapter);



                semesterDropdown.setOnItemClickListener(new AdapterView.OnItemClickListener() {
                    @Override
                    public void onItemClick(AdapterView<?> adapterView, View view, int i, long l) {
                        try {
                            vorlesungModels.clear();
                            JSONArray vorlesungen = data.getJSONArray("semester").getJSONObject(i).getJSONArray("Vorlesungen");
                            for (int k = 0; k < vorlesungen.length(); k++) {
                                JSONObject vorlesung = vorlesungen.getJSONObject(k);
                                vorlesungModels.add(new VorlesungModel(vorlesung.getString("name"),
                                        vorlesung.getJSONObject("pruefung").getString("thema"),
                                        vorlesung.getJSONObject("pruefung").getString("note")));
                            }
                            vorlesungAdapter.notifyDataSetChanged();
                        } catch (JSONException e) {
                            e.printStackTrace();
                        }
                    }
                });


                mainProgressIndicator.setVisibility(View.GONE);
                mainLayout.setVisibility(View.VISIBLE);
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
        alarmManager.setRepeating(AlarmManager.RTC_WAKEUP, updateTime.getTimeInMillis(), 15 * 60 * 1000, recurring);
    }
}