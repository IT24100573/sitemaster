package com.sitemaster.equipment.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/**
 * Create or edit an equipment register entry.
 * <p>
 * OOP concept: Encapsulation - private fields with explicit accessors.
 */
public class EquipmentRequest {

    @NotBlank(message = "{field.required}")
    @Pattern(regexp = "^[A-Za-z0-9-]{2,30}$", message = "{equipment.code.pattern}")
    private String code;

    @NotBlank(message = "{field.required}")
    @Size(min = 2, max = 100, message = "{equipment.name.size}")
    private String name;

    @NotBlank(message = "{field.required}")
    @Size(min = 2, max = 50, message = "{equipment.category.size}")
    private String category;

    public String getCode() {
        return code;
    }

    public void setCode(String code) {
        this.code = code;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getCategory() {
        return category;
    }

    public void setCategory(String category) {
        this.category = category;
    }
}
