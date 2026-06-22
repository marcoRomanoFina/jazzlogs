package com.marcoromanofinaa.jazzlogs.editorial.vocabulary;

public enum EditorialVocabularyType {
    CONTEXT("context-vocabulary.json"),
    INSTRUMENT("instrument-vocabulary.json"),
    MOOD("mood-vocabulary.json"),
    RHYTHM("rhythm-vocabulary.json"),
    STYLE("style-vocabulary.json");

    private final String resourceName;

    EditorialVocabularyType(String resourceName) {
        this.resourceName = resourceName;
    }

    public String resourceName() {
        return resourceName;
    }
}
