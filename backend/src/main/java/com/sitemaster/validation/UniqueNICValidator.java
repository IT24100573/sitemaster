package com.sitemaster.validation;

import com.sitemaster.employee.EmployeeRepository;
import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

import java.util.Locale;

/**
 * Checks {@link UniqueNIC} against the employees table (NICs are stored uppercase).
 * <p>
 * OOP concept: Polymorphism - a ConstraintValidator implementation.
 */
public class UniqueNICValidator implements ConstraintValidator<UniqueNIC, String> {

    private final EmployeeRepository employeeRepository;

    public UniqueNICValidator(EmployeeRepository employeeRepository) {
        this.employeeRepository = employeeRepository;
    }

    @Override
    public boolean isValid(String value, ConstraintValidatorContext context) {
        return value == null || !employeeRepository.existsByNic(value.trim().toUpperCase(Locale.ROOT));
    }
}
