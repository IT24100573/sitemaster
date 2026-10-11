package com.sitemaster.common.enums;

/**
 * Lifecycle of a client invoice.
 * <p>
 * OOP concept: Abstraction - a closed, type-safe set of values stored as text.
 */
public enum InvoiceStatus {
    DRAFT,
    ISSUED,
    PARTIALLY_PAID,
    PAID,
    OVERDUE
}
