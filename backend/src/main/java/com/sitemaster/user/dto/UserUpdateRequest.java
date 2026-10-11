package com.sitemaster.user.dto;

import com.sitemaster.common.enums.RoleName;
import com.sitemaster.validation.ValidSriLankanPhone;
import com.sitemaster.validation.ValidationPatterns;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/**
 * Request body of PUT /api/admin/users/{id}. Email uniqueness (excluding this
 * user) and the "last active admin" rule are checked in the service.
 * <p>
 * OOP concept: Encapsulation - private fields, explicit accessors, declarative rules.
 */
public class UserUpdateRequest {

    @NotBlank(message = "{field.required}")
    @Size(min = 2, max = 100, message = "{name.size}")
    @Pattern(regexp = ValidationPatterns.PERSON_NAME, message = "{name.pattern}")
    private String fullName;

    @NotBlank(message = "{field.required}")
    @Email(message = "{field.email.invalid}")
    @Pattern(regexp = ValidationPatterns.EMAIL, message = "{field.email.invalid}")
    @Size(max = 100, message = "{field.email.size}")
    private String email;

    @ValidSriLankanPhone
    private String phone;

    @NotNull(message = "{field.required}")
    private RoleName role;

    public String getFullName() {
        return fullName;
    }

    public void setFullName(String fullName) {
        this.fullName = fullName;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getPhone() {
        return phone;
    }

    public void setPhone(String phone) {
        this.phone = phone;
    }

    public RoleName getRole() {
        return role;
    }

    public void setRole(RoleName role) {
        this.role = role;
    }
}
