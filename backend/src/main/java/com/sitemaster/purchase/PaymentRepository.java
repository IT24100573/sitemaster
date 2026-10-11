package com.sitemaster.purchase;

import com.sitemaster.common.enums.PaymentMethod;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import java.util.List;
import java.util.Optional;

/** Data access for payments. */
public interface PaymentRepository extends JpaRepository<Payment, Long>, JpaSpecificationExecutor<Payment> {

    boolean existsByMethodAndReferenceNoIgnoreCase(PaymentMethod method, String referenceNo);

    List<Payment> findBySupplierInvoiceIdOrderByPaymentDateAsc(Long invoiceId);

    Optional<Payment> findFirstByExpenseRequestId(Long expenseRequestId);
}
