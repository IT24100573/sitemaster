package com.sitemaster.employee.dto;

import com.sitemaster.validation.UniqueEmail;
import com.sitemaster.validation.UniqueNIC;
import com.sitemaster.validation.ValidationPatterns;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/**
 * Request body of POST /api/employees. Either link an existing staff account
 * ({@code existingUserId}) or give an {@code email} to create a new WORKER
 * account; the service enforces that exactly one of the two is used.
 * <p>
 * OOP concepts: Inheritance (extends EmployeeDetailsRequest) and Polymorphism
 * (overrides getNic() to add the create-only @UniqueNIC rule).
 */
public class EmployeeCreateRequest extends EmployeeDetailsRequest {

    private Long existingUserId;

    @Email(message = "{field.email.invalid}")
    @Pattern(regexp = ValidationPatterns.EMAIL, message = "{field.email.invalid}")
    @Size(max = 100, message = "{field.email.size}")
    @UniqueEmail
    private String email;

    @Override
    @UniqueNIC
    public String getNic() {
        return super.getNic();
    }

    public Long getExistingUserId() {
        return existingUserId;
    }

    public void setExistingUserId(Long existingUserId) {
        this.existingUserId = existingUserId;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }
}
