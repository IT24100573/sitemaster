package com.sitemaster.employee;

import com.sitemaster.common.exception.BusinessRuleException;
import com.sitemaster.security.CurrentUserProvider;
import org.springframework.stereotype.Component;

/**
 * The Employee record of the signed-in user (needed for check-in, leave and
 * payslips). Users without one get a clear message to contact HR.
 * <p>
 * OOP concept: Encapsulation - one place maps the session user to an employee.
 */
@Component
public class CurrentEmployeeProvider {

    private final EmployeeRepository employeeRepository;
    private final CurrentUserProvider currentUser;

    public CurrentEmployeeProvider(EmployeeRepository employeeRepository, CurrentUserProvider currentUser) {
        this.employeeRepository = employeeRepository;
        this.currentUser = currentUser;
    }

    public Employee requireEmployee() {
        return employeeRepository.findByUserId(currentUser.requireId())
                .orElseThrow(() -> new BusinessRuleException("employee.notRegistered"));
    }
}
