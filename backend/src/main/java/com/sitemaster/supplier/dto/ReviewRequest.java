package com.sitemaster.supplier.dto;

import jakarta.validation.constraints.Size;

/**
 * Admin's remarks when approving (optional) or rejecting (required, min 5 characters)
 * a supplier application.
 * <p>
 * OOP concept: Encapsulation - private field with explicit accessors.
 */
public class ReviewRequest {

    @Size(max = 500, message = "{note.size}")
    private String remarks;

    public String getRemarks() {
        return remarks;
    }

    public void setRemarks(String remarks) {
        this.remarks = remarks;
    }
}
