package com.aivanouski.ttstorage.validation;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;
import jakarta.validation.ReportAsSingleViolation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Target({ElementType.FIELD, ElementType.PARAMETER})
@Constraint(validatedBy = {ValidVisibilityValidator.class})
@Retention(RetentionPolicy.RUNTIME)
@ReportAsSingleViolation
public @interface ValidVisibility {
    String message() default "must not be null or empty and must include valid values: {validValues}";

    Class<?>[] groups() default {};

    Class<? extends Payload>[] payload() default {};
}