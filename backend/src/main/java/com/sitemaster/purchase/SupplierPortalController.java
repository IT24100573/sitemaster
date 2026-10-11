package com.sitemaster.purchase;

import com.sitemaster.common.dto.PageQuery;
import com.sitemaster.common.dto.PageResponse;
import com.sitemaster.common.enums.InvoiceStatus;
import com.sitemaster.common.enums.PurchaseOrderStatus;
import com.sitemaster.file.FileResponses;
import com.sitemaster.purchase.dto.DeclineRequest;
import com.sitemaster.purchase.dto.DispatchRequest;
import com.sitemaster.purchase.dto.InvoiceUploadForm;
import com.sitemaster.purchase.dto.PaymentResponse;
import com.sitemaster.purchase.dto.PurchaseOrderResponse;
import com.sitemaster.purchase.dto.SupplierInvoiceResponse;
import com.sitemaster.supplier.SupplierProfileService;
import com.sitemaster.supplier.dto.SupplierProfileRequest;
import com.sitemaster.supplier.dto.SupplierResponse;
import jakarta.validation.Valid;
import org.springframework.core.io.Resource;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * Supplier portal (/api/supplier/** is limited to the SUPPLIER role in
 * SecurityConfig). Every call works on the signed-in supplier's own data only.
 */
@RestController
@RequestMapping("/api/supplier")
public class SupplierPortalController {

    private final PurchaseOrderService orderService;
    private final InvoicePaymentService invoiceService;
    private final SupplierProfileService profileService;

    public SupplierPortalController(PurchaseOrderService orderService, InvoicePaymentService invoiceService,
                                    SupplierProfileService profileService) {
        this.orderService = orderService;
        this.invoiceService = invoiceService;
        this.profileService = profileService;
    }

    @GetMapping("/purchase-orders")
    @PreAuthorize("hasAuthority('PURCHASE_ORDER:VIEW')")
    public PageResponse<PurchaseOrderResponse> orders(@RequestParam(required = false) PurchaseOrderStatus status,
                                                      @Valid @ModelAttribute PageQuery pageQuery) {
        return orderService.supplierOrders(status, pageQuery);
    }

    @GetMapping("/purchase-orders/{id}")
    @PreAuthorize("hasAuthority('PURCHASE_ORDER:VIEW')")
    public PurchaseOrderResponse order(@PathVariable Long id) {
        return orderService.supplierOrder(id);
    }

    @PostMapping("/purchase-orders/{id}/accept")
    @PreAuthorize("hasAuthority('PURCHASE_ORDER:EDIT')")
    public PurchaseOrderResponse accept(@PathVariable Long id) {
        return orderService.accept(id);
    }

    @PostMapping("/purchase-orders/{id}/decline")
    @PreAuthorize("hasAuthority('PURCHASE_ORDER:EDIT')")
    public PurchaseOrderResponse decline(@PathVariable Long id, @Valid @RequestBody DeclineRequest request) {
        return orderService.decline(id, request.getReason());
    }

    @PostMapping("/purchase-orders/{id}/dispatch")
    @PreAuthorize("hasAuthority('PURCHASE_ORDER:EDIT')")
    public PurchaseOrderResponse dispatch(@PathVariable Long id, @Valid @RequestBody DispatchRequest request) {
        return orderService.dispatch(id, request.getExpectedDeliveryDate());
    }

    @PostMapping(value = "/purchase-orders/{id}/invoice", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("hasAuthority('SUPPLIER_INVOICE:CREATE')")
    public PurchaseOrderResponse uploadInvoice(@PathVariable Long id, @Valid @ModelAttribute InvoiceUploadForm form) {
        return orderService.uploadInvoice(id, form);
    }

    @GetMapping("/invoices")
    @PreAuthorize("hasAuthority('SUPPLIER_INVOICE:VIEW')")
    public PageResponse<SupplierInvoiceResponse> invoices(@RequestParam(required = false) InvoiceStatus status,
                                                          @Valid @ModelAttribute PageQuery pageQuery) {
        return invoiceService.invoices(status, null, pageQuery);
    }

    @GetMapping("/invoices/{id}/file")
    @PreAuthorize("hasAuthority('SUPPLIER_INVOICE:VIEW')")
    public ResponseEntity<Resource> invoiceFile(@PathVariable Long id) {
        return FileResponses.attachment(invoiceService.openInvoiceFile(id));
    }

    @GetMapping("/payments")
    @PreAuthorize("hasAuthority('PAYMENT:VIEW')")
    public PageResponse<PaymentResponse> payments(@Valid @ModelAttribute PageQuery pageQuery) {
        return invoiceService.payments(pageQuery);
    }

    @GetMapping("/profile")
    @PreAuthorize("hasAuthority('SUPPLIER:VIEW')")
    public SupplierResponse profile() {
        return profileService.mine();
    }

    @PutMapping("/profile")
    @PreAuthorize("hasAuthority('SUPPLIER:EDIT')")
    public SupplierResponse updateProfile(@Valid @RequestBody SupplierProfileRequest request) {
        return profileService.updateMine(request);
    }
}
