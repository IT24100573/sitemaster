package com.sitemaster.finance;

import com.sitemaster.clientinvoice.ClientInvoice;
import com.sitemaster.clientinvoice.ClientInvoiceLine;
import com.sitemaster.common.enums.InvoiceStatus;
import com.sitemaster.common.enums.PurchaseOrderStatus;
import com.sitemaster.common.enums.RoleName;
import com.sitemaster.common.enums.Urgency;
import com.sitemaster.common.exception.BusinessRuleException;
import com.sitemaster.common.exception.InvalidStatusTransitionException;
import com.sitemaster.materialrequest.MaterialRequest;
import com.sitemaster.material.Material;
import com.sitemaster.project.Project;
import com.sitemaster.purchase.PurchaseOrder;
import com.sitemaster.purchase.PurchaseOrderItem;
import com.sitemaster.purchase.SupplierInvoice;
import com.sitemaster.supplier.Supplier;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import static com.sitemaster.support.TestData.TODAY;
import static com.sitemaster.support.TestData.material;
import static com.sitemaster.support.TestData.phase;
import static com.sitemaster.support.TestData.project;
import static com.sitemaster.support.TestData.user;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/** Client invoices (money in) and purchase orders / supplier invoices (money out). */
class InvoiceWorkflowTest {

    private final Project project = project(1, TODAY.minusDays(60), TODAY.plusDays(200), "10000000.00");

    private ClientInvoice clientInvoice(String taxRate, String... amounts) {
        ClientInvoice invoice = new ClientInvoice("INV-2026-0001", project);
        List<ClientInvoiceLine> lines = new java.util.ArrayList<>();
        for (int i = 0; i < amounts.length; i++) {
            lines.add(new ClientInvoiceLine(invoice, i + 1, "Line " + i, BigDecimal.ONE, new BigDecimal(amounts[i])));
        }
        invoice.update(TODAY.minusDays(40), TODAY.minusDays(10), new BigDecimal(taxRate), null, lines);
        return invoice;
    }

    @Test
    void clientInvoiceTotalsIncludeTax() {
        ClientInvoice invoice = clientInvoice("18", "100000.00", "50000.00");
        assertThat(invoice.getSubtotal()).isEqualByComparingTo("150000.00");
        assertThat(invoice.getTaxAmount()).isEqualByComparingTo("27000.00");
        assertThat(invoice.getTotal()).isEqualByComparingTo("177000.00");
    }

    @Test
    void draftCannotTakePaymentsAndEmptyInvoiceCannotBeIssued() {
        ClientInvoice invoice = clientInvoice("0", "1000.00");
        assertThatThrownBy(() -> invoice.receive(BigDecimal.TEN)).isInstanceOf(InvalidStatusTransitionException.class);
        ClientInvoice empty = clientInvoice("0");
        assertThatThrownBy(() -> empty.issue(LocalDateTime.now())).isInstanceOf(BusinessRuleException.class);
    }

    @Test
    void partialPaymentThenFullPaymentAndOverdueDisplay() {
        ClientInvoice invoice = clientInvoice("0", "15000000.00");
        invoice.issue(LocalDateTime.now());
        invoice.receive(new BigDecimal("10000000.00"));
        assertThat(invoice.getStatus()).isEqualTo(InvoiceStatus.PARTIALLY_PAID);
        assertThat(invoice.displayStatus(TODAY)).isEqualTo(InvoiceStatus.OVERDUE);
        assertThatThrownBy(() -> invoice.receive(new BigDecimal("5000000.01"))).isInstanceOf(BusinessRuleException.class);
        invoice.receive(new BigDecimal("5000000.00"));
        assertThat(invoice.getStatus()).isEqualTo(InvoiceStatus.PAID);
        assertThat(invoice.displayStatus(TODAY)).isEqualTo(InvoiceStatus.PAID);
    }

    @Test
    void issuedInvoiceCannotBeEdited() {
        ClientInvoice invoice = clientInvoice("0", "1000.00");
        invoice.issue(LocalDateTime.now());
        assertThatThrownBy(() -> invoice.update(TODAY, TODAY, BigDecimal.ZERO, null, List.of()))
                .isInstanceOf(InvalidStatusTransitionException.class);
    }

    private PurchaseOrder purchaseOrder() {
        Material cement = material(5, "2450.00");
        MaterialRequest request = new MaterialRequest(project, phase(2, project, TODAY.minusDays(60), TODAY.plusDays(60),
                "5000000.00"), cement, new BigDecimal("100"), Urgency.NORMAL, TODAY.plusDays(5), null,
                user(3, RoleName.SITE_ENGINEER));
        Supplier supplier = new Supplier(user(4, RoleName.SUPPLIER), "Lanka Building Supplies", "PV 104522");
        PurchaseOrder po = new PurchaseOrder("PO-2026-0001", request, supplier, TODAY, null);
        po.addItem(new PurchaseOrderItem(po, cement, new BigDecimal("100"), new BigDecimal("2400.00")));
        return po;
    }

    @Test
    void purchaseOrderFollowsTheSupplierWorkflow() {
        PurchaseOrder po = purchaseOrder();
        assertThat(po.getTotalAmount()).isEqualByComparingTo("240000.00");
        assertThatThrownBy(() -> po.dispatch(TODAY.plusDays(2), TODAY, LocalDateTime.now()))
                .isInstanceOf(InvalidStatusTransitionException.class);
        po.accept(LocalDateTime.now());
        assertThatThrownBy(() -> po.dispatch(TODAY.minusDays(1), TODAY, LocalDateTime.now()))
                .isInstanceOf(BusinessRuleException.class);
        po.dispatch(TODAY.plusDays(2), TODAY, LocalDateTime.now());
        po.markInvoiced();
        po.markPaid(LocalDateTime.now());
        po.markDelivered(LocalDateTime.now());
        assertThat(po.getStatus()).isEqualTo(PurchaseOrderStatus.DELIVERED);
    }

    @Test
    void declineNeedsAReason() {
        PurchaseOrder po = purchaseOrder();
        assertThatThrownBy(() -> po.decline("no", LocalDateTime.now())).isInstanceOf(BusinessRuleException.class);
        po.decline("Out of stock until next month.", LocalDateTime.now());
        assertThat(po.getStatus()).isEqualTo(PurchaseOrderStatus.DECLINED);
    }

    @Test
    void supplierInvoiceCannotExceedOrderAndIsPaidInParts() {
        PurchaseOrder po = purchaseOrder();
        assertThatThrownBy(() -> new SupplierInvoice(po, "X-1", TODAY, new BigDecimal("240000.01"), null))
                .isInstanceOf(BusinessRuleException.class);
        SupplierInvoice invoice = new SupplierInvoice(po, "X-1", TODAY, new BigDecimal("240000.00"), null);
        assertThat(invoice.recordPayment(new BigDecimal("100000.00"))).isFalse();
        assertThat(invoice.getStatus()).isEqualTo(InvoiceStatus.PARTIALLY_PAID);
        assertThatThrownBy(() -> invoice.recordPayment(new BigDecimal("140000.01"))).isInstanceOf(BusinessRuleException.class);
        assertThat(invoice.recordPayment(new BigDecimal("140000.00"))).isTrue();
    }
}
