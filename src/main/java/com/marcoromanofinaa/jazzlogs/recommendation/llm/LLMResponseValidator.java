package com.marcoromanofinaa.jazzlogs.recommendation.llm;

import com.marcoromanofinaa.jazzlogs.recommendation.exception.RecommendationGenerationException;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validator;
import java.util.Comparator;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class LLMResponseValidator {

    private final Validator validator;

    public <T> T validate(T response, Class<?> responseType) {
        if (response == null) {
            throw new RecommendationGenerationException(
                    "LLM did not return a parsed " + responseType.getSimpleName() + " response"
            );
        }

        Set<ConstraintViolation<T>> violations = validator.validate(response);
        if (!violations.isEmpty()) {
            var firstViolation = violations.stream()
                    .sorted(Comparator.comparing(v -> v.getPropertyPath().toString()))
                    .findFirst()
                    .orElseThrow();
            throw new RecommendationGenerationException(
                    "Invalid " + responseType.getSimpleName() + " from LLM: "
                            + firstViolation.getPropertyPath() + " " + firstViolation.getMessage()
            );
        }

        return response;
    }
}
