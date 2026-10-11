package com.sitemaster.employee.dto;

/**
 * Request body of PUT /api/employees/{id}. Same fields as on create; the
 * linked account and its email cannot be changed here (email is managed by
 * the Admin in User Management).
 * <p>
 * OOP concept: Inheritance - reuses every field and rule of EmployeeDetailsRequest.
 */
public class EmployeeUpdateRequest extends EmployeeDetailsRequest {
}
