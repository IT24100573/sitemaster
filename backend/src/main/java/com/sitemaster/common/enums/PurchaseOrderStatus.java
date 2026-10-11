package com.sitemaster.common.enums;

/**
 * Lifecycle of a purchase order sent to a supplier.
 * <p>
 * OOP concept: Abstraction - a closed, type-safe set of values stored as text.
 */
public enum PurchaseOrderStatus {
    SENT,
    ACCEPTED,
    DECLINED,
    DISPATCHED,
    INVOICED,
    PAID,
    DELIVERED
}
