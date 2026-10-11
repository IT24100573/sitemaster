package com.sitemaster.finance;

import com.sitemaster.common.enums.RequestStatus;
import jakarta.persistence.EntityManager;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.EnumSet;

/**
 * Money totals per project, read with JPQL aggregates so finance figures are
 * always calculated from the source records, never stored twice.
 * <p>
 * OOP concept: Encapsulation - the queries live in one place.
 */
@Component
public class FinanceQueries {

    private final EntityManager entityManager;

    public FinanceQueries(EntityManager entityManager) {
        this.entityManager = entityManager;
    }

    public BigDecimal supplierPayments(Long projectId) {
        return sum("select coalesce(sum(p.amount), 0) from Payment p "
                + "where p.supplierInvoice.purchaseOrder.project.id = :id", projectId);
    }

    public BigDecimal paidExpenses(Long projectId) {
        return sum("select coalesce(sum(e.amount), 0) from ExpenseRequest e where e.project.id = :id "
                + "and e.status = com.sitemaster.common.enums.RequestStatus.PAID", projectId);
    }

    /** Approved material requests not yet paid, plus approved expenses not yet paid. */
    public BigDecimal committed(Long projectId) {
        BigDecimal materials = entityManager.createQuery("select coalesce(sum(r.estimatedCost), 0) from MaterialRequest r "
                        + "where r.project.id = :id and r.status in :statuses", BigDecimal.class)
                .setParameter("id", projectId)
                .setParameter("statuses", EnumSet.of(RequestStatus.APPROVED, RequestStatus.ORDERED))
                .getSingleResult();
        BigDecimal expenses = sum("select coalesce(sum(e.amount), 0) from ExpenseRequest e where e.project.id = :id "
                + "and e.status = com.sitemaster.common.enums.RequestStatus.APPROVED", projectId);
        return materials.add(expenses);
    }

    public BigDecimal invoiced(Long projectId) {
        return sum("select coalesce(sum(i.total), 0) from ClientInvoice i where i.project.id = :id "
                + "and i.status <> com.sitemaster.common.enums.InvoiceStatus.DRAFT", projectId);
    }

    public BigDecimal received(Long projectId) {
        return sum("select coalesce(sum(i.paidAmount), 0) from ClientInvoice i where i.project.id = :id", projectId);
    }

    private BigDecimal sum(String jpql, Long projectId) {
        return entityManager.createQuery(jpql, BigDecimal.class).setParameter("id", projectId).getSingleResult();
    }
}
