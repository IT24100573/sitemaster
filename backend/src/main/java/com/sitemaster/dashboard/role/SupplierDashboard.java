package com.sitemaster.dashboard.role;

// PATTERN: Factory Method

import com.sitemaster.common.enums.InvoiceStatus;
import com.sitemaster.common.enums.PurchaseOrderStatus;
import com.sitemaster.common.enums.RoleName;
import com.sitemaster.dashboard.DashboardQueries;
import com.sitemaster.dashboard.RoleDashboard;
import com.sitemaster.dashboard.dto.DashboardResponse;
import com.sitemaster.purchase.PurchaseOrder;
import com.sitemaster.security.SiteMasterUserPrincipal;
import com.sitemaster.supplier.Supplier;
import com.sitemaster.supplier.SupplierRepository;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static com.sitemaster.dashboard.DashboardQueries.attention;
import static com.sitemaster.dashboard.DashboardQueries.stat;

/** Factory Method pattern - CONCRETE PRODUCT for a Supplier: their orders and money. */
@Component
public class SupplierDashboard implements RoleDashboard {

    private final DashboardQueries q;
    private final SupplierRepository supplierRepository;
    private final Clock clock;

    public SupplierDashboard(DashboardQueries q, SupplierRepository supplierRepository, Clock clock) {
        this.q = q;
        this.supplierRepository = supplierRepository;
        this.clock = clock;
    }

    @Override
    public RoleName role() {
        return RoleName.SUPPLIER;
    }

    @Override
    @Transactional(readOnly = true)
    public DashboardResponse build(SiteMasterUserPrincipal user) {
        Optional<Supplier> supplier = supplierRepository.findByUserId(user.getUserId());
        if (supplier.isEmpty()) {
            return new DashboardResponse("Supplier portal", "Your account is not linked to a supplier yet.",
                    List.of(), List.of(), List.of());
        }
        Long id = supplier.get().getId();
        LocalDate today = LocalDate.now(clock);
        BigDecimal newOrders = q.count("select count(p) from PurchaseOrder p where p.supplier.id = :id and p.status = :s",
                "id", id, "s", PurchaseOrderStatus.SENT);
        BigDecimal toDispatch = q.count("select count(p) from PurchaseOrder p where p.supplier.id = :id and p.status = :s",
                "id", id, "s", PurchaseOrderStatus.ACCEPTED);
        BigDecimal owed = q.sum("select sum(i.amount - i.paidAmount) from SupplierInvoice i where i.supplier.id = :id "
                + "and i.status in :s", "id", id, "s", List.of(InvoiceStatus.ISSUED, InvoiceStatus.PARTIALLY_PAID));
        BigDecimal paidThisYear = q.sum("select sum(p.amount) from Payment p where p.supplierInvoice.supplier.id = :id "
                + "and p.paymentDate >= :start", "id", id, "start", today.withDayOfYear(1));

        List<DashboardResponse.Item> recent = q.list("select p from PurchaseOrder p where p.supplier.id = :id "
                        + "order by p.createdAt desc", PurchaseOrder.class, 8, "id", id).stream()
                .map(p -> new DashboardResponse.Item(p.getPoNumber(), p.getProject().getName() + ", "
                        + p.getTotalAmount().toPlainString(), p.getStatus().name(), "purchase-orders/" + p.getId()))
                .toList();

        return new DashboardResponse(supplier.get().getName(), "Orders from SiteMaster projects",
                List.of(stat("New orders", newOrders, "number", attention(newOrders), "purchase-orders", "Accept or decline"),
                        stat("To dispatch", toDispatch, "number", attention(toDispatch), "purchase-orders", null),
                        stat("Awaiting payment", owed, "money", "neutral", "invoices", null),
                        stat("Paid this year", paidThisYear, "money", "good", "payments", null)),
                List.of(),
                List.of(new DashboardResponse.ItemList("Recent orders", "No orders yet.", "purchase-orders", recent)));
    }
}
