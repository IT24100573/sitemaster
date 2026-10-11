package com.sitemaster.validation;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;
import org.springframework.beans.BeanWrapper;
import org.springframework.beans.BeanWrapperImpl;

import java.time.LocalDate;

/**
 * Checks {@link ValidDateRange}: end &gt;= start when both are present.
 * <p>
 * OOP concept: Polymorphism - a ConstraintValidator implementation reusable on any DTO.
 */
public class ValidDateRangeValidator implements ConstraintValidator<ValidDateRange, Object> {

    private String startField;
    private String endField;

    @Override
    public void initialize(ValidDateRange annotation) {
        this.startField = annotation.start();
        this.endField = annotation.end();
    }

    @Override
    public boolean isValid(Object value, ConstraintValidatorContext context) {
        if (value == null) {
            return true;
        }
        BeanWrapper wrapper = new BeanWrapperImpl(value);
        Object start = wrapper.getPropertyValue(startField);
        Object end = wrapper.getPropertyValue(endField);
        if (!(start instanceof LocalDate startDate) || !(end instanceof LocalDate endDate)
                || !endDate.isBefore(startDate)) {
            return true;
        }
        context.disableDefaultConstraintViolation();
        context.buildConstraintViolationWithTemplate(context.getDefaultConstraintMessageTemplate())
                .addPropertyNode(endField)
                .addConstraintViolation();
        return false;
    }
}
