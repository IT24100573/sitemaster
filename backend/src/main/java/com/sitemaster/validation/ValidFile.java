package com.sitemaster.validation;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Uploaded file must be non-empty, within the size limit, of an allowed type, and its real content (checked with Apache Tika) must match its extension.
 */
@Documented
@Constraint(validatedBy = ValidFileValidator.class)
@Target({ElementType.PARAMETER, ElementType.FIELD})
@Retention(RetentionPolicy.RUNTIME)
public @interface ValidFile {

    String message() default "{file.invalid}";

    /** Which upload rules apply. */
    com.sitemaster.file.FileCategory category();

    /** When false, a missing or empty file is accepted (optional uploads). */
    boolean required() default true;

    Class<?>[] groups() default {};

    Class<? extends Payload>[] payload() default {};
}
