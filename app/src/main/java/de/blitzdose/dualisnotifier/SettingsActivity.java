package de.blitzdose.dualisnotifier;

import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.View;

import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.preference.ListPreference;
import androidx.preference.Preference;
import androidx.preference.PreferenceFragmentCompat;
import androidx.preference.PreferenceManager;
import androidx.preference.SwitchPreference;
import androidx.work.WorkManager;

import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;

import java.util.List;

import static android.content.Intent.FLAG_ACTIVITY_NEW_TASK;

public class SettingsActivity extends AppCompatActivity {

    MaterialToolbar toolbar;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.settings_activity);

        SharedPreferences settingsPref = PreferenceManager.getDefaultSharedPreferences(this);
        int theme = Integer.parseInt(settingsPref.getString("theme", "-1"));
        AppCompatDelegate.setDefaultNightMode(theme);

        if (savedInstanceState == null) {
            getSupportFragmentManager()
                    .beginTransaction()
                    .replace(R.id.settings, new SettingsFragment())
                    .commit();
        }
        ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) {
            actionBar.setDisplayHomeAsUpEnabled(true);
        }

        toolbar = findViewById(R.id.topAppBar_settings);
        toolbar.setNavigationOnClickListener(v -> finish());
    }

    public static class SettingsFragment extends PreferenceFragmentCompat implements Preference.OnPreferenceChangeListener, Preference.OnPreferenceClickListener {
        @Override
        public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {
            setPreferencesFromResource(R.xml.root_preferences, rootKey);

            SwitchPreference notificationPreference = findPreference("sync");
            notificationPreference.setOnPreferenceChangeListener(this);

            ListPreference notificationTimePreference = findPreference("sync_time");
            notificationTimePreference.setOnPreferenceChangeListener(this);

            ListPreference themePreference = findPreference("theme");
            themePreference.setOnPreferenceChangeListener(this);

            Preference aboutPreference = findPreference("about");
            aboutPreference.setOnPreferenceClickListener(this);

            Preference licencePreference = findPreference("licence");
            licencePreference.setOnPreferenceClickListener(this);
        }

        @Override
        public boolean onPreferenceChange(Preference preference, Object newValue) {
            System.out.println(preference.getKey());
            switch (preference.getKey()) {
                case "sync":
                    if (!(boolean) newValue) {
                        WorkManager.getInstance(getContext()).cancelUniqueWork("DualisNotifier");
                    } else {
                        MainActivity.setAlarmManager(getContext(), true);
                    }
                    return true;
                case "sync_time":
                    MainActivity.setAlarmManager(getContext(), true);
                    return true;
                case "theme":
                    final Handler handler = new Handler(Looper.getMainLooper());
                    handler.postDelayed(new Runnable() {
                        @Override
                        public void run() {
                            Intent intent = new Intent(getActivity(), LoginActivity.class);
                            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK|Intent.FLAG_ACTIVITY_CLEAR_TASK);
                            getActivity().startActivity(intent);
                            getActivity().finish();
                            Runtime.getRuntime().exit(0);
                        }
                    }, 100);
                    return true;
            }
            return false;
        }

        @Override
        public boolean onPreferenceClick(Preference preference) {
            switch (preference.getKey()) {
                case "about":
                    String version = "Error";
                    try {
                        PackageInfo pInfo = null;
                        pInfo = getContext().getPackageManager().getPackageInfo(getContext().getPackageName(), 0);
                        version = pInfo.versionName;
                    } catch (PackageManager.NameNotFoundException e){
                        e.printStackTrace();
                    }

                    new MaterialAlertDialogBuilder(getContext())
                            .setTitle(R.string.app_name)
                            .setIcon(R.mipmap.ic_launcher_round)
                            .setMessage(getContext().getString(R.string.about_message, version))
                            .setPositiveButton("Ok", null)
                            .show();
                    return true;
                case "licence":
                    Intent intent = new Intent(getActivity(), LicenceActivity.class);
                    startActivity(intent);
            }
            return false;
        }
    }
}