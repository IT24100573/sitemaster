package com.sitemaster.clientinvoice;

import com.sitemaster.clientinvoice.dto.ClientInvoiceRequest;
import com.sitemaster.clientinvoice.dto.ClientInvoiceResponse;
import com.sitemaster.clientinvoice.dto.InvoiceLineRequest;
import com.sitemaster.clientinvoice.dto.ReceiptRequest;
import com.sitemaster.common.dto.PageQuery;
import com.sitemaster.common.dto.PageResponse;
import com.sitemaster.common.enums.InvoiceStatus;
import com.sitemaster.common.enums.PaymentMethod;
import com.sitemaster.common.enums.ProjectStatus;
import com.sitemaster.common.exception.BusinessRuleException;
import com.sitemaster.common.exception.DuplicateRecordException;
import com.sitemaster.common.exception.ResourceNotFoundException;
import com.sitemaster.project.Project;
import com.sitemaster.project.ProjectAccess;
import com.sitemaster.security.CurrentUserProvider;
import com.sitemaster.workflow.WorkflowEvent;
import com.sitemaster.workflow.WorkflowEventPublisher;
import com.sitemaster.workflow.WorkflowEventType;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Implementation of {@link ClientInvoiceService}. Rules:
 * <ul>
 *   <li>the client comes from the project; planned or archived projects cannot be billed;</li>
 *   <li>due date on or after issue date; tax 0-30%; 1-50 lines;</li>
 *   <li>only drafts can be edited, deleted or issued; numbers INV-YYYY-0001 are never reused;</li>
 *   <li>receipts only on issued invoices, never above what is owed; references unique.</li>
 * </ul>
 * OOP concept: Abstraction - implements ClientInvoiceService.
 */
@Service
public class ClientInvoiceServiceImpl implements ClientInvoiceService {

    private static final String NUMBER_FORMAT = "INV-%d-%04d";
    private static final Map<String, String> SORTS = Map.of("issueDate", "issueDate", "dueDate", "dueDate",
            "total", "total", "invoiceNumber", "invoiceNumber", "createdAt", "createdAt");

    private final ClientInvoiceRepository invoiceRepository;
    private final ClientPaymentRepository paymentRepository;
    private final InvoicePdfGenerator pdfGenerator;
    private final ProjectAccess access;
    private final CurrentUserProvider currentUser;
    private final WorkflowEventPublisher eventPublisher;
    private final Clock clock;

    public ClientInvoiceServiceImpl(ClientInvoiceRepository invoiceRepository, ClientPaymentRepository paymentRepository,
                                    InvoicePdfGenerator pdfGenerator, ProjectAccess access,
                                    CurrentUserProvider currentUser, WorkflowEventPublisher eventPublisher, Clock clock) {
        this.invoiceRepository = invoiceRepository;
        this.paymentRepository = paymentRepository;
        this.pdfGenerator = pdfGenerator;
        this.access = access;
        this.currentUser = currentUser;
        this.eventPublisher = eventPublisher;
        this.clock = clock;
    }

    @Override
    @Transactional(readOnly = true)
    public PageResponse<ClientInvoiceResponse> search(Long projectId, InvoiceStatus status, PageQuery pageQuery) {
        if (projectId != null) {
            access.requireVisible(projectId);
        }
        LocalDate today = LocalDate.now(clock);
        String search = pageQuery.getSearch();
        Specification<ClientInvoice> spec = Specification.where(access.<ClientInvoice>visibleThrough("project"));
        spec = spec.and((root, q, cb) -> projectId == null ? null : cb.equal(root.get("project").get("id"), projectId))
                .and((root, q, cb) -> {
                    if (status == null) {
                        return null;
                    }
                    if (status == InvoiceStatus.OVERDUE) {
                        return cb.and(root.get("status").in(InvoiceStatus.ISSUED, InvoiceStatus.PARTIALLY_PAID),
                                cb.lessThan(root.get("dueDate"), today));
                    }
                    return cb.equal(root.get("status"), status);
                })
                .and((root, q, cb) -> search == null ? null
                        : cb.like(cb.lower(root.get("invoiceNumber")), "%" + search.toLowerCase(Locale.ROOT) + "%"));
        return PageResponse.from(invoiceRepository.findAll(spec, pageQuery.toPageable(SORTS, "createdAt", Sort.Direction.DESC)),
                this::toResponse);
    }

    @Override
    @Transactional(readOnly = true)
    public ClientInvoiceResponse get(Long id) {
        return toResponse(findVisible(id));
    }

    @Override
    @Transactional
    public ClientInvoiceResponse create(ClientInvoiceRequest request) {
        Project project = access.requireParticipant(request.getProjectId());
        if (project.getStatus() == ProjectStatus.PLANNED) {
            throw BusinessRuleException.forField("projectId", "clientInvoice.project.notStarted");
        }
        LocalDate today = LocalDate.now(clock);
        String number = String.format(Locale.ROOT, NUMBER_FORMAT, today.getYear(), invoiceRepository.nextNumber());
        ClientInvoice invoice = new ClientInvoice(number, project);
        apply(invoice, request);
        invoiceRepository.save(invoice);
        publish(WorkflowEventType.CLIENT_INVOICE_SAVED, invoice);
        return toResponse(invoice);
    }

    @Override
    @Transactional
    public ClientInvoiceResponse update(Long id, ClientInvoiceRequest request) {
        ClientInvoice invoice = findVisible(id);
        if (!invoice.getProject().getId().equals(request.getProjectId())) {
            throw BusinessRuleException.forField("projectId", "clientInvoice.project.fixed");
        }
        access.requireParticipant(invoice.getProject().getId());
        apply(invoice, request);
        publish(WorkflowEventType.CLIENT_INVOICE_SAVED, invoice);
        return toResponse(invoice);
    }

    @Override
    @Transactional
    public void deleteDraft(Long id) {
        ClientInvoice invoice = findVisible(id);
        invoice.ensureDraft();
        publish(WorkflowEventType.CLIENT_INVOICE_DELETED, invoice);
        invoiceRepository.delete(invoice);
    }

    @Override
    @Transactional
    public ClientInvoiceResponse issue(Long id) {
        ClientInvoice invoice = findVisible(id);
        access.requireParticipant(invoice.getProject().getId());
        invoice.issue(LocalDateTime.now(clock));
        eventPublisher.publish(WorkflowEvent.byCurrentUser(WorkflowEventType.CLIENT_INVOICE_ISSUED)
                .entityId(id).args(invoice.getInvoiceNumber(), invoice.getClient().getName(), invoice.getTotal(),
                        invoice.getProject().getName())
                .recipients(invoice.getProject().getManager().getId()).link("projects/" + invoice.getProject().getId())
                .build());
        return toResponse(invoice);
    }

    @Override
    @Transactional
    public ClientInvoiceResponse receive(Long id, ReceiptRequest body) {
        ClientInvoice invoice = findVisible(id);
        PaymentMethod method = body.getMethod();
        String reference = method == PaymentMethod.CASH ? null : body.getReferenceNo();
        if (reference != null && paymentRepository.existsByMethodAndReferenceNoIgnoreCase(method, reference)) {
            throw DuplicateRecordException.forField("referenceNo", "payment.reference.duplicate");
        }
        invoice.receive(body.getAmount());
        paymentRepository.save(new ClientPayment(invoice, body.getAmount(), body.getPaymentDate(), method, reference,
                body.getNotes(), currentUser.requireUser()));
        eventPublisher.publish(WorkflowEvent.byCurrentUser(WorkflowEventType.CLIENT_PAYMENT_RECEIVED)
                .entityId(id).args(invoice.getInvoiceNumber(), invoice.getClient().getName(), body.getAmount(),
                        invoice.outstanding())
                .recipients(invoice.getProject().getManager().getId()).link("projects/" + invoice.getProject().getId())
                .build());
        return toResponse(invoice);
    }

    @Override
    @Transactional(readOnly = true)
    public byte[] pdf(Long id) {
        ClientInvoice invoice = findVisible(id);
        return pdfGenerator.generate(invoice, invoice.displayStatus(LocalDate.now(clock)));
    }

    private void apply(ClientInvoice invoice, ClientInvoiceRequest r) {
        if (invoice.getProject().getStatus() == ProjectStatus.ARCHIVED) {
            throw new BusinessRuleException("project.archived");
        }
        List<ClientInvoiceLine> lines = new ArrayList<>();
        int order = 1;
        for (InvoiceLineRequest line : r.getLines()) {
            lines.add(new ClientInvoiceLine(invoice, order++, line.getDescription(), line.getQuantity(), line.getUnitPrice()));
        }
        invoice.update(r.getIssueDate(), r.getDueDate(), r.getTaxRate(), r.getNotes(), lines);
    }

    private void publish(WorkflowEventType type, ClientInvoice invoice) {
        eventPublisher.publish(WorkflowEvent.byCurrentUser(type).entityId(invoice.getId())
                .args(invoice.getInvoiceNumber(), invoice.getClient().getName(), invoice.getTotal()).build());
    }

    private ClientInvoice findVisible(Long id) {
        ClientInvoice invoice = invoiceRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("resource.notFound", "Invoice"));
        access.requireVisible(invoice.getProject().getId());
        return invoice;
    }

    private ClientInvoiceResponse toResponse(ClientInvoice i) {
        return new ClientInvoiceResponse(i.getId(), i.getInvoiceNumber(), i.getProject().getId(),
                i.getProject().getName(), i.getClient().getId(), i.getClient().getName(), i.getClient().getAddress(),
                i.getIssueDate(), i.getDueDate(), i.displayStatus(LocalDate.now(clock)).name(), i.getTaxRate(),
                i.getSubtotal(), i.getTaxAmount(), i.getTotal(), i.getPaidAmount(), i.outstanding(), i.getNotes(),
                i.getIssuedAt(),
                i.getLines().stream().map(l -> new ClientInvoiceResponse.Line(l.getDescription(), l.getQuantity(),
                        l.getUnitPrice(), l.getLineTotal())).toList(),
                paymentRepository.findByInvoiceIdOrderByPaymentDateAsc(i.getId()).stream()
                        .map(p -> new ClientInvoiceResponse.Receipt(p.getId(), p.getAmount(), p.getPaymentDate(),
                                p.getMethod().name(), p.getReferenceNo(), p.getNotes(), p.getReceivedBy().getFullName()))
                        .toList());
    }
}
