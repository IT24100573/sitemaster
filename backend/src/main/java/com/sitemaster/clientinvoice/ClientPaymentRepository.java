package com.sitemaster.clientinvoice;

import com.sitemaster.common.enums.PaymentMethod;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

/** Data access for client receipts. */
public interface ClientPaymentRepository extends JpaRepository<ClientPayment, Long> {

    List<ClientPayment> findByInvoiceIdOrderByPaymentDateAsc(Long invoiceId);

    boolean existsByMethodAndReferenceNoIgnoreCase(PaymentMethod method, String referenceNo);
}
