package com.sitemaster.employee;

import com.sitemaster.employee.dto.EmployeeDocumentResponse;
import com.sitemaster.employee.dto.EmployeeListItem;
import com.sitemaster.employee.dto.EmployeeResponse;
import com.sitemaster.user.User;

import java.time.LocalDate;

/**
 * Maps employee entities to response DTOs.
 * <p>
 * OOP concept: Encapsulation - entities never leave the service layer.
 */
public final class EmployeeMapper {

    private EmployeeMapper() {
        // utility class
    }

    public static EmployeeListItem toListItem(Employee e) {
        User u = e.getUser();
        return new EmployeeListItem(e.getId(), e.getEmployeeNo(), u.getId(), u.getFullName(), u.getEmail(),
                u.getRole().getDisplayName(), e.getEmploymentType().name(), e.getTrade(), e.getJoinDate(),
                u.isActive());
    }

    public static EmployeeResponse toResponse(Employee e, LocalDate today) {
        User u = e.getUser();
        return new EmployeeResponse(e.getId(), e.getEmployeeNo(), u.getId(), u.getFullName(), u.getEmail(),
                u.getPhone(), u.getRole().getName().name(), u.getRole().getDisplayName(), u.isActive(),
                u.hasProfilePhoto(), e.getNic(), e.getDateOfBirth(), e.ageOn(today), e.getAddress(),
                e.getJoinDate(), e.getEmploymentType().name(), e.getTrade(), e.getDailyRate(),
                e.getMonthlySalary(), e.getContractAmount(), e.getContractStartDate(), e.getContractEndDate(),
                e.getEmergencyContactName(), e.getEmergencyContactPhone(),
                e.getDocuments().stream().map(EmployeeMapper::toDocument).toList(), e.getCreatedAt());
    }

    public static EmployeeDocumentResponse toDocument(EmployeeDocument d) {
        return new EmployeeDocumentResponse(d.getId(), d.getDocumentType().name(), d.getTitle(),
                d.getFile().getOriginalName(), d.getFile().getContentType(), d.getFile().getSizeBytes(),
                d.getCreatedAt());
    }
}
