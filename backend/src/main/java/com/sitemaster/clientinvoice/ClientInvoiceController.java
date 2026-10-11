package com.sitemaster.clientinvoice;

import com.sitemaster.clientinvoice.dto.ClientInvoiceRequest;
import com.sitemaster.clientinvoice.dto.ClientInvoiceResponse;
import com.sitemaster.clientinvoice.dto.ReceiptRequest;
import com.sitemaster.common.dto.MessageResponse;
import com.sitemaster.common.dto.PageQuery;
import com.sitemaster.common.dto.PageResponse;
import com.sitemaster.common.enums.InvoiceStatus;
import jakarta.validation.Valid;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/** Client invoices and receipts. */
@RestController
@RequestMapping("/api/client-invoices")
public class ClientInvoiceController {

    private final ClientInvoiceService service;

    public ClientInvoiceController(ClientInvoiceService service) {
        this.service = service;
    }

    @GetMapping
    @PreAuthorize("hasAuthority('INVOICE:VIEW')")
    public PageResponse<ClientInvoiceResponse> list(@RequestParam(required = false) Long projectId,
                                                    @RequestParam(required = false) InvoiceStatus status,
                                                    @Valid @ModelAttribute PageQuery pageQuery) {
        return service.search(projectId, status, pageQuery);
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAuthority('INVOICE:VIEW')")
    public ClientInvoiceResponse get(@PathVariable Long id) {
        return service.get(id);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasAuthority('INVOICE:CREATE')")
    public ClientInvoiceResponse create(@Valid @RequestBody ClientInvoiceRequest request) {
        return service.create(request);
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAuthority('INVOICE:EDIT')")
    public ClientInvoiceResponse update(@PathVariable Long id, @Valid @RequestBody ClientInvoiceRequest request) {
        return service.update(id, request);
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAuthority('INVOICE:EDIT')")
    public MessageResponse delete(@PathVariable Long id) {
        service.deleteDraft(id);
        return new MessageResponse("Draft invoice deleted.");
    }

    @PostMapping("/{id}/issue")
    @PreAuthorize("hasAuthority('INVOICE:EDIT')")
    public ClientInvoiceResponse issue(@PathVariable Long id) {
        return service.issue(id);
    }

    @PostMapping("/{id}/receipts")
    @PreAuthorize("hasAuthority('INVOICE:EDIT')")
    public ClientInvoiceResponse receive(@PathVariable Long id, @Valid @RequestBody ReceiptRequest request) {
        return service.receive(id, request);
    }

    @GetMapping("/{id}/pdf")
    @PreAuthorize("hasAuthority('INVOICE:VIEW')")
    public ResponseEntity<byte[]> pdf(@PathVariable Long id) {
        ClientInvoiceResponse invoice = service.get(id);
        return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_PDF)
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        ContentDisposition.attachment().filename(invoice.invoiceNumber() + ".pdf").build().toString())
                .body(service.pdf(id));
    }
}
