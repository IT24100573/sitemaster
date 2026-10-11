package com.sitemaster.supplier.dto;

import com.sitemaster.file.FileCategory;
import com.sitemaster.validation.ValidFile;
import com.sitemaster.validation.ValidSriLankanPhone;
import com.sitemaster.validation.ValidationPatterns;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

/**
 * The public "Become a Supplier" form (multipart: fields plus the business
 * registration document).
 * <p>
 * OOP concept: Encapsulation - private fields, explicit accessors, declarative rules.
 */
public class SupplierApplicationForm {

    @NotBlank(message = "{field.required}")
    @Size(min = 2, max = 150, message = "{organisation.name.size}")
    private String companyName;

    @NotBlank(message = "{field.required}")
    @Pattern(regexp = ValidationPatterns.REGISTRATION_NO, message = "{registrationNo.pattern}")
    private String registrationNo;

    @NotBlank(message = "{field.required}")
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

    @NotBlank(message = "{field.required}")
    @Size(max = 255, message = "{address.size}")
    private String address;

    @NotEmpty(message = "{materialCategories.choose}")
    @Size(max = 50, message = "{materialCategories.size}")
    private List<@NotBlank(message = "{field.required}") @Size(max = 50, message = "{trade.size}") String> materialCategories;

    @ValidFile(category = FileCategory.DOCUMENT)
    private MultipartFile registrationDocument;

    public String getCompanyName() {
        return companyName;
    }

    public void setCompanyName(String companyName) {
        this.companyName = companyName;
    }

    public String getRegistrationNo() {
        return registrationNo;
    }

    public void setRegistrationNo(String registrationNo) {
        this.registrationNo = registrationNo;
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

    public List<String> getMaterialCategories() {
        return materialCategories;
    }

    public void setMaterialCategories(List<String> materialCategories) {
        this.materialCategories = materialCategories;
    }

    public MultipartFile getRegistrationDocument() {
        return registrationDocument;
    }

    public void setRegistrationDocument(MultipartFile registrationDocument) {
        this.registrationDocument = registrationDocument;
    }
}
