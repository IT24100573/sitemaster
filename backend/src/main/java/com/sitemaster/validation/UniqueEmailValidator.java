package com.sitemaster.validation;

import com.sitemaster.user.UserRepository;
import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

/**
 * Checks {@link UniqueEmail} against the users table. Spring injects the
 * repository because validators are created by Spring's validator factory.
 * The database unique index remains the final guarantee.
 * <p>
 * OOP concept: Polymorphism - a ConstraintValidator implementation.
 */
public class UniqueEmailValidator implements ConstraintValidator<UniqueEmail, String> {

    private final UserRepository userRepository;

    public UniqueEmailValidator(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @Override
    public boolean isValid(String value, ConstraintValidatorContext context) {
        return value == null || !userRepository.existsByEmailIgnoreCase(value.trim());
    }
}
