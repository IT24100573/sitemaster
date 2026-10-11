package com.sitemaster.employee.dto;

import java.time.LocalDate;

/**
 * One row of the employee list.
 *
 * @param id              employee id
 * @param employeeNo      e.g. EMP-0007
 * @param userId          linked user id
 * @param fullName        name
 * @param email           email
 * @param roleDisplayName readable role
 * @param employmentType  PERMANENT, CONTRACT or DAILY_WAGE
 * @param trade           trade (workers)
 * @param joinDate        start date
 * @param active          whether the account is active
 */
public record EmployeeListItem(Long id,
                               String employeeNo,
                               Long userId,
                               String fullName,
                               String email,
                               String roleDisplayName,
                               String employmentType,
                               String trade,
                               LocalDate joinDate,
                               boolean active) {
}
