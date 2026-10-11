package com.sitemaster.audit;

import com.sitemaster.common.enums.AppModule;
import org.springframework.data.jpa.domain.Specification;

import java.time.LocalDateTime;
import java.util.Locale;

/** Reusable filters for the audit log list. */
public final class AuditLogSpecifications {

    private AuditLogSpecifications() {
        // utility class
    }

    public static Specification<AuditLog> actor(Long actorId) {
        return (root, query, cb) -> actorId == null ? null : cb.equal(root.get("actorId"), actorId);
    }

    public static Specification<AuditLog> module(AppModule module) {
        return (root, query, cb) -> module == null ? null : cb.equal(root.get("module"), module);
    }

    public static Specification<AuditLog> from(LocalDateTime from) {
        return (root, query, cb) -> from == null ? null : cb.greaterThanOrEqualTo(root.get("createdAt"), from);
    }

    public static Specification<AuditLog> before(LocalDateTime before) {
        return (root, query, cb) -> before == null ? null : cb.lessThan(root.get("createdAt"), before);
    }

    public static Specification<AuditLog> text(String search) {
        return (root, query, cb) -> {
            if (search == null) {
                return null;
            }
            String like = "%" + search.toLowerCase(Locale.ROOT) + "%";
            return cb.or(cb.like(cb.lower(root.get("description")), like),
                    cb.like(cb.lower(root.get("actorName")), like));
        };
    }
}
