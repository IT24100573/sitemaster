package com.sitemaster.expense.dto;

import com.sitemaster.common.enums.ExpenseCategory;
import com.sitemaster.file.FileCategory;
import com.sitemaster.validation.NotFutureDate;
import com.sitemaster.validation.ValidFile;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.multipart.MultipartFile;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * Multipart form for a new expense. The receipt is optional unless the amount
 * is above the receipt threshold in System Settings (checked in the service).
 * <p>
 * OOP concept: Encapsulation - private fields with explicit accessors.
 */
public class ExpenseForm {

    @NotNull(message = "{field.required}")
    private Long phaseId;

    @NotNull(message = "{field.required}")
    private ExpenseCategory category;

    @NotNull(message = "{field.required}")
    @DecimalMin(value = "0", inclusive = false, message = "{money.positive}")
    @Digits(integer = 12, fraction = 2, message = "{money.digits}")
    private BigDecimal amount;

    @NotNull(message = "{field.required}")
    @NotFutureDate
    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
    private LocalDate expenseDate;

    @NotBlank(message = "{field.required}")
    @Size(min = 2, max = 100, message = "{expense.payee.size}")
    private String payee;

    @NotBlank(message = "{field.required}")
    @Size(min = 5, max = 500, message = "{expense.description.size}")
    private String description;

    @ValidFile(category = FileCategory.DOCUMENT, required = false)
    private MultipartFile receipt;

    public Long getPhaseId() {
        return phaseId;
    }

    public void setPhaseId(Long phaseId) {
        this.phaseId = phaseId;
    }

    public ExpenseCategory getCategory() {
        return category;
    }

    public void setCategory(ExpenseCategory category) {
        this.category = category;
    }

    public BigDecimal getAmount() {
        return amount;
    }

    public void setAmount(BigDecimal amount) {
        this.amount = amount;
    }

    public LocalDate getExpenseDate() {
        return expenseDate;
    }

    public void setExpenseDate(LocalDate expenseDate) {
        this.expenseDate = expenseDate;
    }

    public String getPayee() {
        return payee;
    }

    public void setPayee(String payee) {
        this.payee = payee;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public MultipartFile getReceipt() {
        return receipt;
    }

    public void setReceipt(MultipartFile receipt) {
        this.receipt = receipt;
    }
}
