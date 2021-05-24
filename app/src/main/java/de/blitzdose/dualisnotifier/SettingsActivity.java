package de.blitzdose.dualisnotifier;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.provider.Settings;

import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.biometric.BiometricManager;
import androidx.preference.ListPreference;
import androidx.preference.Preference;
import androidx.preference.PreferenceFragmentCompat;
import androidx.preference.PreferenceManager;
import androidx.preference.SwitchPreference;
import androidx.work.WorkManager;

import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.snackbar.Snackbar;

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

            Preference donatePreference = findPreference("donate");
            donatePreference.setOnPreferenceClickListener(this);

            Preference githubPreference = findPreference("github");
            githubPreference.setOnPreferenceClickListener(this);

            SwitchPreference useBiometricsPreference = findPreference("useBiometrics");

            BiometricManager biometricManager = BiometricManager.from(getContext());
            if (biometricManager.canAuthenticate(BiometricManager.Authenticators.BIOMETRIC_STRONG | BiometricManager.Authenticators.BIOMETRIC_WEAK | BiometricManager.Authenticators.DEVICE_CREDENTIAL) != BiometricManager.BIOMETRIC_SUCCESS){
                useBiometricsPreference.setEnabled(false);
            }

            Preference customizeNotificationPreference = findPreference("customize_notification");
            customizeNotificationPreference.setOnPreferenceClickListener(this);
        }

        @Override
        public boolean onPreferenceChange(Preference preference, Object newValue) {
            SharedPreferences sharedPref = getContext().getSharedPreferences(getContext().getString(R.string.preference_file_key), Context.MODE_PRIVATE);
            switch (preference.getKey()) {
                case "sync":
                    if (!sharedPref.getBoolean("saveCredentials", true)) {
                        Snackbar.make(getActivity().findViewById(android.R.id.content), getContext().getResources().getString(R.string.sync_makes_no_difference), Snackbar.LENGTH_LONG).show();
                    }
                    if (!(boolean) newValue) {
                        WorkManager.getInstance(getContext()).cancelUniqueWork("DualisNotifier");
                    } else {
                        MainActivity.setAlarmManager(getContext());
                    }
                    return true;
                case "sync_time":
                    if (!sharedPref.getBoolean("saveCredentials", true)) {
                        Snackbar.make(getActivity().findViewById(android.R.id.content), getContext().getResources().getString(R.string.sync_makes_no_difference), Snackbar.LENGTH_LONG).show();
                    }
                    MainActivity.setAlarmManager(getContext());
                    return true;
                case "theme":
                    final Handler handler = new Handler(Looper.getMainLooper());
                    handler.postDelayed(() -> {
                        Intent intent = new Intent(getActivity(), LoginActivity.class);
                        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK|Intent.FLAG_ACTIVITY_CLEAR_TASK);
                        getActivity().startActivity(intent);
                        getActivity().finish();
                        Runtime.getRuntime().exit(0);
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
                        PackageInfo pInfo;
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
                    return true;
                case "donate":
                    Intent donateIntent = new Intent(Intent.ACTION_VIEW, Uri.parse("https://www.paypal.com/donate/?hosted_button_id=QRDS8T657KU7S"));
                    startActivity(donateIntent);
                    return true;
                case "github":
                    Intent githubIntent = new Intent(Intent.ACTION_VIEW, Uri.parse("https://github.com/blitzdose/DualisNotifier"));
                    startActivity(githubIntent);
                    return true;
                case "customize_notification":
                    Intent settingsIntent = null;
                    if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
                        settingsIntent = new Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS)
                                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                                .putExtra(Settings.EXTRA_APP_PACKAGE, getActivity().getPackageName());
                    } else {
                        settingsIntent = new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
                        settingsIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                        Uri uri = Uri.fromParts("package", getActivity().getPackageName(), null);
                        settingsIntent.setData(uri);
                    }

                    startActivity(settingsIntent);
                    return true;
            }
            return false;
        }
    }
}