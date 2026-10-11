package com.sitemaster.validation;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

import java.util.regex.Pattern;

/**
 * Checks {@link ValidSriLankanPhone}.
 * <p>
 * OOP concept: Polymorphism - a ConstraintValidator implementation.
 */
public class ValidSriLankanPhoneValidator implements ConstraintValidator<ValidSriLankanPhone, String> {

    private static final Pattern PHONE = Pattern.compile(ValidationPatterns.SRI_LANKAN_PHONE);

    @Override
    public boolean isValid(String value, ConstraintValidatorContext context) {
        return value == null || PHONE.matcher(value).matches();
    }
}
