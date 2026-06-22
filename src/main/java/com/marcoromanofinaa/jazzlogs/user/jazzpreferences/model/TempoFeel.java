package com.marcoromanofinaa.jazzlogs.user.jazzpreferences.model;

public enum TempoFeel {
    LOW("Low"),
    MID("Mid"),
    HIGH("High");

    private final String label;

    TempoFeel(String label) {
        this.label = label;
    }

    public String label() {
        return label;
    }
}
