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
 * Accountant tops up a petty cash float (never above its limit).
 * <p>
 * OOP concept: Encapsulation - private fields with explicit accessors.
 */
public class TopUpRequest {

    @NotNull(message = "{field.required}")
    @DecimalMin(value = "0", inclusive = false, message = "{money.positive}")
    @Digits(integer = 12, fraction = 2, message = "{money.digits}")
    private BigDecimal amount;

    @NotNull(message = "{field.required}")
    @NotFutureDate
    private LocalDate date;

    @NotBlank(message = "{field.required}")
    @Size(min = 3, max = 255, message = "{pettyCash.description.size}")
    private String description;

    public BigDecimal getAmount() {
        return amount;
    }

    public void setAmount(BigDecimal amount) {
        this.amount = amount;
    }

    public LocalDate getDate() {
        return date;
    }

    public void setDate(LocalDate date) {
        this.date = date;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }
}
