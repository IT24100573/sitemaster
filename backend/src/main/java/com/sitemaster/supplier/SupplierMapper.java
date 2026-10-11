package com.sitemaster.supplier;

import com.sitemaster.supplier.dto.SupplierApplicationResponse;
import com.sitemaster.supplier.dto.SupplierResponse;

/**
 * Maps supplier entities to DTOs.
 * <p>
 * OOP concept: Encapsulation - entities never leave the service layer.
 */
public final class SupplierMapper {

    private SupplierMapper() {
        // utility class
    }

    public static SupplierResponse toResponse(Supplier s) {
        return new SupplierResponse(s.getId(), s.getName(), s.getRegistrationNo(), s.getContactPerson(), s.getEmail(),
                s.getPhone(), s.getAddress(), s.getMaterialCategories(), s.isActive(),
                s.getUser() == null ? null : s.getUser().getId(), s.getCreatedAt());
    }

    public static SupplierApplicationResponse toResponse(SupplierApplication a) {
        return new SupplierApplicationResponse(a.getId(), a.getCompanyName(), a.getRegistrationNo(),
                a.getContactPerson(), a.getEmail(), a.getPhone(), a.getAddress(), a.getMaterialCategories(),
                a.getRegistrationDocument().getOriginalName(), a.getStatus().name(),
                a.getReviewedBy() == null ? null : a.getReviewedBy().getFullName(), a.getReviewedAt(),
                a.getReviewRemarks(), a.getSupplier() == null ? null : a.getSupplier().getId(), a.getCreatedAt());
    }
}
