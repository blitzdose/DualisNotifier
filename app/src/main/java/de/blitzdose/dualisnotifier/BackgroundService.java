package de.blitzdose.dualisnotifier;

import android.app.Service;
import android.app.job.JobParameters;
import android.app.job.JobService;
import android.content.Context;
import android.content.Intent;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.IBinder;
import android.os.Looper;
import android.os.Message;
import android.util.Log;
import android.widget.Toast;

import androidx.annotation.Nullable;

import com.github.leonardoxh.keystore.CipherStorage;
import com.github.leonardoxh.keystore.CipherStorageFactory;

import org.json.JSONObject;

import java.io.IOException;
import java.io.OutputStreamWriter;
import java.net.CookieHandler;
import java.net.HttpCookie;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import javax.net.ssl.HttpsURLConnection;

public class BackgroundService extends Service {
    private static final String TAG = "ExampleJobService";
    private String username = "";
    private String password = "";
    private Context context = this;

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        doBackgroundWork();
        return flags;
    }

    @Override
    public void onCreate() {

    }

    private void doBackgroundWork() {
        CipherStorage cipherStorage = CipherStorageFactory.newInstance(context);
        username = cipherStorage.decrypt("username");
        password = cipherStorage.decrypt("password");
        new Thread(new Runnable() {
            @Override
            public void run() {
                ExecutorService executor = Executors.newSingleThreadExecutor();
                Handler handler = new Handler(Looper.getMainLooper());

                executor.execute(new Runnable() {
                    @Override
                    public void run() {
                        java.net.CookieManager cookieManager = new java.net.CookieManager();
                        CookieHandler.setDefault(cookieManager);
                        URL url = null;
                        try {
                            url = new URL("https://dualis.dhbw.de/scripts/mgrqispi.dll");
                            HttpsURLConnection conn = (HttpsURLConnection) url.openConnection();
                            conn.setDoOutput(true);
                            conn.setRequestMethod("POST");
                            conn.setRequestProperty( "Content-type", "application/x-www-form-urlencoded");

                            OutputStreamWriter writer = new OutputStreamWriter(conn.getOutputStream());

                            writer.write("usrname=" + URLEncoder.encode(username, "UTF-8") + "&pass=" + URLEncoder.encode(password, "UTF-8") + "&APPNAME=CampusNet&PRGNAME=LOGINCHECK&ARGUMENTS=clino%2Cusrname%2Cpass%2Cmenuno%2Cmenu_type%2Cbrowser%2Cplatform&clino=000000000000001&menuno=000324&menu_type=classic&browser=&platform=");
                            writer.flush();
                            writer.close();

                            int status = conn.getResponseCode();

                            if (status == HttpURLConnection.HTTP_OK) {
                                List<HttpCookie> cookies = cookieManager.getCookieStore().getCookies();
                                if (cookies.size() == 0) {
                                    handler.post(new Runnable() {
                                        @Override
                                        public void run() {
                                            Toast.makeText(getApplicationContext(), "Anmeldedaten falsch", Toast.LENGTH_SHORT).show();
                                        }
                                    });
                                } else {

                                    String arguments = conn.getHeaderField("REFRESH");
                                    arguments = arguments.split("&")[2];

                                    //intent.putExtra("arguments", arguments);
                                    DualisAPI dualisAPI = new DualisAPI();
                                    dualisAPI.makeRequest(context, arguments, cookieManager);

                                }
                            }
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                    }
                });
                //jobFinished(params, true);
            }
        }).start();
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }
}