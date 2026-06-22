package com.marcoromanofinaa.jazzlogs.editorial.vocabulary;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import org.springframework.stereotype.Component;

@Component
public class ValuesFromVocabularyValidator implements ConstraintValidator<ValuesFromVocabulary, Object> {

    private final EditorialVocabularyCatalogService vocabularyCatalogService;
    private EditorialVocabularyType vocabularyType;

    public ValuesFromVocabularyValidator(EditorialVocabularyCatalogService vocabularyCatalogService) {
        this.vocabularyCatalogService = vocabularyCatalogService;
    }

    @Override
    public void initialize(ValuesFromVocabulary annotation) {
        this.vocabularyType = annotation.value();
    }

    @Override
    public boolean isValid(Object value, ConstraintValidatorContext context) {
        if (value == null) {
            return true;
        }

        var renderedValues = toStringValues(value);
        var invalidValues = vocabularyCatalogService.invalidValues(vocabularyType, renderedValues);
        if (invalidValues.isEmpty()) {
            return true;
        }

        context.disableDefaultConstraintViolation();
        context.buildConstraintViolationWithTemplate(
                "contains values outside the %s catalog: %s"
                        .formatted(vocabularyType.name().toLowerCase(), String.join(", ", invalidValues))
        ).addConstraintViolation();
        return false;
    }

    private List<String> toStringValues(Object value) {
        if (value instanceof String stringValue) {
            return List.of(stringValue);
        }
        if (value instanceof Collection<?> collection) {
            var values = new ArrayList<String>(collection.size());
            for (var item : collection) {
                if (item != null) {
                    values.add(item.toString());
                }
            }
            return List.copyOf(values);
        }
        throw new IllegalArgumentException(
                "ValuesFromVocabulary can only validate String or Collection values, got: " + value.getClass().getName()
        );
    }
}
