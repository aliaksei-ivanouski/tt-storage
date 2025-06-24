package com.aivanouski.ttstorage.validation;

import com.aivanouski.ttstorage.file.File;
import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

import java.util.Arrays;
import java.util.stream.Collectors;

public class ValidVisibilityValidator implements ConstraintValidator<ValidVisibility, String> {
    @Override
    public boolean isValid(String value, ConstraintValidatorContext context) {
        String validValues = Arrays.stream(File.Visibility.values())
                .map(Enum::name)
                .collect(Collectors.joining(", "));

        boolean isMatch = Arrays.stream(File.Visibility.values()).anyMatch(v -> v.name().equals(value));
        if (value != null && isMatch) {
            return true;
        }

        String message = context.getDefaultConstraintMessageTemplate().replace("{validValues}", validValues);
        context.buildConstraintViolationWithTemplate(message)
                .addConstraintViolation();

        return false;
    }
}