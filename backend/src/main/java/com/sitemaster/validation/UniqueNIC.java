package com.sitemaster.validation;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import jakarta.validation.Constraint;
import jakarta.validation.Payload;

/**
 * No employee may already have this NIC. For create requests; updates check uniqueness in the service.
 */
@Documented
@Constraint(validatedBy = UniqueNICValidator.class)
@Target({ ElementType.FIELD, ElementType.METHOD, ElementType.PARAMETER, ElementType.ANNOTATION_TYPE })
@Retention(RetentionPolicy.RUNTIME)
public @interface UniqueNIC {
    
    String message() default "NIC already exists";
    Class<?>[] groups() default {};
    Class<? extends Payload>[] payload() default {};
}
