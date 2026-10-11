package com.sitemaster.user;

import com.sitemaster.common.entity.BaseEntity;
import com.sitemaster.file.StoredFile;
import com.sitemaster.role.Role;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Locale;
import java.util.Objects;

/**
 * A person who can sign in to SiteMaster (staff, worker or supplier contact).
 * Users are never hard-deleted; they are deactivated (soft delete).
 * <p>
 * OOP concepts:
 * <ul>
 *   <li>Inheritance - extends BaseEntity.</li>
 *   <li>Encapsulation - sign-in state, role and active flag only change through
 *       intention-revealing methods. Each of those bumps {@link #getSecurityVersion()},
 *       which makes SessionRefreshFilter refresh or end the user's open sessions.</li>
 * </ul>
 */
@Entity
@Table(name = "users")
public class User extends BaseEntity {

    @Column(name = "full_name", nullable = false, length = 100)
    private String fullName;

    @Column(name = "email", nullable = false, length = 100)
    private String email;

    @Column(name = "phone", length = 15)
    private String phone;

    @Column(name = "password_hash", nullable = false, length = 100)
    private String passwordHash;

    @ManyToOne(fetch = FetchType.EAGER, optional = false)
    @JoinColumn(name = "role_id", nullable = false)
    private Role role;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "profile_photo_id")
    private StoredFile profilePhoto;

    @Column(name = "active", nullable = false)
    private boolean active = true;

    @Column(name = "failed_login_attempts", nullable = false)
    private int failedLoginAttempts;

    @Column(name = "locked_until")
    private LocalDateTime lockedUntil;

    @Column(name = "last_login_at")
    private LocalDateTime lastLoginAt;

    @Column(name = "security_version", nullable = false)
    private int securityVersion;

    protected User() {
        // required by JPA
    }

    public User(String fullName, String email, String passwordHash, Role role) {
        setFullName(fullName);
        setEmail(email);
        this.passwordHash = passwordHash;
        this.role = role;
    }

    // ----- sign-in behaviour -------------------------------------------------

    /** True while a lock set by too many failed attempts has not yet expired. */
    public boolean isLockedAt(LocalDateTime now) {
        return lockedUntil != null && lockedUntil.isAfter(now);
    }

    /** Forgets an expired lock so the user starts again with a clean counter. */
    public void clearExpiredLock(LocalDateTime now) {
        if (lockedUntil != null && !lockedUntil.isAfter(now)) {
            unlock();
        }
    }

    /** Counts a failed sign-in and locks the account once the limit is reached. */
    public void registerFailedLogin(int maxAttempts, Duration lockDuration, LocalDateTime now) {
        failedLoginAttempts++;
        if (failedLoginAttempts >= maxAttempts) {
            lockedUntil = now.plus(lockDuration);
        }
    }

    public void registerSuccessfulLogin(LocalDateTime now) {
        failedLoginAttempts = 0;
        lockedUntil = null;
        lastLoginAt = now;
    }

    public void unlock() {
        failedLoginAttempts = 0;
        lockedUntil = null;
    }

    /** Replaces the password. The caller must pass an already-encoded hash. */
    public void changePasswordHash(String newPasswordHash) {
        this.passwordHash = newPasswordHash;
    }

    // ----- account state -------------------------------------------------------

    public void activate() {
        if (!active) {
            active = true;
            bumpSecurityVersion();
        }
    }

    /** Soft delete: the user can no longer sign in and open sessions end. */
    public void deactivate() {
        if (active) {
            active = false;
            bumpSecurityVersion();
        }
    }

    /** Changes the role; open sessions pick up the new permissions on their next request. */
    public void changeRole(Role newRole) {
        if (!Objects.equals(role.getId(), newRole.getId())) {
            this.role = newRole;
            bumpSecurityVersion();
        }
    }

    /** Updates display details; the session principal is refreshed so the new name shows. */
    public void updateDetails(String fullName, String phone) {
        setFullName(fullName);
        this.phone = phone;
        bumpSecurityVersion();
    }

    public void changeProfilePhoto(StoredFile photo) {
        this.profilePhoto = photo;
    }

    public void bumpSecurityVersion() {
        securityVersion++;
    }

    // ----- getters / setters ---------------------------------------------------

    public String getFullName() {
        return fullName;
    }

    public void setFullName(String fullName) {
        this.fullName = fullName == null ? null : fullName.trim();
    }

    public String getEmail() {
        return email;
    }

    /** Emails are stored trimmed and lowercase so uniqueness is case-insensitive. */
    public void setEmail(String email) {
        this.email = email == null ? null : email.trim().toLowerCase(Locale.ROOT);
    }

    public String getPhone() {
        return phone;
    }

    public String getPasswordHash() {
        return passwordHash;
    }

    public Role getRole() {
        return role;
    }

    public StoredFile getProfilePhoto() {
        return profilePhoto;
    }

    public boolean hasProfilePhoto() {
        return profilePhoto != null;
    }

    public boolean isActive() {
        return active;
    }

    public int getFailedLoginAttempts() {
        return failedLoginAttempts;
    }

    public LocalDateTime getLockedUntil() {
        return lockedUntil;
    }

    public LocalDateTime getLastLoginAt() {
        return lastLoginAt;
    }

    public int getSecurityVersion() {
        return securityVersion;
    }
}
