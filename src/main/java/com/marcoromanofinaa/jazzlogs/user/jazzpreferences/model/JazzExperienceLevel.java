package com.marcoromanofinaa.jazzlogs.user.jazzpreferences.model;

public enum JazzExperienceLevel {
    NEW_LISTENER("New listener"),
    CASUAL_LISTENER("Casual listener"),
    INTERMEDIATE("Intermediate"),
    ADVANCED("Advanced");

    private final String label;

    JazzExperienceLevel(String label) {
        this.label = label;
    }

    public String label() {
        return label;
    }
}
