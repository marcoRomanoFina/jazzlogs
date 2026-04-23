package com.marcoromanofinaa.jazzlogs.ai.semantic.core;

import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;

public final class SemanticTextBuilder {

    private final StringBuilder text = new StringBuilder();

    public SemanticTextBuilder addSection(String label, String sentence) {
        if (!hasText(sentence)) {
            return this;
        }

        text.append(label).append(": ").append(sentence.trim()).append(System.lineSeparator());
        return this;
    }

    public SemanticTextBuilder addSection(String label, Optional<String> sentence) {
        sentence.ifPresent(value -> addSection(label, value));
        return this;
    }

    public static String naturalJoin(String[] values) {
        return values == null ? "" : naturalJoin(Arrays.asList(values));
    }

    public static String naturalJoin(Integer[] values) {
        return values == null ? "" : naturalJoin(Arrays.asList(values));
    }

    public static String naturalJoin(Collection<?> values) {
        if (values == null || values.isEmpty()) {
            return "";
        }

        var cleanValues = values.stream()
                .filter(Objects::nonNull)
                .map(Object::toString)
                .filter(value -> !value.isBlank())
                .toList();

        if (cleanValues.isEmpty()) {
            return "";
        }

        if (cleanValues.size() == 1) {
            return cleanValues.getFirst();
        }

        var leadingValues = cleanValues.subList(0, cleanValues.size() - 1);
        return leadingValues.stream().collect(Collectors.joining(", "))
                + " y "
                + cleanValues.getLast();
    }

    public static boolean hasText(String value) {
        return value != null && !value.isBlank();
    }

    public static String firstText(String preferred, String fallback) {
        return hasText(preferred) ? preferred : fallback;
    }

    public static Optional<String> optionalText(String value) {
        return hasText(value) ? Optional.of(value) : Optional.empty();
    }

    public static Optional<String> formatIfHasText(String value, String template) {
        return optionalText(value).map(template::formatted);
    }

    public static String phrase(String value, String template) {
        return hasText(value) ? template.formatted(value) : "";
    }

    public static List<String> clean(Collection<String> values) {
        if (values == null || values.isEmpty()) {
            return List.of();
        }

        return values.stream()
                .filter(SemanticTextBuilder::hasText)
                .toList();
    }

    public String build() {
        return text.toString().trim();
    }
}
