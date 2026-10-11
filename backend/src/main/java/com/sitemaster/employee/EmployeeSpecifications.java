package com.sitemaster.employee;

import com.sitemaster.common.enums.EmploymentType;
import jakarta.persistence.criteria.Join;
import org.springframework.data.jpa.domain.Specification;

import java.util.Locale;

/** Reusable filters for the employee list. */
public final class EmployeeSpecifications {

    private EmployeeSpecifications() {
        // utility class
    }

    public static Specification<Employee> text(String search) {
        return (root, query, cb) -> {
            if (search == null) {
                return null;
            }
            Join<Object, Object> user = root.join("user");
            String like = "%" + search.toLowerCase(Locale.ROOT) + "%";
            return cb.or(cb.like(cb.lower(user.get("fullName")), like),
                    cb.like(cb.lower(user.get("email")), like),
                    cb.like(cb.lower(root.get("employeeNo")), like),
                    cb.like(cb.lower(root.get("nic")), like));
        };
    }

    public static Specification<Employee> type(EmploymentType type) {
        return (root, query, cb) -> type == null ? null : cb.equal(root.get("employmentType"), type);
    }

    public static Specification<Employee> trade(String trade) {
        return (root, query, cb) -> trade == null ? null : cb.equal(root.get("trade"), trade);
    }

    public static Specification<Employee> active(Boolean active) {
        return (root, query, cb) -> active == null ? null : cb.equal(root.get("user").get("active"), active);
    }
}
