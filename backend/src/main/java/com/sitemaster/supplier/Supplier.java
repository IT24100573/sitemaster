package com.sitemaster.supplier;

import com.sitemaster.common.entity.BaseEntity;
import com.sitemaster.user.User;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;

import java.util.Arrays;
import java.util.List;
import java.util.Locale;

/**
 * An approved supplier company, linked to its SUPPLIER sign-in account.
 * <p>
 * OOP concepts: Inheritance (extends BaseEntity) and Encapsulation (categories
 * are stored as text but exposed as a list).
 */
@Entity
@Table(name = "suppliers")
public class Supplier extends BaseEntity {

    static final String SEPARATOR = ",";

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", unique = true)
    private User user;

    @Column(name = "name", nullable = false, length = 150)
    private String name;

    @Column(name = "registration_no", length = 50)
    private String registrationNo;

    @Column(name = "contact_person", nullable = false, length = 100)
    private String contactPerson;

    @Column(name = "email", nullable = false, length = 100)
    private String email;

    @Column(name = "phone", nullable = false, length = 15)
    private String phone;

    @Column(name = "address", nullable = false, length = 255)
    private String address;

    @Column(name = "material_categories", nullable = false, length = 1000)
    private String materialCategories;

    @Column(name = "active", nullable = false)
    private boolean active = true;

    protected Supplier() {
        // required by JPA
    }

    public Supplier(User user, String name, String registrationNo) {
        this.user = user;
        this.name = name;
        this.registrationNo = registrationNo == null ? null : registrationNo.trim().toUpperCase(Locale.ROOT);
    }

    public void updateContact(String contactPerson, String email, String phone, String address, List<String> categories) {
        this.contactPerson = contactPerson;
        this.email = email.trim().toLowerCase(Locale.ROOT);
        this.phone = phone;
        this.address = address;
        this.materialCategories = String.join(SEPARATOR, categories);
    }

    public List<String> getMaterialCategories() {
        return materialCategories == null || materialCategories.isBlank()
                ? List.of() : Arrays.stream(materialCategories.split(SEPARATOR)).map(String::trim).toList();
    }

    public void activate() {
        active = true;
    }

    public void deactivate() {
        active = false;
    }

    public User getUser() {
        return user;
    }

    public String getName() {
        return name;
    }

    public String getRegistrationNo() {
        return registrationNo;
    }

    public String getContactPerson() {
        return contactPerson;
    }

    public String getEmail() {
        return email;
    }

    public String getPhone() {
        return phone;
    }

    public String getAddress() {
        return address;
    }

    public boolean isActive() {
        return active;
    }
}
