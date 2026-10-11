package com.sitemaster.validation;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

import java.time.Clock;
import java.time.LocalDate;

/**
 * Checks {@link NotFutureDate} using the application Clock bean.
 * <p>
 * OOP concept: Polymorphism - a ConstraintValidator implementation.
 */
public class NotFutureDateValidator implements ConstraintValidator<NotFutureDate, LocalDate> {

    private final Clock clock;

    public NotFutureDateValidator(Clock clock) {
        this.clock = clock;
    }

    @Override
    public boolean isValid(LocalDate value, ConstraintValidatorContext context) {
        return value == null || !value.isAfter(LocalDate.now(clock));
    }
}
