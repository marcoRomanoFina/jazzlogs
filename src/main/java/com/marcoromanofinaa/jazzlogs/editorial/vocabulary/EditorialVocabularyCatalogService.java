package com.marcoromanofinaa.jazzlogs.editorial.vocabulary;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.io.InputStream;
import java.util.Collection;
import java.util.EnumMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;

@Service
public class EditorialVocabularyCatalogService {

    private final Map<EditorialVocabularyType, Set<String>> valuesByType;

    public EditorialVocabularyCatalogService(ObjectMapper objectMapper) {
        this.valuesByType = loadCatalogs(objectMapper);
    }

    public boolean contains(EditorialVocabularyType type, String value) {
        if (value == null || value.isBlank()) {
            return false;
        }
        return valuesByType.getOrDefault(type, Set.of()).contains(value.trim());
    }

    public List<String> invalidValues(EditorialVocabularyType type, Collection<String> values) {
        if (values == null || values.isEmpty()) {
            return List.of();
        }
        var invalid = new LinkedHashSet<String>();
        for (var value : values) {
            if (value == null || value.isBlank()) {
                continue;
            }
            var trimmed = value.trim();
            if (!contains(type, trimmed)) {
                invalid.add(trimmed);
            }
        }
        return List.copyOf(invalid);
    }

    private Map<EditorialVocabularyType, Set<String>> loadCatalogs(ObjectMapper objectMapper) {
        var values = new EnumMap<EditorialVocabularyType, Set<String>>(EditorialVocabularyType.class);
        for (var type : EditorialVocabularyType.values()) {
            values.put(type, loadVocabulary(type, objectMapper));
        }
        return Map.copyOf(values);
    }

    private Set<String> loadVocabulary(EditorialVocabularyType type, ObjectMapper objectMapper) {
        var resource = new ClassPathResource("editorial-vocabulary/" + type.resourceName());
        try (InputStream inputStream = resource.getInputStream()) {
            var document = objectMapper.readValue(inputStream, VocabularyDocument.class);
            var values = new LinkedHashSet<String>();
            if (document.values() != null) {
                for (var value : document.values()) {
                    if (value != null && !value.isBlank()) {
                        values.add(value.trim());
                    }
                }
            }
            return Set.copyOf(values);
        } catch (IOException exception) {
            throw new IllegalStateException("Failed to load editorial vocabulary catalog: " + type.resourceName(), exception);
        }
    }

    private record VocabularyDocument(List<String> values) {
    }
}
