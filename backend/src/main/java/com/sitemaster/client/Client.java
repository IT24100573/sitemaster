package com.sitemaster.client;

import com.sitemaster.common.entity.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;

import java.util.Locale;

/**
 * A customer that projects are built for. Never hard-deleted; deactivated instead.
 * <p>
 * OOP concepts: Inheritance (extends BaseEntity) and Encapsulation (email is
 * always stored lowercase; state changes through activate/deactivate).
 */
@Entity
@Table(name = "clients")
public class Client extends BaseEntity {

    @Column(name = "name", nullable = false, length = 150)
    private String name;

    @Column(name = "contact_person", length = 100)
    private String contactPerson;

    @Column(name = "email", nullable = false, length = 100)
    private String email;

    @Column(name = "phone", nullable = false, length = 15)
    private String phone;

    @Column(name = "address", length = 255)
    private String address;

    @Column(name = "active", nullable = false)
    private boolean active = true;

    protected Client() {
        // required by JPA
    }

    public Client(String name) {
        this.name = name;
    }

    public void updateDetails(String name, String contactPerson, String email, String phone, String address) {
        this.name = name;
        this.contactPerson = contactPerson;
        this.email = email == null ? null : email.trim().toLowerCase(Locale.ROOT);
        this.phone = phone;
        this.address = address;
    }

    public void activate() {
        this.active = true;
    }

    public void deactivate() {
        this.active = false;
    }

    public String getName() {
        return name;
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
