package com.sitemaster.user;

import com.sitemaster.common.enums.RoleName;
import org.springframework.data.jpa.domain.Specification;

import java.util.Locale;

/** Reusable filters for the user list. */
public final class UserSpecifications {

    private UserSpecifications() {
        // utility class
    }

    public static Specification<User> text(String search) {
        return (root, query, cb) -> {
            if (search == null) {
                return null;
            }
            String like = "%" + search.toLowerCase(Locale.ROOT) + "%";
            return cb.or(cb.like(cb.lower(root.get("fullName")), like), cb.like(cb.lower(root.get("email")), like));
        };
    }

    public static Specification<User> role(RoleName role) {
        return (root, query, cb) -> role == null ? null : cb.equal(root.get("role").get("name"), role);
    }

    public static Specification<User> active(Boolean active) {
        return (root, query, cb) -> active == null ? null : cb.equal(root.get("active"), active);
    }
}
