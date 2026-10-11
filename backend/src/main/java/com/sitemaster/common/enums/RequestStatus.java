package com.sitemaster.common.enums;

/**
 * Status shared by every request type that extends BaseRequest.
 * Leave requests only use PENDING, APPROVED and REJECTED; each request type
 * declares which values it accepts and the approve/reject logic rejects others.
 * <p>
 * OOP concept: Abstraction - one status vocabulary for a family of request types.
 */
public enum RequestStatus {
    PENDING,
    APPROVED,
    REJECTED,
    ORDERED,
    PAID,
    DELIVERED
}
