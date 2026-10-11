package com.sitemaster.validation;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Sri Lankan NIC: old format (9 digits + V or X) or new format (12 digits). Null is accepted; combine with @NotBlank.
 */
@Documented
@Constraint(validatedBy = ValidNICValidator.class)
@Target({ElementType.FIELD, ElementType.PARAMETER})
@Retention(RetentionPolicy.RUNTIME)
public @interface ValidNIC {

    String message() default "{nic.invalid}";

    Class<?>[] groups() default {};

    Class<? extends Payload>[] payload() default {};
}
