package com.sitemaster.common.enums;

/**
 * Lifecycle of a project. Allowed transitions are enforced in the project service.
 * <p>
 * OOP concept: Abstraction - a closed, type-safe set of values stored as text.
 */
public enum ProjectStatus {
    PLANNED,
    ACTIVE,
    DELAYED,
    COMPLETED,
    ARCHIVED
}
