package de.blitzdose.dualisnotifier;

import java.util.Locale;

public class VorlesungModel {
    private String title;
    private String subtitle;
    private String note;

    public VorlesungModel(String title, String subtitle, String note) {
        this.title = title;
        this.subtitle = subtitle;
        this.note = note;
    }
    public String getTitle() {
        return title;
    }
    public String getSubtitle() {
        return subtitle;
    }

    public String getNote() {
        return note;
    }
}
