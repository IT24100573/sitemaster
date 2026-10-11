package com.sitemaster.attendance;

import com.sitemaster.common.entity.BaseEntity;
import com.sitemaster.common.exception.BusinessRuleException;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;

import java.time.LocalTime;

/**
 * A named working shift (e.g. Day 08:00-17:00).
 * <p>
 * OOP concepts: Inheritance (extends BaseEntity) and Encapsulation (end must be after start).
 */
@Entity
@Table(name = "shifts")
public class Shift extends BaseEntity {

    @Column(name = "name", nullable = false, length = 50)
    private String name;

    @Column(name = "start_time", nullable = false)
    private LocalTime startTime;

    @Column(name = "end_time", nullable = false)
    private LocalTime endTime;

    @Column(name = "description", length = 255)
    private String description;

    @Column(name = "active", nullable = false)
    private boolean active = true;

    protected Shift() {
        // required by JPA
    }

    public Shift(String name) {
        this.name = name;
    }

    public void update(String name, LocalTime start, LocalTime end, String description) {
        if (!end.isAfter(start)) {
            throw BusinessRuleException.forField("endTime", "shift.times.order");
        }
        this.name = name;
        this.startTime = start;
        this.endTime = end;
        this.description = description;
    }

    public void setActive(boolean active) {
        this.active = active;
    }

    public String getName() {
        return name;
    }

    public LocalTime getStartTime() {
        return startTime;
    }

    public LocalTime getEndTime() {
        return endTime;
    }

    public String getDescription() {
        return description;
    }

    public boolean isActive() {
        return active;
    }
}
