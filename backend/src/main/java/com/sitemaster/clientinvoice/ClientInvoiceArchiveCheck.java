package com.sitemaster.clientinvoice;

import com.sitemaster.common.enums.InvoiceStatus;
import com.sitemaster.project.ProjectArchiveCheck;
import org.springframework.stereotype.Component;

import java.util.EnumSet;
import java.util.Optional;

/**
 * Blocks archiving while client invoices are still owed.
 * <p>
 * OOP concept: Polymorphism - an implementation of ProjectArchiveCheck.
 */
@Component
public class ClientInvoiceArchiveCheck implements ProjectArchiveCheck {

    private final ClientInvoiceRepository repository;

    public ClientInvoiceArchiveCheck(ClientInvoiceRepository repository) {
        this.repository = repository;
    }

    @Override
    public Optional<String> blockingReason(Long projectId) {
        return repository.countByProjectIdAndStatusIn(projectId,
                EnumSet.of(InvoiceStatus.ISSUED, InvoiceStatus.PARTIALLY_PAID)) > 0
                ? Optional.of("project.archive.unpaidInvoices") : Optional.empty();
    }
}
