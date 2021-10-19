package de.blitzdose.dualisnotifier;

import android.animation.LayoutTransition;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.LinearLayout;

import androidx.appcompat.app.AppCompatActivity;
import androidx.preference.PreferenceManager;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.work.Constraints;
import androidx.work.ExistingPeriodicWorkPolicy;
import androidx.work.NetworkType;
import androidx.work.PeriodicWorkRequest;
import androidx.work.WorkManager;

import com.android.volley.VolleyError;
import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.progressindicator.CircularProgressIndicator;
import com.google.android.material.snackbar.BaseTransientBottomBar;
import com.google.android.material.snackbar.Snackbar;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.net.CookieHandler;
import java.net.CookieManager;
import java.net.HttpCookie;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

public class MainActivity extends AppCompatActivity implements DualisAPI.DataLoadedListener, DualisAPI.ErrorListener {

    LinearLayout mainLayout;
    AutoCompleteTextView semesterDropdown;
    CircularProgressIndicator mainProgressIndicator;
    List<VorlesungModel> vorlesungModels = new ArrayList<>();
    VorlesungAdapter vorlesungAdapter;
    MaterialToolbar toolbar;
    String arguments = "";
    String currentSemester = "";


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        SharedPreferences sharedPref = this.getSharedPreferences(this.getString(R.string.preference_file_key), Context.MODE_PRIVATE);
        if (!sharedPref.getBoolean("saveCredentials", true)) {
            WorkManager.getInstance(this).cancelUniqueWork("DualisNotifier");
        }

        mainLayout = findViewById(R.id.main_layout);
        mainLayout.setVisibility(View.GONE);
        semesterDropdown = findViewById(R.id.autoComplete);
        mainProgressIndicator = findViewById(R.id.progress_main);
        toolbar = findViewById(R.id.topAppBar);

        DualisAPI dualisAPI = new DualisAPI();

        Bundle bundle = getIntent().getExtras();
        arguments = bundle.getString("arguments");
        String cookies = bundle.getString("cookies");
        List<HttpCookie> cookieArray = HttpCookie.parse(cookies);
        CookieManager cookieManager = new CookieManager();
        try {
            cookieManager.getCookieStore().add(new URI("dualis.dhbw.de"), cookieArray.get(0));
        } catch (URISyntaxException e) {
            e.printStackTrace();
        }
        CookieHandler cookieHandler = CookieManager.getDefault();

        toolbar.setOnMenuItemClickListener(item -> {
            switch (item.getItemId()) {
                case R.id.refresh:
                    mainLayout.setVisibility(View.GONE);
                    mainProgressIndicator.setVisibility(View.VISIBLE);
                    dualisAPI.makeRequest(MainActivity.this, arguments, cookieHandler);
                    return true;
                case R.id.settings:
                    Intent settingsIntent = new Intent(MainActivity.this, SettingsActivity.class);
                    startActivity(settingsIntent);
                    return true;
                default:
                    return false;
            }
        });

        ViewGroup viewGroup = mainLayout;
        viewGroup.getLayoutTransition().enableTransitionType(LayoutTransition.CHANGING);

        dualisAPI.setOnDataLoadedListener(this);
        dualisAPI.setOnErrorListener(this);
        dualisAPI.makeRequest(this, arguments, cookieHandler);

    }

    static void setAlarmManager(Context context) {
        SharedPreferences settingsPref = PreferenceManager.getDefaultSharedPreferences(context);
        SharedPreferences sharedPref = context.getSharedPreferences(context.getString(R.string.preference_file_key), Context.MODE_PRIVATE);
        if (!sharedPref.getBoolean("saveCredentials", false) || !settingsPref.getBoolean("sync", true)) {
            return;
        }

        int time = Integer.parseInt(settingsPref.getString("sync_time", "15"));

        Constraints constraints = new Constraints.Builder()
                .setRequiredNetworkType(NetworkType.CONNECTED)
                .build();

        PeriodicWorkRequest periodicWorkRequest = new PeriodicWorkRequest.Builder(BackgroundWorker.class, time, TimeUnit.MINUTES)
                .setConstraints(constraints)
                .build();

        WorkManager workManager = WorkManager.getInstance(context.getApplicationContext());
        workManager.enqueueUniquePeriodicWork("DualisNotifier", ExistingPeriodicWorkPolicy.REPLACE, periodicWorkRequest);
    }

    void updateList(JSONObject data, int position) throws JSONException {
        vorlesungModels.clear();
        JSONArray vorlesungen = data.getJSONArray("semester").getJSONObject(position).getJSONArray("Vorlesungen");
        for (int k = 0; k < vorlesungen.length(); k++) {
            JSONObject vorlesung = vorlesungen.getJSONObject(k);
            vorlesungModels.add(new VorlesungModel(vorlesung.getString("name"),
                    vorlesung.getJSONArray("pruefungen"),
                    vorlesung.getString("credits"),
                    vorlesung.getString("note")));
        }
    }

    @Override
    public void onDataLoaded(JSONObject data) {
        setAlarmManager(getApplicationContext());

        DualisAPI.copareAndSave(MainActivity.this, data);

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
        if (items.contains(currentSemester)) {
            semesterDropdown.setText(currentSemester, false);
        } else {
            semesterDropdown.setText(items.get(0), false);
        }


        vorlesungModels = new ArrayList<>();
        RecyclerView mRecyclerView = findViewById(R.id.recycler_view);
        try {
            if (items.contains(currentSemester)) {
                updateList(data, items.indexOf(currentSemester));
            } else {
                updateList(data, 0);
            }
        } catch (JSONException e) {
            e.printStackTrace();
        }

        mRecyclerView.setHasFixedSize(true);
        LinearLayoutManager mLayoutManager = new LinearLayoutManager(MainActivity.this);
        mRecyclerView.setLayoutManager(mLayoutManager);
        vorlesungAdapter = new VorlesungAdapter(vorlesungModels, MainActivity.this);
        mRecyclerView.setAdapter(vorlesungAdapter);



        semesterDropdown.setOnItemClickListener((adapterView, view, i, l) -> {
            currentSemester = semesterDropdown.getText().toString();
            try {
                updateList(data, i);
                vorlesungAdapter.notifyDataSetChanged();
            } catch (JSONException e) {
                e.printStackTrace();
            }
        });


        semesterDropdown.setEnabled(true);
        mainProgressIndicator.setVisibility(View.GONE);
        mainLayout.setVisibility(View.VISIBLE);
    }

    @Override
    public void onError(VolleyError error) {
        Snackbar.make(findViewById(android.R.id.content), "Error: " + error.toString(), BaseTransientBottomBar.LENGTH_LONG).show();
    }
}