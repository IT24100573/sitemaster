package com.sitemaster.validation;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Sri Lankan phone number: 0XXXXXXXXX or +94XXXXXXXXX. Null is accepted; combine with @NotBlank when required.
 */
@Documented
@Constraint(validatedBy = ValidSriLankanPhoneValidator.class)
@Target({ElementType.FIELD, ElementType.PARAMETER})
@Retention(RetentionPolicy.RUNTIME)
public @interface ValidSriLankanPhone {

    String message() default "{phone.invalid}";

    Class<?>[] groups() default {};

    Class<? extends Payload>[] payload() default {};
}
