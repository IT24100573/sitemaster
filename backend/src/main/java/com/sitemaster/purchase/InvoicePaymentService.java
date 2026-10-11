package com.sitemaster.purchase;

import com.sitemaster.common.dto.PageQuery;
import com.sitemaster.common.dto.PageResponse;
import com.sitemaster.common.enums.InvoiceStatus;
import com.sitemaster.file.FileDownload;
import com.sitemaster.purchase.dto.PaymentRequest;
import com.sitemaster.purchase.dto.PaymentResponse;
import com.sitemaster.purchase.dto.SupplierInvoiceResponse;

/**
 * Supplier invoices and the payments that settle them. A SUPPLIER user only
 * ever sees their own invoices and payments.
 * <p>
 * OOP concept: Abstraction - controllers depend on this interface.
 */
public interface InvoicePaymentService {

    PageResponse<SupplierInvoiceResponse> invoices(InvoiceStatus status, Long supplierId, PageQuery pageQuery);

    SupplierInvoiceResponse invoice(Long id);

    FileDownload openInvoiceFile(Long id);

    SupplierInvoiceResponse pay(Long invoiceId, PaymentRequest request);

    PageResponse<PaymentResponse> payments(PageQuery pageQuery);
}
