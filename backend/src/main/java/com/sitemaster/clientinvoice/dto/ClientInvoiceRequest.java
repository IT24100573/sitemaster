package com.sitemaster.clientinvoice.dto;

import com.sitemaster.validation.ValidDateRange;
import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

/**
 * Create or edit a draft client invoice.
 * <p>
 * OOP concept: Encapsulation - private fields with explicit accessors.
 */
@ValidDateRange(start = "issueDate", end = "dueDate")
public class ClientInvoiceRequest {

    @NotNull(message = "{field.required}")
    private Long projectId;

    @NotNull(message = "{field.required}")
    private LocalDate issueDate;

    @NotNull(message = "{field.required}")
    private LocalDate dueDate;

    @NotNull(message = "{field.required}")
    @DecimalMin(value = "0", message = "{clientInvoice.tax.range}")
    @DecimalMax(value = "30", message = "{clientInvoice.tax.range}")
    @Digits(integer = 2, fraction = 2, message = "{clientInvoice.tax.range}")
    private BigDecimal taxRate;

    @Size(max = 500, message = "{note.size}")
    private String notes;

    @Valid
    @NotEmpty(message = "{clientInvoice.lines.required}")
    @Size(max = 50, message = "{clientInvoice.lines.size}")
    private List<InvoiceLineRequest> lines;

    public Long getProjectId() {
        return projectId;
    }

    public void setProjectId(Long projectId) {
        this.projectId = projectId;
    }

    public LocalDate getIssueDate() {
        return issueDate;
    }

    public void setIssueDate(LocalDate issueDate) {
        this.issueDate = issueDate;
    }

    public LocalDate getDueDate() {
        return dueDate;
    }

    public void setDueDate(LocalDate dueDate) {
        this.dueDate = dueDate;
    }

    public BigDecimal getTaxRate() {
        return taxRate;
    }

    public void setTaxRate(BigDecimal taxRate) {
        this.taxRate = taxRate;
    }

    public String getNotes() {
        return notes;
    }

    public void setNotes(String notes) {
        this.notes = notes;
    }

    public List<InvoiceLineRequest> getLines() {
        return lines;
    }

    public void setLines(List<InvoiceLineRequest> lines) {
        this.lines = lines;
    }
}
