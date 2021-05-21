package de.blitzdose.dualisnotifier;

import androidx.appcompat.app.AppCompatActivity;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.job.JobScheduler;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.security.KeyPairGeneratorSpec;
import android.security.keystore.KeyProperties;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Base64;
import android.view.KeyEvent;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import com.github.leonardoxh.keystore.CipherStorage;
import com.github.leonardoxh.keystore.CipherStorageFactory;
import com.google.android.material.internal.TextWatcherAdapter;
import com.google.android.material.progressindicator.CircularProgressIndicator;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;

import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.math.BigInteger;
import java.net.CookieHandler;
import java.net.CookieManager;
import java.net.CookieStore;
import java.net.HttpCookie;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.security.InvalidAlgorithmParameterException;
import java.security.Key;
import java.security.KeyPairGenerator;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;
import java.security.SecureRandom;
import java.security.cert.CertificateException;
import java.util.Calendar;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.Executor;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import javax.crypto.Cipher;
import javax.crypto.KeyGenerator;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import javax.net.ssl.HttpsURLConnection;
import javax.security.auth.x500.X500Principal;

public class LoginActivity extends AppCompatActivity {

    TextInputEditText username;
    TextInputEditText password;
    TextInputLayout passwordLayout;
    Button button;
    Context context = this;
    CircularProgressIndicator progressIndicator;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        username = findViewById(R.id.username);
        password = findViewById(R.id.password);
        button = findViewById(R.id.login);
        progressIndicator = findViewById(R.id.progress_circular);
        passwordLayout = findViewById(R.id.passwordLayout);

        username.addTextChangedListener(new Watcher());
        password.addTextChangedListener(new Watcher());

        createNotificationChannel();

        button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
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
                                            passwordLayout.setError("Anmeldedaten falsch");
                                            progressIndicator.setVisibility(View.GONE);
                                            button.setEnabled(true);
                                        }
                                    });
                                } else {
                                    CipherStorage cipherStorage = CipherStorageFactory.newInstance(context);
                                    cipherStorage.encrypt("password", password.getText().toString());
                                    cipherStorage.encrypt("username", username.getText().toString());

                                    String arguments = conn.getHeaderField("REFRESH");
                                    arguments = arguments.split("&")[2];

                                    Intent intent = new Intent(LoginActivity.this, MainActivity.class);
                                    intent.putExtra("arguments", arguments);
                                    intent.putExtra("cookies", cookies.get(0).toString());
                                    startActivity(intent);
                                    finish();
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


    private void createNotificationChannel() {
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
}