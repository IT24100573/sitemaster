package com.sitemaster.validation;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

import java.util.regex.Pattern;

/**
 * Checks {@link ValidNIC}.
 * <p>
 * OOP concept: Polymorphism - a ConstraintValidator implementation.
 */
public class ValidNICValidator implements ConstraintValidator<ValidNIC, String> {

    private static final Pattern NIC = Pattern.compile(ValidationPatterns.NIC);

    @Override
    public boolean isValid(String value, ConstraintValidatorContext context) {
        return value == null || NIC.matcher(value).matches();
    }
}
