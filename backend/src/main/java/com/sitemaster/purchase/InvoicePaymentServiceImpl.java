package com.sitemaster.purchase;

import com.sitemaster.common.dto.PageQuery;
import com.sitemaster.common.dto.PageResponse;
import com.sitemaster.common.enums.InvoiceStatus;
import com.sitemaster.common.enums.PaymentMethod;
import com.sitemaster.common.enums.PurchaseOrderStatus;
import com.sitemaster.common.enums.RequestStatus;
import com.sitemaster.common.enums.RoleName;
import com.sitemaster.common.exception.DuplicateRecordException;
import com.sitemaster.common.exception.InvalidStatusTransitionException;
import com.sitemaster.common.exception.ResourceNotFoundException;
import com.sitemaster.file.FileDownload;
import com.sitemaster.file.FileStorageService;
import com.sitemaster.materialrequest.MaterialRequest;
import com.sitemaster.purchase.dto.PaymentRequest;
import com.sitemaster.purchase.dto.PaymentResponse;
import com.sitemaster.purchase.dto.SupplierInvoiceResponse;
import com.sitemaster.security.CurrentUserProvider;
import com.sitemaster.supplier.CurrentSupplierProvider;
import com.sitemaster.workflow.WorkflowEvent;
import com.sitemaster.workflow.WorkflowEventPublisher;
import com.sitemaster.workflow.WorkflowEventType;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.LocalDateTime;
import java.util.Locale;
import java.util.Map;

/**
 * Implementation of {@link InvoicePaymentService}. Rules for paying:
 * <ul>
 *   <li>only invoiced orders of approved (ordered) material requests can be paid;</li>
 *   <li>the amount cannot exceed what is still owed; an invoice cannot be paid twice;</li>
 *   <li>bank transfers and cheques need a unique reference number.</li>
 * </ul>
 * When the invoice is fully paid the order and request become PAID, the supplier
 * is notified and the site engineer is asked to confirm delivery.
 * <p>
 * OOP concept: Abstraction - implements InvoicePaymentService.
 */
@Service
public class InvoicePaymentServiceImpl implements InvoicePaymentService {

    private static final Map<String, String> INVOICE_SORTS = Map.of(
            "createdAt", "createdAt", "invoiceDate", "invoiceDate", "amount", "amount", "status", "status");
    private static final Map<String, String> PAYMENT_SORTS = Map.of(
            "paymentDate", "paymentDate", "amount", "amount", "createdAt", "createdAt");

    private final SupplierInvoiceRepository invoiceRepository;
    private final PaymentRepository paymentRepository;
    private final FileStorageService fileStorage;
    private final CurrentUserProvider currentUser;
    private final CurrentSupplierProvider currentSupplier;
    private final WorkflowEventPublisher eventPublisher;
    private final Clock clock;

    public InvoicePaymentServiceImpl(SupplierInvoiceRepository invoiceRepository, PaymentRepository paymentRepository,
                                     FileStorageService fileStorage, CurrentUserProvider currentUser,
                                     CurrentSupplierProvider currentSupplier, WorkflowEventPublisher eventPublisher,
                                     Clock clock) {
        this.invoiceRepository = invoiceRepository;
        this.paymentRepository = paymentRepository;
        this.fileStorage = fileStorage;
        this.currentUser = currentUser;
        this.currentSupplier = currentSupplier;
        this.eventPublisher = eventPublisher;
        this.clock = clock;
    }

    @Override
    @Transactional(readOnly = true)
    public PageResponse<SupplierInvoiceResponse> invoices(InvoiceStatus status, Long supplierId, PageQuery pageQuery) {
        Long onlySupplier = isSupplier() ? currentSupplier.requireSupplier().getId() : supplierId;
        String search = pageQuery.getSearch();
        Specification<SupplierInvoice> spec = (root, q, cb) ->
                onlySupplier == null ? null : cb.equal(root.get("supplier").get("id"), onlySupplier);
        spec = spec.and((root, q, cb) -> status == null ? null : cb.equal(root.get("status"), status))
                .and((root, q, cb) -> search == null ? null
                        : cb.like(cb.lower(root.get("invoiceNumber")), "%" + search.toLowerCase(Locale.ROOT) + "%"));
        return PageResponse.from(invoiceRepository.findAll(spec,
                        pageQuery.toPageable(INVOICE_SORTS, "createdAt", Sort.Direction.DESC)),
                this::toResponse);
    }

    @Override
    @Transactional(readOnly = true)
    public SupplierInvoiceResponse invoice(Long id) {
        return toResponse(findVisible(id));
    }

    @Override
    @Transactional(readOnly = true)
    public FileDownload openInvoiceFile(Long id) {
        return fileStorage.open(findVisible(id).getFile());
    }

    @Override
    @Transactional
    public SupplierInvoiceResponse pay(Long invoiceId, PaymentRequest body) {
        SupplierInvoice invoice = find(invoiceId);
        PurchaseOrder order = invoice.getPurchaseOrder();
        MaterialRequest request = order.getMaterialRequest();
        if (order.getStatus() != PurchaseOrderStatus.INVOICED || request.getStatus() != RequestStatus.ORDERED) {
            throw new InvalidStatusTransitionException("payment.notPayable");
        }
        if (body.getMethod() != PaymentMethod.CASH && body.getReferenceNo() != null
                && paymentRepository.existsByMethodAndReferenceNoIgnoreCase(body.getMethod(), body.getReferenceNo())) {
            throw DuplicateRecordException.forField("referenceNo", "payment.reference.duplicate");
        }
        Payment payment = new Payment(body.getAmount(), body.getMethod(), body.getReferenceNo(), body.getPaymentDate(),
                body.getNotes(), currentUser.requireUser());
        boolean fullyPaid = invoice.recordPayment(body.getAmount());
        payment.settle(invoice);
        paymentRepository.save(payment);

        if (fullyPaid) {
            order.markPaid(LocalDateTime.now(clock));
            request.markPaid();
            WorkflowEvent.Builder event = WorkflowEvent.byCurrentUser(WorkflowEventType.SUPPLIER_INVOICE_PAID)
                    .entityId(payment.getId())
                    .args(invoice.getInvoiceNumber(), order.getPoNumber(), invoice.getSupplier().getName(),
                            invoice.getAmount())
                    .recipients(request.getRequestedBy().getId())
                    .link("material-requests/" + request.getId());
            if (invoice.getSupplier().getUser() != null) {
                // The supplier's own link points at their purchase order page.
                eventPublisher.publish(WorkflowEvent.byCurrentUser(WorkflowEventType.SUPPLIER_INVOICE_PAID)
                        .entityId(payment.getId())
                        .args(invoice.getInvoiceNumber(), order.getPoNumber(), invoice.getSupplier().getName(),
                                invoice.getAmount())
                        .recipients(invoice.getSupplier().getUser().getId())
                        .link("purchase-orders/" + order.getId()).build());
            }
            eventPublisher.publish(event.build());
        }
        return toResponse(invoice);
    }

    @Override
    @Transactional(readOnly = true)
    public PageResponse<PaymentResponse> payments(PageQuery pageQuery) {
        Long onlySupplier = isSupplier() ? currentSupplier.requireSupplier().getId() : null;
        Specification<Payment> spec = (root, q, cb) -> onlySupplier == null ? null
                : cb.equal(root.get("supplierInvoice").get("supplier").get("id"), onlySupplier);
        return PageResponse.from(paymentRepository.findAll(spec,
                        pageQuery.toPageable(PAYMENT_SORTS, "paymentDate", Sort.Direction.DESC)),
                PurchasingMapper::toResponse);
    }

    private boolean isSupplier() {
        return currentUser.requirePrincipal().getRole() == RoleName.SUPPLIER;
    }

    private SupplierInvoice findVisible(Long id) {
        SupplierInvoice invoice = find(id);
        if (isSupplier() && !invoice.getSupplier().getId().equals(currentSupplier.requireSupplier().getId())) {
            throw new ResourceNotFoundException("resource.notFound", "Invoice");
        }
        return invoice;
    }

    private SupplierInvoice find(Long id) {
        return invoiceRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("resource.notFound", "Invoice"));
    }

    private SupplierInvoiceResponse toResponse(SupplierInvoice invoice) {
        boolean payable = !isSupplier() && invoice.getStatus() != InvoiceStatus.PAID
                && invoice.getPurchaseOrder().getStatus() == PurchaseOrderStatus.INVOICED;
        return PurchasingMapper.toResponse(invoice,
                paymentRepository.findBySupplierInvoiceIdOrderByPaymentDateAsc(invoice.getId()), payable);
    }
}
