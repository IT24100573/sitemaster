package com.sitemaster.auth.dto;

import com.sitemaster.validation.PasswordMatches;
import com.sitemaster.validation.StrongPassword;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * Request body of POST /api/auth/reset-password.
 * <p>
 * OOP concept: Encapsulation - private fields, explicit accessors and both
 * field-level (@StrongPassword) and class-level (@PasswordMatches) validation.
 */
@PasswordMatches(passwordField = "newPassword", confirmField = "confirmPassword")
public class ResetPasswordRequest {

    @NotBlank(message = "{token.required}")
    @Size(max = 200, message = "{token.invalid}")
    private String token;

    @NotBlank(message = "{field.required}")
    @StrongPassword
    private String newPassword;

    @NotBlank(message = "{field.required}")
    private String confirmPassword;

    public String getToken() {
        return token;
    }

    public void setToken(String token) {
        this.token = token;
    }

    public String getNewPassword() {
        return newPassword;
    }

    public void setNewPassword(String newPassword) {
        this.newPassword = newPassword;
    }

    public String getConfirmPassword() {
        return confirmPassword;
    }

    public void setConfirmPassword(String confirmPassword) {
        this.confirmPassword = confirmPassword;
    }
}
