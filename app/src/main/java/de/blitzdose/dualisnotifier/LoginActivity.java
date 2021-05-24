package de.blitzdose.dualisnotifier;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.widget.Button;
import android.widget.CheckBox;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.biometric.BiometricManager;
import androidx.biometric.BiometricPrompt;
import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;
import androidx.core.content.ContextCompat;
import androidx.preference.PreferenceManager;

import com.github.leonardoxh.keystore.CipherStorage;
import com.github.leonardoxh.keystore.CipherStorageFactory;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.progressindicator.CircularProgressIndicator;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;

import java.io.IOException;
import java.io.OutputStreamWriter;
import java.net.CookieHandler;
import java.net.CookieManager;
import java.net.HttpCookie;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.util.List;
import java.util.concurrent.Executor;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import javax.net.ssl.HttpsURLConnection;

public class LoginActivity extends AppCompatActivity {

    TextInputEditText username;
    TextInputEditText password;
    TextInputLayout passwordLayout;
    TextInputLayout usernameLayout;
    CheckBox saveLogin;
    Button button;
    Context context = this;
    CircularProgressIndicator progressIndicator;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);
        PreferenceManager.setDefaultValues(this, R.xml.root_preferences, false);

        NotificationManagerCompat notificationManager = NotificationManagerCompat.from(context);
        notificationManager.cancel(55);

        SharedPreferences settingsPref = PreferenceManager.getDefaultSharedPreferences(context);
        SharedPreferences sharedPref = this.getSharedPreferences(getString(R.string.preference_file_key), Context.MODE_PRIVATE);
        int theme = Integer.parseInt(settingsPref.getString("theme", "-1"));
        AppCompatDelegate.setDefaultNightMode(theme);

        username = findViewById(R.id.username);
        password = findViewById(R.id.password);
        saveLogin = findViewById(R.id.checkbox_save_credentials);
        button = findViewById(R.id.login);
        progressIndicator = findViewById(R.id.progress_circular);
        passwordLayout = findViewById(R.id.passwordLayout);
        usernameLayout = findViewById(R.id.usernameLayout);

        if (settingsPref.getBoolean("useBiometrics", false)) {
            BiometricManager biometricManager = BiometricManager.from(context);
            if (biometricManager.canAuthenticate(BiometricManager.Authenticators.BIOMETRIC_STRONG | BiometricManager.Authenticators.BIOMETRIC_WEAK | BiometricManager.Authenticators.DEVICE_CREDENTIAL) == BiometricManager.BIOMETRIC_SUCCESS){
                BiometricPrompt biometricPrompt = getInstanceOfBiometricPromt(context, username, password, button);
                biometricPrompt.authenticate(getPromptInfo());

                usernameLayout.setEndIconDrawable(R.drawable.ic_baseline_fingerprint_24);
                usernameLayout.setEndIconOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        BiometricPrompt biometricPrompt = getInstanceOfBiometricPromt(context, username, password, button);
                        biometricPrompt.authenticate(getPromptInfo());
                    }
                });
            }
        } else if(sharedPref.getBoolean("saveCredentials", true)) {
            CipherStorage cipherStorage = CipherStorageFactory.newInstance(context);
            if (cipherStorage.containsAlias("password") && cipherStorage.containsAlias("username")){
                username.setText(cipherStorage.decrypt("username"));
                password.setText(cipherStorage.decrypt("password"));
            }
        }

        username.addTextChangedListener(new Watcher());
        password.addTextChangedListener(new Watcher());

        saveLogin.setChecked(sharedPref.getBoolean("saveCredentials", true));

        createNotificationChannelNewGrade();
        createNotificationChannelGeneral();

        button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                boolean saveCredentials = saveLogin.isChecked();
                sharedPref.edit().putBoolean("saveCredentials", saveCredentials).apply();
                progressIndicator.setVisibility(View.VISIBLE);
                button.setEnabled(false);
                ExecutorService executor = Executors.newSingleThreadExecutor();
                Handler handler = new Handler(Looper.getMainLooper());

                executor.execute(new Runnable() {
                    @Override
                    public void run() {
                        CookieManager cookieManager = new java.net.CookieManager();
                        CookieHandler.setDefault(cookieManager);
                        URL url = null;
                        try {
                            url = new URL("https://dualis.dhbw.de/scripts/mgrqispi.dll");
                            HttpsURLConnection conn = (HttpsURLConnection) url.openConnection();
                            conn.setDoOutput(true);
                            conn.setRequestMethod("POST");
                            conn.setRequestProperty( "Content-type", "application/x-www-form-urlencoded");

                            OutputStreamWriter writer = new OutputStreamWriter(conn.getOutputStream());

                            writer.write("usrname=" + URLEncoder.encode(username.getText().toString(), "UTF-8") + "&pass=" + URLEncoder.encode(password.getText().toString(), "UTF-8") + "&APPNAME=CampusNet&PRGNAME=LOGINCHECK&ARGUMENTS=clino%2Cusrname%2Cpass%2Cmenuno%2Cmenu_type%2Cbrowser%2Cplatform&clino=000000000000001&menuno=000324&menu_type=classic&browser=&platform=");
                            writer.flush();
                            writer.close();

                            int status = conn.getResponseCode();
                            if (status == HttpURLConnection.HTTP_OK) {
                                List<HttpCookie> cookies = cookieManager.getCookieStore().getCookies();
                                if (cookies.size() == 0) {
                                    handler.post(new Runnable() {
                                        @Override
                                        public void run() {
                                            passwordLayout.setError(getString(R.string.wrong_credentials));
                                            progressIndicator.setVisibility(View.GONE);
                                            button.setEnabled(true);
                                        }
                                    });
                                } else {
                                    CipherStorage cipherStorage = CipherStorageFactory.newInstance(context);
                                    if (saveCredentials) {
                                        cipherStorage.encrypt("password", password.getText().toString());
                                        cipherStorage.encrypt("username", username.getText().toString());
                                    } else {
                                        if (cipherStorage.containsAlias("username")) {
                                            cipherStorage.removeKey("username");
                                        }
                                        if (cipherStorage.containsAlias("password")) {
                                            cipherStorage.removeKey("password");
                                        }
                                    }

                                    String arguments = conn.getHeaderField("REFRESH").split("&")[2];

                                    BiometricManager biometricManager = BiometricManager.from(context);
                                    if (saveCredentials && sharedPref.getBoolean("askBiometrics", true) &&  (biometricManager.canAuthenticate(BiometricManager.Authenticators.BIOMETRIC_STRONG | BiometricManager.Authenticators.BIOMETRIC_WEAK | BiometricManager.Authenticators.DEVICE_CREDENTIAL) == BiometricManager.BIOMETRIC_SUCCESS)) {
                                        sharedPref.edit().putBoolean("askBiometrics", false).apply();
                                        Handler mainHandler = new Handler(context.getMainLooper());
                                        Runnable runnable = new Runnable() {
                                            @Override
                                            public void run() {
                                                new MaterialAlertDialogBuilder(context)
                                                        .setCancelable(false)
                                                        .setTitle(R.string.biometrics)
                                                        .setIcon(R.drawable.ic_baseline_fingerprint_24)
                                                        .setMessage(R.string.biometrics_ask)
                                                        .setNegativeButton("Nein", new DialogInterface.OnClickListener() {
                                                            @Override
                                                            public void onClick(DialogInterface dialog, int which) {
                                                                settingsPref.edit().putBoolean("useBiometrics", false).apply();
                                                                Intent intent = new Intent(LoginActivity.this, MainActivity.class);
                                                                intent.putExtra("arguments", arguments);
                                                                intent.putExtra("cookies", cookies.get(0).toString());
                                                                startActivity(intent);
                                                                finish();
                                                            }
                                                        })
                                                        .setPositiveButton("Ja", new DialogInterface.OnClickListener() {
                                                            @Override
                                                            public void onClick(DialogInterface dialog, int which) {
                                                                settingsPref.edit().putBoolean("useBiometrics", true).apply();
                                                                Intent intent = new Intent(LoginActivity.this, MainActivity.class);
                                                                intent.putExtra("arguments", arguments);
                                                                intent.putExtra("cookies", cookies.get(0).toString());
                                                                startActivity(intent);
                                                                finish();
                                                            }
                                                        })
                                                        .show();
                                            }
                                        };
                                        mainHandler.post(runnable);
                                    } else {
                                        Intent intent = new Intent(LoginActivity.this, MainActivity.class);
                                        intent.putExtra("arguments", arguments);
                                        intent.putExtra("cookies", cookies.get(0).toString());
                                        startActivity(intent);
                                        finish();
                                    }

                                }
                            }
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                    }
                });
            }
        });
    }


    private void createNotificationChannelNewGrade() {
        // Create the NotificationChannel, but only on API 26+ because
        // the NotificationChannel class is new and not in the support library
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            CharSequence name = getString(R.string.channel_name);
            String description = getString(R.string.channel_description);
            int importance = NotificationManager.IMPORTANCE_DEFAULT;
            NotificationChannel channel = new NotificationChannel("1234", name, importance);
            channel.setDescription(description);
            // Register the channel with the system; you can't change the importance
            // or other notification behaviors after this
            NotificationManager notificationManager = getSystemService(NotificationManager.class);
            notificationManager.createNotificationChannel(channel);
        }
    }

    private void createNotificationChannelGeneral() {
        // Create the NotificationChannel, but only on API 26+ because
        // the NotificationChannel class is new and not in the support library
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            CharSequence name = getString(R.string.channel_name_general);
            String description = getString(R.string.channel_description_general);
            int importance = NotificationManager.IMPORTANCE_DEFAULT;
            NotificationChannel channel = new NotificationChannel("4321", name, importance);
            channel.setDescription(description);
            // Register the channel with the system; you can't change the importance
            // or other notification behaviors after this
            NotificationManager notificationManager = getSystemService(NotificationManager.class);
            notificationManager.createNotificationChannel(channel);
        }
    }

    class Watcher implements TextWatcher {

        @Override
        public void beforeTextChanged(CharSequence charSequence, int i, int i1, int i2) {

        }

        @Override
        public void onTextChanged(CharSequence charSequence, int i, int i1, int i2) {
            if (password.getText().length() > 0 && username.getText().length() > 0) {
                button.setEnabled(true);
            } else {
                button.setEnabled(false);
            }
        }

        @Override
        public void afterTextChanged(Editable editable) {

        }
    }

    BiometricPrompt getInstanceOfBiometricPromt(Context context, TextInputEditText username, TextInputEditText password, Button button) {
        Executor executor = ContextCompat.getMainExecutor(context);
        BiometricPrompt.AuthenticationCallback callback = new BiometricPrompt.AuthenticationCallback() {
            @Override
            public void onAuthenticationError(int errorCode, @NonNull CharSequence errString) {
                super.onAuthenticationError(errorCode, errString);
                System.out.println(errorCode + "  "  + errString);
            }

            @Override
            public void onAuthenticationSucceeded(@NonNull BiometricPrompt.AuthenticationResult result) {
                super.onAuthenticationSucceeded(result);
                System.out.println("AUTHENTICATION SUCCESSFUL");
                CipherStorage cipherStorage = CipherStorageFactory.newInstance(context);
                username.setText(cipherStorage.decrypt("username"));
                password.setText(cipherStorage.decrypt("password"));
                button.callOnClick();
            }

            @Override
            public void onAuthenticationFailed() {
                super.onAuthenticationFailed();
                System.out.println("UNKNOWN REASON");
            }
        };
        return new BiometricPrompt(this, executor, callback);
    }

    BiometricPrompt.PromptInfo getPromptInfo() {
        BiometricPrompt.PromptInfo promptInfo = new BiometricPrompt.PromptInfo.Builder()
                .setTitle(getString(R.string.authentication))
                .setSubtitle(getString(R.string.authentication_ask))
                .setNegativeButtonText(getString(R.string.cancel))
                .build();
        return promptInfo;
    }
}