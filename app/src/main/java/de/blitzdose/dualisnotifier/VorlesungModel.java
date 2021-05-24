package de.blitzdose.dualisnotifier;

public class VorlesungModel {
    private final String title;
    private final String subtitle;
    private final String note;
    private final String credits;
    private final String endnote;

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
