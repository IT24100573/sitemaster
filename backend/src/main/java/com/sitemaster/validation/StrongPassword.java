package com.sitemaster.validation;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Password must be 8-64 characters with at least one uppercase letter, one
 * lowercase letter, one digit and one special character. Null is accepted so
 * that @NotBlank reports a missing value with its own message.
 */
@Documented
@Constraint(validatedBy = StrongPasswordValidator.class)
@Target({ElementType.FIELD, ElementType.PARAMETER})
@Retention(RetentionPolicy.RUNTIME)
public @interface StrongPassword {

    String message() default "{password.strong}";

    Class<?>[] groups() default {};

    Class<? extends Payload>[] payload() default {};
}
