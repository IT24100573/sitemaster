package com.sitemaster.purchase;

import com.sitemaster.expense.ExpenseRequest;
import com.sitemaster.purchase.dto.PaymentResponse;
import com.sitemaster.purchase.dto.PurchaseOrderResponse;
import com.sitemaster.purchase.dto.SupplierInvoiceResponse;

import java.util.List;

/**
 * Maps purchasing entities to DTOs.
 * <p>
 * OOP concept: Encapsulation - entities never leave the service layer.
 */
public final class PurchasingMapper {

    private PurchasingMapper() {
        // utility class
    }

    public static PurchaseOrderResponse toResponse(PurchaseOrder p, SupplierInvoice invoice) {
        return new PurchaseOrderResponse(p.getId(), p.getPoNumber(), p.getMaterialRequest().getId(),
                p.getProject().getId(), p.getProject().getName(), p.getProject().getLocation(),
                p.getSupplier().getId(), p.getSupplier().getName(), p.getStatus().name(), p.getOrderDate(),
                p.getTotalAmount(), p.getNotes(), p.getRespondedAt(), p.getDeclineReason(), p.getDispatchedAt(),
                p.getExpectedDeliveryDate(), p.getPaidAt(), p.getDeliveredAt(),
                p.getItems().stream().map(i -> new PurchaseOrderResponse.Item(i.getMaterial().getName(),
                        i.getMaterial().getUnit(), i.getQuantity(), i.getUnitPrice(), i.getLineTotal())).toList(),
                invoice == null ? null : new PurchaseOrderResponse.InvoiceSummary(invoice.getId(),
                        invoice.getInvoiceNumber(), invoice.getAmount(), invoice.getPaidAmount(),
                        invoice.getStatus().name()));
    }

    /** Supplier-invoice payments show the supplier; expense payments show the payee and "EXP-id". */
    public static PaymentResponse toResponse(Payment p) {
        SupplierInvoice invoice = p.getSupplierInvoice();
        ExpenseRequest expense = p.getExpenseRequest();
        return new PaymentResponse(p.getId(), invoice == null ? null : invoice.getId(),
                invoice != null ? invoice.getInvoiceNumber() : expense != null ? "EXP-" + expense.getId() : null,
                invoice != null ? invoice.getPurchaseOrder().getPoNumber() : expense != null ? expense.getProject().getName() : null,
                invoice != null ? invoice.getSupplier().getName() : expense != null ? expense.getPayee() : null,
                p.getAmount(), p.getMethod().name(),
                p.getReferenceNo(), p.getPaymentDate(), p.getNotes(), p.getPaidBy().getFullName(), p.getCreatedAt());
    }

    public static SupplierInvoiceResponse toResponse(SupplierInvoice i, List<Payment> payments, boolean payable) {
        PurchaseOrder po = i.getPurchaseOrder();
        return new SupplierInvoiceResponse(i.getId(), po.getId(), po.getPoNumber(), po.getStatus().name(),
                i.getSupplier().getId(), i.getSupplier().getName(), po.getProject().getName(), i.getInvoiceNumber(),
                i.getInvoiceDate(), i.getAmount(), i.getPaidAmount(), i.outstanding(), i.getStatus().name(),
                i.getFile().getOriginalName(), payable,
                payments.stream().map(PurchasingMapper::toResponse).toList(), i.getCreatedAt());
    }
}
