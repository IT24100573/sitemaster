package com.sitemaster.supplier.dto;

import java.time.LocalDateTime;
import java.util.List;

/** An approved supplier. */
public record SupplierResponse(Long id,
                               String name,
                               String registrationNo,
                               String contactPerson,
                               String email,
                               String phone,
                               String address,
                               List<String> materialCategories,
                               boolean active,
                               Long userId,
                               LocalDateTime createdAt) {

    public SupplierResponse {
        materialCategories = List.copyOf(materialCategories);
    }
}
