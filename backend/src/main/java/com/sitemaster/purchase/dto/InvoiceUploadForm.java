package com.sitemaster.purchase.dto;

import com.sitemaster.file.FileCategory;
import com.sitemaster.validation.NotFutureDate;
import com.sitemaster.validation.ValidFile;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.multipart.MultipartFile;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * Supplier uploads an invoice (multipart form) against a purchase order.
 * <p>
 * OOP concept: Encapsulation - private fields with explicit accessors.
 */
public class InvoiceUploadForm {

    @NotBlank(message = "{field.required}")
    @Pattern(regexp = "^[A-Za-z0-9/\\-]{1,50}$", message = "{invoiceNumber.pattern}")
    private String invoiceNumber;

    @NotNull(message = "{field.required}")
    @NotFutureDate
    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
    private LocalDate invoiceDate;

    @NotNull(message = "{field.required}")
    @DecimalMin(value = "0", inclusive = false, message = "{money.positive}")
    @Digits(integer = 12, fraction = 2, message = "{money.digits}")
    private BigDecimal amount;

    @ValidFile(category = FileCategory.DOCUMENT)
    private MultipartFile file;

    public String getInvoiceNumber() {
        return invoiceNumber;
    }

    public void setInvoiceNumber(String invoiceNumber) {
        this.invoiceNumber = invoiceNumber;
    }

    public LocalDate getInvoiceDate() {
        return invoiceDate;
    }

    public void setInvoiceDate(LocalDate invoiceDate) {
        this.invoiceDate = invoiceDate;
    }

    public BigDecimal getAmount() {
        return amount;
    }

    public void setAmount(BigDecimal amount) {
        this.amount = amount;
    }

    public MultipartFile getFile() {
        return file;
    }

    public void setFile(MultipartFile file) {
        this.file = file;
    }
}
