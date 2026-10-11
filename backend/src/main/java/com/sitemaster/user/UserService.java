package com.sitemaster.user;

import com.sitemaster.common.dto.PageQuery;
import com.sitemaster.common.dto.PageResponse;
import com.sitemaster.common.enums.RoleName;
import com.sitemaster.user.dto.UserCreateRequest;
import com.sitemaster.user.dto.UserResponse;
import com.sitemaster.user.dto.UserUpdateRequest;

/**
 * Admin user management.
 * <p>
 * OOP concept: Abstraction - controllers depend on this interface.
 */
public interface UserService {

    PageResponse<UserResponse> search(String search, RoleName role, Boolean active, PageQuery pageQuery);

    UserResponse get(Long id);

    /** Creates a staff account and issues a set-password link. */
    UserResponse create(UserCreateRequest request);

    UserResponse update(Long id, UserUpdateRequest request);

    UserResponse changeStatus(Long id, boolean active);

    UserResponse unlock(Long id);

    void sendPasswordLink(Long id);

    /**
     * Creates a WORKER account (used when HR registers a worker) and issues a
     * set-password link. Uniqueness of the email is checked here.
     */
    User createWorkerAccount(String fullName, String email, String phone);

    /**
     * Creates a SUPPLIER account when an application is approved and issues a
     * set-password link (written to the console on localhost).
     */
    User createSupplierAccount(String fullName, String email, String phone);
}
