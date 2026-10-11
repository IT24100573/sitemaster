package com.sitemaster.auth.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * Request body of POST /api/auth/forgot-password.
 * <p>
 * OOP concept: Encapsulation - private field with explicit accessors.
 */
public class ForgotPasswordRequest {

    @NotBlank(message = "{field.required}")
    @Email(message = "{field.email.invalid}")
    @Size(max = 100, message = "{field.email.size}")
    private String email;

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }
}
