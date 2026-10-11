package com.sitemaster.validation;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;
import org.springframework.beans.BeanWrapper;
import org.springframework.beans.BeanWrapperImpl;

import java.util.Objects;

/**
 * Checks the {@link PasswordMatches} rule on any DTO by reading the two
 * configured properties through their getters.
 * <p>
 * OOP concept: Polymorphism - a ConstraintValidator implementation that works on
 * any class, because it depends only on property names.
 */
public class PasswordMatchesValidator implements ConstraintValidator<PasswordMatches, Object> {

    private String passwordField;
    private String confirmField;

    @Override
    public void initialize(PasswordMatches annotation) {
        this.passwordField = annotation.passwordField();
        this.confirmField = annotation.confirmField();
    }

    @Override
    public boolean isValid(Object value, ConstraintValidatorContext context) {
        if (value == null) {
            return true;
        }
        BeanWrapper wrapper = new BeanWrapperImpl(value);
        Object password = wrapper.getPropertyValue(passwordField);
        Object confirm = wrapper.getPropertyValue(confirmField);
        if (password == null || confirm == null || Objects.equals(password, confirm)) {
            return true;
        }
        context.disableDefaultConstraintViolation();
        context.buildConstraintViolationWithTemplate(context.getDefaultConstraintMessageTemplate())
                .addPropertyNode(confirmField)
                .addConstraintViolation();
        return false;
    }
}
