package com.sitemaster.purchase;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import java.util.Optional;

/** Data access for supplier invoices. */
public interface SupplierInvoiceRepository
        extends JpaRepository<SupplierInvoice, Long>, JpaSpecificationExecutor<SupplierInvoice> {

    Optional<SupplierInvoice> findByPurchaseOrderId(Long purchaseOrderId);

    boolean existsBySupplierIdAndInvoiceNumberIgnoreCase(Long supplierId, String invoiceNumber);
}
