package com.sitemaster.supplier.dto;

import java.time.LocalDateTime;
import java.util.List;

/** A supplier application as shown to the Admin. */
public record SupplierApplicationResponse(Long id,
                                          String companyName,
                                          String registrationNo,
                                          String contactPerson,
                                          String email,
                                          String phone,
                                          String address,
                                          List<String> materialCategories,
                                          String documentName,
                                          String status,
                                          String reviewedByName,
                                          LocalDateTime reviewedAt,
                                          String reviewRemarks,
                                          Long supplierId,
                                          LocalDateTime createdAt) {

    public SupplierApplicationResponse {
        materialCategories = List.copyOf(materialCategories);
    }
}
