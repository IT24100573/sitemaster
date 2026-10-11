package com.sitemaster.supplier.dto;

import com.sitemaster.validation.ValidSriLankanPhone;
import com.sitemaster.validation.ValidationPatterns;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.util.List;

/**
 * A supplier updating its own company profile. The company name, registration
 * number and sign-in email are fixed (changed only by the Admin).
 * <p>
 * OOP concept: Encapsulation - private fields with explicit accessors.
 */
public class SupplierProfileRequest {

    @NotBlank(message = "{field.required}")
    @Size(min = 2, max = 100, message = "{name.size}")
    @Pattern(regexp = ValidationPatterns.PERSON_NAME, message = "{name.pattern}")
    private String contactPerson;

    @NotBlank(message = "{field.required}")
    @ValidSriLankanPhone
    private String phone;

    @NotBlank(message = "{field.required}")
    @Size(max = 255, message = "{address.size}")
    private String address;

    @NotEmpty(message = "{materialCategories.choose}")
    @Size(max = 50, message = "{materialCategories.size}")
    private List<@NotBlank(message = "{field.required}") String> materialCategories;

    public String getContactPerson() {
        return contactPerson;
    }

    public void setContactPerson(String contactPerson) {
        this.contactPerson = contactPerson;
    }

    public String getPhone() {
        return phone;
    }

    public void setPhone(String phone) {
        this.phone = phone;
    }

    public String getAddress() {
        return address;
    }

    public void setAddress(String address) {
        this.address = address;
    }

    public List<String> getMaterialCategories() {
        return materialCategories;
    }

    public void setMaterialCategories(List<String> materialCategories) {
        this.materialCategories = materialCategories;
    }
}
