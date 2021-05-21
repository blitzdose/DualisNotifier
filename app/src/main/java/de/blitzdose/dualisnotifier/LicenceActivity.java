package de.blitzdose.dualisnotifier;

import androidx.annotation.ColorInt;
import androidx.appcompat.app.AppCompatActivity;

import android.content.res.Resources;
import android.graphics.Typeface;
import android.os.Bundle;
import android.util.TypedValue;
import android.widget.ArrayAdapter;
import android.widget.LinearLayout;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.Collections;

public class LicenceActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_licence);
        LinearLayout mainLayout = findViewById(R.id.main_layout_licence);

        ArrayList<String> titles = new ArrayList<>();
        String[] titlesArr = getResources().getStringArray(R.array.licence_titles);
        Collections.addAll(titles, titlesArr);

        String[] licences = getResources().getStringArray(R.array.licences);
        for (int i=0; i<licences.length; i++) {
            String licenceTitle = titles.get(i);
            TextView titleTextView = new TextView(this);
            titleTextView.setText(licenceTitle);
            LinearLayout.LayoutParams layoutParamsTitle = new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT);
            layoutParamsTitle.setMargins(dpToPx(8), dpToPx(8), dpToPx(8), dpToPx(0));
            titleTextView.setLayoutParams(layoutParamsTitle);

            TypedValue typedValue = new TypedValue();
            Resources.Theme theme = this.getTheme();
            theme.resolveAttribute(R.attr.colorOnSecondary, typedValue, true);
            @ColorInt int color = typedValue.data;

            titleTextView.setTextColor(color);
            titleTextView.setTextSize(24);
            mainLayout.addView(titleTextView);

            String licence = licences[i];
            TextView textView = new TextView(this);
            textView.setText(licence);
            textView.setBackgroundColor(getResources().getColor(android.R.color.darker_gray));
            textView.setTextColor(getResources().getColor(R.color.black));
            textView.setTypeface(Typeface.MONOSPACE);
            LinearLayout.LayoutParams layoutParams = new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
            layoutParams.setMargins(dpToPx(8), dpToPx(8), dpToPx(8), dpToPx(8));

            textView.setLayoutParams(layoutParams);
            textView.setPadding(dpToPx(8), dpToPx(8), dpToPx(8), dpToPx(8));
            mainLayout.addView(textView);
        }
    }

    int dpToPx(int dp) {
        Resources r = this.getResources();
        return (int) TypedValue.applyDimension(
                TypedValue.COMPLEX_UNIT_DIP,
                dp,
                r.getDisplayMetrics()
        );
    }
}