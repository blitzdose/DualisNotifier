package de.blitzdose.dualisnotifier;

import java.util.Locale;

public class VorlesungModel {
    private String title;
    private String subtitle;
    private String note;
    private String credits;
    private String endnote;

    public VorlesungModel(String title, String subtitle, String note, String credits, String endnote) {
        this.title = title;
        this.subtitle = subtitle;
        this.note = note;
        this.credits = credits;
        this.endnote = endnote;
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

    public String getCredits() {
        return credits;
    }

    public String getEndnote() {
        return endnote;
    }
}
