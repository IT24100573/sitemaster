package com.sitemaster.client.dto;

import com.sitemaster.validation.ValidSriLankanPhone;
import com.sitemaster.validation.ValidationPatterns;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/**
 * Request body for creating or updating a client.
 * <p>
 * OOP concept: Encapsulation - private fields, explicit accessors, declarative rules.
 */
public class ClientRequest {

    @NotBlank(message = "{field.required}")
    @Size(min = 2, max = 150, message = "{organisation.name.size}")
    private String name;

    @Size(min = 2, max = 100, message = "{name.size}")
    @Pattern(regexp = ValidationPatterns.PERSON_NAME, message = "{name.pattern}")
    private String contactPerson;

    @NotBlank(message = "{field.required}")
    @Email(message = "{field.email.invalid}")
    @Pattern(regexp = ValidationPatterns.EMAIL, message = "{field.email.invalid}")
    @Size(max = 100, message = "{field.email.size}")
    private String email;

    @NotBlank(message = "{field.required}")
    @ValidSriLankanPhone
    private String phone;

    @Size(max = 255, message = "{address.size}")
    private String address;

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getContactPerson() {
        return contactPerson;
    }

    public void setContactPerson(String contactPerson) {
        this.contactPerson = contactPerson;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
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
}
