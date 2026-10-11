package com.sitemaster.user;

import com.sitemaster.common.dto.MessageResponse;
import com.sitemaster.common.dto.PageQuery;
import com.sitemaster.common.dto.PageResponse;
import com.sitemaster.common.enums.RoleName;
import com.sitemaster.common.util.MessageResolver;
import com.sitemaster.user.dto.UserCreateRequest;
import com.sitemaster.user.dto.UserResponse;
import com.sitemaster.user.dto.UserStatusRequest;
import com.sitemaster.user.dto.UserUpdateRequest;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/** Admin: User Management page. */
@RestController
@RequestMapping("/api/admin/users")
public class UserController {

    private final UserService userService;
    private final MessageResolver messages;

    public UserController(UserService userService, MessageResolver messages) {
        this.userService = userService;
        this.messages = messages;
    }

    @GetMapping
    @PreAuthorize("hasAuthority('USER:VIEW')")
    public PageResponse<UserResponse> list(@RequestParam(required = false) RoleName role,
                                           @RequestParam(required = false) Boolean active,
                                           @Valid @ModelAttribute PageQuery pageQuery) {
        return userService.search(pageQuery.getSearch(), role, active, pageQuery);
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAuthority('USER:VIEW')")
    public UserResponse get(@PathVariable Long id) {
        return userService.get(id);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasAuthority('USER:CREATE')")
    public UserResponse create(@Valid @RequestBody UserCreateRequest request) {
        return userService.create(request);
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAuthority('USER:EDIT')")
    public UserResponse update(@PathVariable Long id, @Valid @RequestBody UserUpdateRequest request) {
        return userService.update(id, request);
    }

    @PatchMapping("/{id}/status")
    @PreAuthorize("hasAuthority('USER:EDIT')")
    public UserResponse changeStatus(@PathVariable Long id, @Valid @RequestBody UserStatusRequest request) {
        return userService.changeStatus(id, request.getActive());
    }

    @PostMapping("/{id}/unlock")
    @PreAuthorize("hasAuthority('USER:EDIT')")
    public UserResponse unlock(@PathVariable Long id) {
        return userService.unlock(id);
    }

    @PostMapping("/{id}/password-link")
    @PreAuthorize("hasAuthority('USER:EDIT')")
    public MessageResponse sendPasswordLink(@PathVariable Long id) {
        userService.sendPasswordLink(id);
        return new MessageResponse(messages.get("user.passwordLink.sent"));
    }
}
