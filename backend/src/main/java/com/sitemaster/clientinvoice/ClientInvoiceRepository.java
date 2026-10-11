package com.sitemaster.clientinvoice;

import com.sitemaster.common.enums.InvoiceStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.util.Collection;

/** Data access for client invoices. */
public interface ClientInvoiceRepository extends JpaRepository<ClientInvoice, Long>, JpaSpecificationExecutor<ClientInvoice> {

    @Query(value = "select nextval('client_invoice_number_seq')", nativeQuery = true)
    long nextNumber();

    @Query("select coalesce(sum(i.total), 0) from ClientInvoice i where i.project.id = :projectId and i.status in :statuses")
    BigDecimal sumTotal(@Param("projectId") Long projectId, @Param("statuses") Collection<InvoiceStatus> statuses);

    @Query("select coalesce(sum(i.paidAmount), 0) from ClientInvoice i where i.project.id = :projectId")
    BigDecimal sumPaid(@Param("projectId") Long projectId);

    long countByProjectIdAndStatusIn(Long projectId, Collection<InvoiceStatus> statuses);
}
