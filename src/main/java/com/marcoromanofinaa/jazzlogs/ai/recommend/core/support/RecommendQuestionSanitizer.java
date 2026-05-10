package com.marcoromanofinaa.jazzlogs.ai.recommend.core.support;

import java.util.regex.Pattern;

public final class RecommendQuestionSanitizer {

    private static final Pattern WHITESPACE = Pattern.compile("\\s+");
    private static final Pattern LEADING_QUOTES = Pattern.compile("^[\"'`“”‘’«»]+");
    private static final Pattern TRAILING_QUOTES = Pattern.compile("[\"'`“”‘’«»]+$");
    private static final Pattern REPEATED_PUNCTUATION = Pattern.compile("([?!.,:;])\\1+");

    private RecommendQuestionSanitizer() {
    }

    public static String sanitize(String question) {
        if (question == null) {
            return "";
        }

        var sanitized = question
                .replace('“', '"')
                .replace('”', '"')
                .replace('‘', '\'')
                .replace('’', '\'')
                .replace('«', '"')
                .replace('»', '"');

        sanitized = LEADING_QUOTES.matcher(sanitized).replaceFirst("");
        sanitized = TRAILING_QUOTES.matcher(sanitized).replaceFirst("");
        sanitized = REPEATED_PUNCTUATION.matcher(sanitized).replaceAll("$1");
        sanitized = WHITESPACE.matcher(sanitized).replaceAll(" ").trim();

        return sanitized;
    }
}
