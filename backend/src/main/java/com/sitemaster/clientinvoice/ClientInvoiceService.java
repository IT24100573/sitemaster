package com.sitemaster.clientinvoice;

import com.sitemaster.clientinvoice.dto.ClientInvoiceRequest;
import com.sitemaster.clientinvoice.dto.ClientInvoiceResponse;
import com.sitemaster.clientinvoice.dto.ReceiptRequest;
import com.sitemaster.common.dto.PageQuery;
import com.sitemaster.common.dto.PageResponse;
import com.sitemaster.common.enums.InvoiceStatus;

/**
 * Client invoices: drafted and issued by the Accountant, receipts recorded
 * against them, printed as PDF.
 * <p>
 * OOP concept: Abstraction - controllers depend on this interface.
 */
public interface ClientInvoiceService {

    PageResponse<ClientInvoiceResponse> search(Long projectId, InvoiceStatus status, PageQuery pageQuery);

    ClientInvoiceResponse get(Long id);

    ClientInvoiceResponse create(ClientInvoiceRequest request);

    ClientInvoiceResponse update(Long id, ClientInvoiceRequest request);

    void deleteDraft(Long id);

    ClientInvoiceResponse issue(Long id);

    ClientInvoiceResponse receive(Long id, ReceiptRequest request);

    byte[] pdf(Long id);
}
