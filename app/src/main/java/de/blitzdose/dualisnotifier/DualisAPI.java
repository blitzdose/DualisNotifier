package de.blitzdose.dualisnotifier;

import android.content.Context;

import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.StringRequest;
import com.android.volley.toolbox.Volley;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.net.CookieHandler;
import java.net.CookieManager;
import java.nio.charset.StandardCharsets;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class DualisAPI {

    String mainArguments = "";
    JSONObject mainJson = new JSONObject();

    private DataLoadedListener listener;

    DualisAPI() {
        this.listener = null;
    }

    void makeRequest(Context context, String arguments) {

        System.out.println("New Request");

        mainArguments = arguments.replace("-N000000000000000", "");
        mainArguments = mainArguments.replace("-N000019,", "-N000307");

        RequestQueue queue = Volley.newRequestQueue(context);
        String url = "https://dualis.dhbw.de/scripts/mgrqispi.dll?APPNAME=CampusNet&PRGNAME=COURSERESULTS&" + mainArguments;
        StringRequest stringRequest = new StringRequest(Request.Method.GET, url,
                new Response.Listener<String>() {
                    @Override
                    public void onResponse(String response) {
                        try {
                            response = new String(response.getBytes(StandardCharsets.ISO_8859_1), StandardCharsets.UTF_8);
                            Document doc = Jsoup.parse(response);
                            Elements selectDiv = doc.select("select#semester");
                            Elements options = selectDiv.select("option");
                            try {
                                JSONArray semesterOptionen = new JSONArray();
                                for (Element option : options) {
                                    JSONObject jsonObject = new JSONObject();

                                    jsonObject.put("name", option.text());
                                    jsonObject.put("value", option.val());

                                    semesterOptionen.put(jsonObject);
                                }
                                mainJson.put("semester", semesterOptionen);
                                System.out.println(options.size());
                                for (int i=0; i<options.size(); i++) {
                                    requestSemester(i, context);
                                }
                            } catch (JSONException e) {
                                e.printStackTrace();
                            }
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                    }
                }, new Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError error) {
                System.out.println(error);
            }
        });
        queue.add(stringRequest);

    }

    private void requestSemester(int semesterIndex, Context context) throws JSONException {
        RequestQueue queue = Volley.newRequestQueue(context);
        String url = "https://dualis.dhbw.de/scripts/mgrqispi.dll?APPNAME=CampusNet&PRGNAME=COURSERESULTS&" + mainArguments + ",-N" + mainJson.getJSONArray("semester").getJSONObject(semesterIndex).get("value");
        StringRequest stringRequest = new StringRequest(Request.Method.GET, url,
                new Response.Listener<String>() {
                    @Override
                    public void onResponse(String response) {
                        try {
                            response = new String(response.getBytes(StandardCharsets.ISO_8859_1), StandardCharsets.UTF_8);
                            Document doc = Jsoup.parse(response);
                            Elements table = doc.select("table.nb.list");
                            Elements rows = table.get(0).select("tbody tr");
                            JSONArray vorlesungen = new JSONArray();
                            for (int i=0; i<rows.size(); i++) {
                                Element row = rows.get(i);
                                Elements tabledatas = row.select("td");
                                if (tabledatas.size() > 1) {
                                    try {
                                        JSONObject vorlesung = new JSONObject();
                                        vorlesung.put("nummer", tabledatas.get(0).text());
                                        vorlesung.put("name", tabledatas.get(1).text());
                                        vorlesung.put("note", tabledatas.get(2).text());
                                        vorlesung.put("credits", tabledatas.get(3).text());

                                        Element script = tabledatas.get(5).selectFirst("script");
                                        String scriptText = script.html();

                                        Pattern pattern = Pattern.compile("dl_popUp\\(\"(.+?)\",\"Resultdetails\"", Pattern.DOTALL);
                                        Matcher matcher = pattern.matcher(scriptText);
                                        matcher.find();
                                        String link = matcher.group(1);

                                        vorlesung.put("link", link);
                                        vorlesungen.put(vorlesung);
                                    } catch (JSONException e) {
                                        e.printStackTrace();
                                    }
                                }
                            }
                            try {
                                mainJson.getJSONArray("semester").getJSONObject(semesterIndex).put("Vorlesungen", vorlesungen);
                                boolean vorlesungFehlt = false;
                                for (int i=0; i<mainJson.getJSONArray("semester").length(); i++) {
                                    JSONObject semester = mainJson.getJSONArray("semester").getJSONObject(i);
                                    if (!semester.has("Vorlesungen")) {
                                        vorlesungFehlt = true;
                                    }
                                }
                                if (!vorlesungFehlt) {
                                    requestPruefungen(context);
                                }
                            } catch (JSONException e) {
                                e.printStackTrace();
                            }
                        } catch (Exception e) {
                            e.printStackTrace();
                        }

                    }
                }, new Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError error) {
                System.out.println(error);
            }
        });
        queue.add(stringRequest);
    }

    private void requestPruefungen(Context context) throws JSONException {
        final int[] count = {0};
        final int[] anzahl = {0};
        JSONArray semesterArray = mainJson.getJSONArray("semester");
        for (int i=0; i<semesterArray.length(); i++) {
            JSONObject semester = semesterArray.getJSONObject(i);
            JSONArray vorlesungen = semester.getJSONArray("Vorlesungen");
            for (int j=0; j<vorlesungen.length(); j++) {
                anzahl[0]++;
                String link = vorlesungen.getJSONObject(j).getString("link");

                RequestQueue queue = Volley.newRequestQueue(context);
                String url = "https://dualis.dhbw.de" + link;
                int finalJ = j;
                StringRequest stringRequest = new StringRequest(Request.Method.GET, url,
                        new Response.Listener<String>() {
                            @Override
                            public void onResponse(String response) {
                                try {
                                    response = new String(response.getBytes(StandardCharsets.ISO_8859_1), StandardCharsets.UTF_8);
                                    Document doc = Jsoup.parse(response);
                                    Element table = doc.select("table").get(0);
                                    String thema = table.select("tr").get(4).select("td").get(1).text();
                                    String note = table.select("tr").get(4).select("td").get(3).text();
                                    if (note.isEmpty()) {
                                        thema = table.select("tr").get(5).select("td").get(1).text();
                                        note = table.select("tr").get(5).select("td").get(3).text();
                                    }
                                    JSONObject pruefung = new JSONObject();
                                    try {
                                        pruefung.put("thema", thema);
                                        pruefung.put("note", note);
                                        //pruefung.put("note", "" + System.currentTimeMillis());
                                        vorlesungen.getJSONObject(finalJ).put("pruefung", pruefung);
                                    } catch (JSONException e) {
                                        e.printStackTrace();
                                    }
                                    count[0]++;
                                    if (count[0] == anzahl[0]) {
                                        if (listener != null) {
                                            listener.onDataLoaded(mainJson);
                                        }
                                    }
                                } catch (Exception e) {
                                    e.printStackTrace();
                                }
                            }
                        }, new Response.ErrorListener() {
                    @Override
                    public void onErrorResponse(VolleyError error) {
                        System.out.println(error);
                    }
                });
                queue.add(stringRequest);

            }
        }
    }

    static void copareAndSave(Context context, JSONObject mainJson) {
        File file = new File(context.getFilesDir() + "/test.json");
        String fileContent = "";
        if (file.exists()) {
            try {
                BufferedReader fin = new BufferedReader(new FileReader(context.getFilesDir() + "/test.json"));
                StringBuilder stringBuilder = new StringBuilder();
                while (fin.ready()) {
                    stringBuilder.append(fin.readLine());
                }
                fileContent = stringBuilder.toString();
            } catch (IOException e) {
                e.printStackTrace();
            }

            JSONObject savedJson = new JSONObject();
            try {
                savedJson = new JSONObject(fileContent);
                for (int i=0; i<savedJson.getJSONArray("semester").length(); i++) {
                    for (int j=0; j<savedJson.getJSONArray("semester").getJSONObject(i).getJSONArray("Vorlesungen").length(); j++) {
                        savedJson.getJSONArray("semester").getJSONObject(i).getJSONArray("Vorlesungen").getJSONObject(j).remove("link");
                    }
                }

                for (int i=0; i<mainJson.getJSONArray("semester").length(); i++) {
                    for (int j=0; j<mainJson.getJSONArray("semester").getJSONObject(i).getJSONArray("Vorlesungen").length(); j++) {
                        mainJson.getJSONArray("semester").getJSONObject(i).getJSONArray("Vorlesungen").getJSONObject(j).remove("link");
                    }
                }

            } catch (JSONException e) {
                e.printStackTrace();
            }

            if (fileContent.equals(mainJson.toString())) {
                System.out.println("EQUALS");
            } else {
                try {
                    for (int i=0; i<mainJson.getJSONArray("semester").length(); i++) {
                        for (int j=0; j<mainJson.getJSONArray("semester").getJSONObject(i).getJSONArray("Vorlesungen").length(); j++) {
                            JSONObject vorlesung = mainJson.getJSONArray("semester").getJSONObject(i).getJSONArray("Vorlesungen").getJSONObject(j);
                            String endnoteCurrent = vorlesung.getString("note");
                            String endnoteSaved = savedJson.getJSONArray("semester").getJSONObject(i).getJSONArray("Vorlesungen").getJSONObject(j).getString("note");

                            String noteCurrent = vorlesung.getJSONObject("pruefung").getString("note");
                            String noteSaved = savedJson.getJSONArray("semester").getJSONObject(i).getJSONArray("Vorlesungen").getJSONObject(j).getJSONObject("pruefung").getString("note");
                            if (!noteCurrent.equals(noteSaved)) {
                                NotificationCompat.Builder builder = new NotificationCompat.Builder(context, "1234")
                                        .setSmallIcon(R.drawable.ic_baseline_school_48)
                                        .setContentTitle("Neue Prüfungsnote")
                                        .setContentText("Es wurde eine neue Prüfungsnote eingetragen\n" + vorlesung.getString("name")  + ": " + noteCurrent)
                                        .setPriority(NotificationCompat.PRIORITY_DEFAULT);

                                NotificationManagerCompat notificationManager = NotificationManagerCompat.from(context);
                                notificationManager.notify(j, builder.build());
                            }
                            if (!endnoteCurrent.equals(endnoteSaved)) {
                                NotificationCompat.Builder builder = new NotificationCompat.Builder(context, "1234")
                                        .setSmallIcon(R.drawable.ic_baseline_school_48)
                                        .setContentTitle("Neue Endnote")
                                        .setContentText("Es wurde eine neue Endnote eingetragen\n" + vorlesung.getString("name")  + ": " + endnoteCurrent)
                                        .setPriority(NotificationCompat.PRIORITY_DEFAULT);

                                NotificationManagerCompat notificationManager = NotificationManagerCompat.from(context);
                                notificationManager.notify(j, builder.build());
                            }
                        }
                    }

                } catch (JSONException e) {
                    e.printStackTrace();
                }
            }
        }
        try {
            FileOutputStream fOut = context.openFileOutput("test.json", context.MODE_PRIVATE);
            fOut.write(mainJson.toString().getBytes());
            fOut.close();
        }
        catch (Exception e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
    }

    public void setOnDataLoadedListener(DataLoadedListener listener) {
        this.listener = listener;
    }

    public interface DataLoadedListener {
        public void onDataLoaded(JSONObject data);
    }
}
