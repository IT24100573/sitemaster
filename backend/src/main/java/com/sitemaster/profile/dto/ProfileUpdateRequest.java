package com.sitemaster.profile.dto;

import com.sitemaster.validation.ValidSriLankanPhone;
import com.sitemaster.validation.ValidationPatterns;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/**
 * Request body of PUT /api/profile. Email and role are managed by the Admin.
 * <p>
 * OOP concept: Encapsulation - private fields, explicit accessors.
 */
public class ProfileUpdateRequest {

    @NotBlank(message = "{field.required}")
    @Size(min = 2, max = 100, message = "{name.size}")
    @Pattern(regexp = ValidationPatterns.PERSON_NAME, message = "{name.pattern}")
    private String fullName;

    @ValidSriLankanPhone
    private String phone;

    public String getFullName() {
        return fullName;
    }

    public void setFullName(String fullName) {
        this.fullName = fullName;
    }

    public String getPhone() {
        return phone;
    }

    public void setPhone(String phone) {
        this.phone = phone;
    }
}
