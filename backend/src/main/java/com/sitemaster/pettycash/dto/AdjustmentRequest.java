package com.sitemaster.pettycash.dto;

import com.sitemaster.validation.NotFutureDate;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * Cash count: sets the balance to what was counted, with a reason.
 * <p>
 * OOP concept: Encapsulation - private fields with explicit accessors.
 */
public class AdjustmentRequest {

    @NotNull(message = "{field.required}")
    @DecimalMin(value = "0", message = "{money.min}")
    @Digits(integer = 12, fraction = 2, message = "{money.digits}")
    private BigDecimal countedBalance;

    @NotNull(message = "{field.required}")
    @NotFutureDate
    private LocalDate date;

    @NotBlank(message = "{field.required}")
    @Size(min = 5, max = 255, message = "{reason.size}")
    private String reason;

    public BigDecimal getCountedBalance() {
        return countedBalance;
    }

    public void setCountedBalance(BigDecimal countedBalance) {
        this.countedBalance = countedBalance;
    }

    public LocalDate getDate() {
        return date;
    }

    public void setDate(LocalDate date) {
        this.date = date;
    }

    public String getReason() {
        return reason;
    }

    public void setReason(String reason) {
        this.reason = reason;
    }
}
