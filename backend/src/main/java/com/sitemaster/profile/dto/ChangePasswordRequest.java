package com.sitemaster.profile.dto;

import com.sitemaster.validation.PasswordMatches;
import com.sitemaster.validation.StrongPassword;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * Request body of PUT /api/profile/password. "Current password is correct" and
 * "new differs from current" are checked in the service.
 * <p>
 * OOP concept: Encapsulation - private fields, explicit accessors, field and
 * class-level rules.
 */
@PasswordMatches(passwordField = "newPassword", confirmField = "confirmPassword")
public class ChangePasswordRequest {

    @NotBlank(message = "{field.required}")
    @Size(max = 64, message = "{field.password.size}")
    private String currentPassword;

    @NotBlank(message = "{field.required}")
    @StrongPassword
    private String newPassword;

    @NotBlank(message = "{field.required}")
    private String confirmPassword;

    public String getCurrentPassword() {
        return currentPassword;
    }

    public void setCurrentPassword(String currentPassword) {
        this.currentPassword = currentPassword;
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
