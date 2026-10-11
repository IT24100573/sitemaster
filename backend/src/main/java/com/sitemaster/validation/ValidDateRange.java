package com.sitemaster.validation;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Class-level rule: the end date must be on or after the start date. The error is reported on the end-date field.
 */
@Documented
@Constraint(validatedBy = ValidDateRangeValidator.class)
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
public @interface ValidDateRange {

    String message() default "{daterange.invalid}";

    /** Property holding the start date. */
    String start();

    /** Property holding the end date. */
    String end();

    Class<?>[] groups() default {};

    Class<? extends Payload>[] payload() default {};
}
