package com.sitemaster.pettycash.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;

/**
 * Accountant sets up or changes a project's petty cash float.
 * <p>
 * OOP concept: Encapsulation - private fields with explicit accessors.
 */
public class FundRequest {

    @NotNull(message = "{field.required}")
    private Long custodianId;

    @NotNull(message = "{field.required}")
    @DecimalMin(value = "0", inclusive = false, message = "{money.positive}")
    @Digits(integer = 12, fraction = 2, message = "{money.digits}")
    private BigDecimal floatLimit;

    private boolean active = true;

    public Long getCustodianId() {
        return custodianId;
    }

    public void setCustodianId(Long custodianId) {
        this.custodianId = custodianId;
    }

    public BigDecimal getFloatLimit() {
        return floatLimit;
    }

    public void setFloatLimit(BigDecimal floatLimit) {
        this.floatLimit = floatLimit;
    }

    public boolean isActive() {
        return active;
    }

    public void setActive(boolean active) {
        this.active = active;
    }
}
