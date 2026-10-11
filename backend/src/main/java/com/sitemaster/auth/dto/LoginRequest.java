package com.sitemaster.auth.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * Request body of POST /api/auth/login.
 * <p>
 * OOP concept: Encapsulation - private fields with explicit accessors and
 * declarative validation rules.
 */
public class LoginRequest {

    @NotBlank(message = "{field.required}")
    @Email(message = "{field.email.invalid}")
    @Size(max = 100, message = "{field.email.size}")
    private String email;

    @NotBlank(message = "{field.required}")
    @Size(max = 64, message = "{field.password.size}")
    private String password;

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }
}
