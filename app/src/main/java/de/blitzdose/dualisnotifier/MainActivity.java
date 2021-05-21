package de.blitzdose.dualisnotifier;

import android.animation.LayoutTransition;
import android.app.AlarmManager;
import android.app.PendingIntent;
import android.app.job.JobInfo;
import android.app.job.JobScheduler;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.LinearLayout;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.work.Constraints;
import androidx.work.ExistingPeriodicWorkPolicy;
import androidx.work.ListenableWorker;
import androidx.work.NetworkType;
import androidx.work.PeriodicWorkRequest;
import androidx.work.WorkManager;

import com.google.android.material.appbar.MaterialToolbar;
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
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

public class MainActivity extends AppCompatActivity {

    LinearLayout mainLayout;
    AutoCompleteTextView semesterDropdown;
    CircularProgressIndicator mainProgressIndicator;
    List<VorlesungModel> vorlesungModels = new ArrayList<>();
    VorlesungAdapter vorlesungAdapter;
    MaterialToolbar toolbar;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        mainLayout = findViewById(R.id.main_layout);
        mainLayout.setVisibility(View.GONE);
        semesterDropdown = findViewById(R.id.autoComplete);
        mainProgressIndicator = findViewById(R.id.progress_main);
        toolbar = findViewById(R.id.topAppBar);

        DualisAPI dualisAPI = new DualisAPI();

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

        toolbar.setOnMenuItemClickListener(new Toolbar.OnMenuItemClickListener() {
            @Override
            public boolean onMenuItemClick(MenuItem item) {
                switch (item.getItemId()) {
                    case R.id.refresh:
                        mainLayout.setVisibility(View.GONE);
                        mainProgressIndicator.setVisibility(View.VISIBLE);
                        dualisAPI.makeRequest(MainActivity.this, arguments);
                        return true;
                    case R.id.settings:
                        Intent settingsIntent = new Intent(MainActivity.this, SettingsActivity.class);
                        startActivity(settingsIntent);
                        return true;
                    default:
                        return false;
                }
            }
        });

        ViewGroup viewGroup = mainLayout;
        viewGroup.getLayoutTransition().enableTransitionType(LayoutTransition.CHANGING);

        dualisAPI.setOnDataLoadedListener(new DualisAPI.DataLoadedListener() {
            @Override
            public void onDataLoaded(JSONObject data) {
                System.out.println(data.toString());
                setAlarmManager(getApplicationContext());

                dualisAPI.copareAndSave(MainActivity.this, data);

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
                    updateList(data, 0);
                } catch (JSONException e) {
                    e.printStackTrace();
                }

                mRecyclerView.setHasFixedSize(true);
                LinearLayoutManager mLayoutManager = new LinearLayoutManager(MainActivity.this);
                mRecyclerView.setLayoutManager(mLayoutManager);
                vorlesungAdapter = new VorlesungAdapter(vorlesungModels, MainActivity.this, mRecyclerView);
                mRecyclerView.setAdapter(vorlesungAdapter);



                semesterDropdown.setOnItemClickListener(new AdapterView.OnItemClickListener() {
                    @Override
                    public void onItemClick(AdapterView<?> adapterView, View view, int i, long l) {
                        try {
                            updateList(data, i);
                            vorlesungAdapter.notifyDataSetChanged();
                        } catch (JSONException e) {
                            e.printStackTrace();
                        }
                    }
                });


                semesterDropdown.setEnabled(true);
                mainProgressIndicator.setVisibility(View.GONE);
                mainLayout.setVisibility(View.VISIBLE);
            }
        });
        System.out.println(arguments);
        System.out.println(((CookieManager) CookieHandler.getDefault()).getCookieStore().getCookies().get(0).toString());
        dualisAPI.makeRequest(this, arguments);

    }

    static void setAlarmManager(Context context) {
        Constraints constraints = new Constraints.Builder()
                .setRequiredNetworkType(NetworkType.CONNECTED)
                .build();

        PeriodicWorkRequest periodicWorkRequest = new PeriodicWorkRequest.Builder(BackgroundWorker.class, 15, TimeUnit.MINUTES)
                .setConstraints(constraints)
                .build();

        WorkManager workManager = WorkManager.getInstance(context.getApplicationContext());
        workManager.enqueueUniquePeriodicWork("Counter", ExistingPeriodicWorkPolicy.KEEP,periodicWorkRequest);
    }

    void updateList(JSONObject data, int position) throws JSONException {
        vorlesungModels.clear();
        JSONArray vorlesungen = data.getJSONArray("semester").getJSONObject(position).getJSONArray("Vorlesungen");
        for (int k = 0; k < vorlesungen.length(); k++) {
            JSONObject vorlesung = vorlesungen.getJSONObject(k);
            vorlesungModels.add(new VorlesungModel(vorlesung.getString("name"),
                    vorlesung.getJSONObject("pruefung").getString("thema"),
                    vorlesung.getJSONObject("pruefung").getString("note"),
                    vorlesung.getString("credits"),
                    vorlesung.getString("note")));
        }
    }
}