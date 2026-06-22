package com.marcoromanofinaa.jazzlogs.editorial.vocabulary;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Target({ElementType.FIELD, ElementType.PARAMETER, ElementType.RECORD_COMPONENT})
@Retention(RetentionPolicy.RUNTIME)
@Constraint(validatedBy = ValuesFromVocabularyValidator.class)
public @interface ValuesFromVocabulary {

    String message() default "contains values outside the allowed editorial vocabulary";

    Class<?>[] groups() default {};

    Class<? extends Payload>[] payload() default {};

    EditorialVocabularyType value();
}
