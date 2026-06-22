package com.marcoromanofinaa.jazzlogs.user.jazzpreferences.model;

public enum DiscoveryMode {
    COMFORT_ZONE("Comfort zone"),
    OPEN_TO_EXPLORE("Open to explore"),
    DEEP_DIGGING("Deep digging");

    private final String label;

    DiscoveryMode(String label) {
        this.label = label;
    }

    public String label() {
        return label;
    }
}
