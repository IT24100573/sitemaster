package com.sitemaster.dashboard.role;

// PATTERN: Factory Method

import com.sitemaster.clientinvoice.ClientInvoice;
import com.sitemaster.common.enums.InvoiceStatus;
import com.sitemaster.common.enums.RequestStatus;
import com.sitemaster.common.enums.RoleName;
import com.sitemaster.dashboard.DashboardQueries;
import com.sitemaster.dashboard.RoleDashboard;
import com.sitemaster.dashboard.dto.DashboardResponse;
import com.sitemaster.security.SiteMasterUserPrincipal;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.format.TextStyle;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import static com.sitemaster.dashboard.DashboardQueries.attention;
import static com.sitemaster.dashboard.DashboardQueries.stat;

/** Factory Method pattern - CONCRETE PRODUCT for the Accountant: money in, money out, what to pay. */
@Component
public class AccountantDashboard implements RoleDashboard {

    private static final int MONTHS = 6;
    private static final List<InvoiceStatus> OPEN = List.of(InvoiceStatus.ISSUED, InvoiceStatus.PARTIALLY_PAID);

    private final DashboardQueries q;
    private final Clock clock;

    public AccountantDashboard(DashboardQueries q, Clock clock) {
        this.q = q;
        this.clock = clock;
    }

    @Override
    public RoleName role() {
        return RoleName.ACCOUNTANT;
    }

    @Override
    @Transactional(readOnly = true)
    public DashboardResponse build(SiteMasterUserPrincipal user) {
        LocalDate today = LocalDate.now(clock);
        BigDecimal toOrder = q.count("select count(r) from MaterialRequest r where r.status = :s", "s", RequestStatus.APPROVED);
        BigDecimal supplierOwed = q.sum("select sum(i.amount - i.paidAmount) from SupplierInvoice i where i.status in :s",
                "s", List.of(InvoiceStatus.ISSUED, InvoiceStatus.PARTIALLY_PAID));
        BigDecimal expenses = q.count("select count(e) from ExpenseRequest e where e.status = :s", "s", RequestStatus.APPROVED);
        BigDecimal receivable = q.sum("select sum(i.total - i.paidAmount) from ClientInvoice i where i.status in :s",
                "s", OPEN);

        List<DashboardResponse.Point> cash = new ArrayList<>();
        YearMonth current = YearMonth.from(today);
        for (int i = MONTHS - 1; i >= 0; i--) {
            YearMonth m = current.minusMonths(i);
            BigDecimal out = q.sum("select sum(p.amount) from Payment p where p.paymentDate between :a and :b",
                    "a", m.atDay(1), "b", m.atEndOfMonth());
            BigDecimal in = q.sum("select sum(p.amount) from ClientPayment p where p.paymentDate between :a and :b",
                    "a", m.atDay(1), "b", m.atEndOfMonth());
            cash.add(new DashboardResponse.Point(m.getMonth().getDisplayName(TextStyle.SHORT, Locale.ENGLISH),
                    List.of(in, out)));
        }

        List<DashboardResponse.Item> overdue = q.list("select i from ClientInvoice i where i.status in :s "
                        + "and i.dueDate < :today order by i.dueDate", ClientInvoice.class, 8, "s", OPEN, "today", today)
                .stream().map(i -> new DashboardResponse.Item(i.getInvoiceNumber() + ", " + i.getClient().getName(),
                        "Due " + i.getDueDate() + ", owed " + i.outstanding().toPlainString(), "OVERDUE",
                        "client-invoices/" + i.getId())).toList();

        return new DashboardResponse("Finance", "Payments to make and money to collect",
                List.of(stat("Requests to order", toOrder, "number", attention(toOrder), "material-requests", "Approved, no PO yet"),
                        stat("Owed to suppliers", supplierOwed, "money", attention(supplierOwed), "supplier-invoices", null),
                        stat("Expenses to pay", expenses, "number", attention(expenses), "expenses", null),
                        stat("Owed by clients", receivable, "money", "neutral", "client-invoices", null)),
                List.of(new DashboardResponse.Chart("Cash in and out, last 6 months", "line", "money",
                        List.of("Received", "Paid out"), cash)),
                List.of(new DashboardResponse.ItemList("Overdue client invoices", "No overdue invoices.",
                        "client-invoices", overdue)));
    }
}
