package com.sitemaster.employee.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

/**
 * Full employee record for the detail and edit pages.
 */
public record EmployeeResponse(Long id,
                               String employeeNo,
                               Long userId,
                               String fullName,
                               String email,
                               String phone,
                               String role,
                               String roleDisplayName,
                               boolean active,
                               boolean hasPhoto,
                               String nic,
                               LocalDate dateOfBirth,
                               int age,
                               String address,
                               LocalDate joinDate,
                               String employmentType,
                               String trade,
                               BigDecimal dailyRate,
                               BigDecimal monthlySalary,
                               BigDecimal contractAmount,
                               LocalDate contractStartDate,
                               LocalDate contractEndDate,
                               String emergencyContactName,
                               String emergencyContactPhone,
                               List<EmployeeDocumentResponse> documents,
                               LocalDateTime createdAt) {

    public EmployeeResponse {
        documents = List.copyOf(documents);
    }
}
