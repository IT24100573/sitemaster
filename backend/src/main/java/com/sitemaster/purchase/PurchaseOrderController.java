package com.sitemaster.purchase;

import com.sitemaster.common.dto.PageQuery;
import com.sitemaster.common.dto.PageResponse;
import com.sitemaster.common.enums.InvoiceStatus;
import com.sitemaster.common.enums.PurchaseOrderStatus;
import com.sitemaster.file.FileResponses;
import com.sitemaster.purchase.dto.CreatePurchaseOrderRequest;
import com.sitemaster.purchase.dto.PaymentRequest;
import com.sitemaster.purchase.dto.PaymentResponse;
import com.sitemaster.purchase.dto.PurchaseOrderResponse;
import com.sitemaster.purchase.dto.SupplierInvoiceResponse;
import jakarta.validation.Valid;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/** Staff side of purchasing: orders, supplier invoices and payments. */
@RestController
public class PurchaseOrderController {

    private final PurchaseOrderService orderService;
    private final InvoicePaymentService invoiceService;

    public PurchaseOrderController(PurchaseOrderService orderService, InvoicePaymentService invoiceService) {
        this.orderService = orderService;
        this.invoiceService = invoiceService;
    }

    @GetMapping("/api/purchase-orders")
    @PreAuthorize("hasAuthority('PURCHASE_ORDER:VIEW')")
    public PageResponse<PurchaseOrderResponse> list(@RequestParam(required = false) Long projectId,
                                                    @RequestParam(required = false) PurchaseOrderStatus status,
                                                    @Valid @ModelAttribute PageQuery pageQuery) {
        return orderService.search(projectId, status, pageQuery);
    }

    @GetMapping("/api/purchase-orders/{id}")
    @PreAuthorize("hasAuthority('PURCHASE_ORDER:VIEW')")
    public PurchaseOrderResponse get(@PathVariable Long id) {
        return orderService.get(id);
    }

    @PostMapping("/api/material-requests/{requestId}/purchase-order")
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasAuthority('PURCHASE_ORDER:CREATE')")
    public PurchaseOrderResponse create(@PathVariable Long requestId,
                                        @Valid @RequestBody CreatePurchaseOrderRequest request) {
        return orderService.create(requestId, request);
    }

    @GetMapping("/api/supplier-invoices")
    @PreAuthorize("hasAuthority('SUPPLIER_INVOICE:VIEW')")
    public PageResponse<SupplierInvoiceResponse> invoices(@RequestParam(required = false) InvoiceStatus status,
                                                          @RequestParam(required = false) Long supplierId,
                                                          @Valid @ModelAttribute PageQuery pageQuery) {
        return invoiceService.invoices(status, supplierId, pageQuery);
    }

    @GetMapping("/api/supplier-invoices/{id}")
    @PreAuthorize("hasAuthority('SUPPLIER_INVOICE:VIEW')")
    public SupplierInvoiceResponse invoice(@PathVariable Long id) {
        return invoiceService.invoice(id);
    }

    @GetMapping("/api/supplier-invoices/{id}/file")
    @PreAuthorize("hasAuthority('SUPPLIER_INVOICE:VIEW')")
    public ResponseEntity<Resource> invoiceFile(@PathVariable Long id) {
        return FileResponses.attachment(invoiceService.openInvoiceFile(id));
    }

    @PostMapping("/api/supplier-invoices/{id}/payments")
    @PreAuthorize("hasAuthority('PAYMENT:CREATE')")
    public SupplierInvoiceResponse pay(@PathVariable Long id, @Valid @RequestBody PaymentRequest request) {
        return invoiceService.pay(id, request);
    }

    @GetMapping("/api/payments")
    @PreAuthorize("hasAuthority('PAYMENT:VIEW')")
    public PageResponse<PaymentResponse> payments(@Valid @ModelAttribute PageQuery pageQuery) {
        return invoiceService.payments(pageQuery);
    }
}
