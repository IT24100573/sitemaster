package com.sitemaster.purchase;

import com.sitemaster.common.dto.PageQuery;
import com.sitemaster.common.dto.PageResponse;
import com.sitemaster.common.enums.PurchaseOrderStatus;
import com.sitemaster.common.enums.RequestStatus;
import com.sitemaster.common.enums.RoleName;
import com.sitemaster.common.exception.BusinessRuleException;
import com.sitemaster.common.exception.DuplicateRecordException;
import com.sitemaster.common.exception.InvalidStatusTransitionException;
import com.sitemaster.common.exception.ResourceNotFoundException;
import com.sitemaster.file.FileCategory;
import com.sitemaster.file.FileStorageService;
import com.sitemaster.file.StoredFile;
import com.sitemaster.materialrequest.MaterialRequest;
import com.sitemaster.materialrequest.MaterialRequestRepository;
import com.sitemaster.project.ProjectAccess;
import com.sitemaster.purchase.dto.CreatePurchaseOrderRequest;
import com.sitemaster.purchase.dto.InvoiceUploadForm;
import com.sitemaster.purchase.dto.PurchaseOrderResponse;
import com.sitemaster.security.CurrentUserProvider;
import com.sitemaster.supplier.CurrentSupplierProvider;
import com.sitemaster.supplier.Supplier;
import com.sitemaster.supplier.SupplierRepository;
import com.sitemaster.user.UserRepository;
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
import java.util.Locale;
import java.util.Map;

/**
 * Implementation of {@link PurchaseOrderService}. Rules:
 * <ul>
 *   <li>a PO can only be raised for an APPROVED material request, to an active supplier;</li>
 *   <li>only the addressed supplier accepts, declines (with a reason) or dispatches;
 *       a declined order sends the request back to APPROVED for re-ordering;</li>
 *   <li>an invoice is only accepted for ACCEPTED or DISPATCHED orders and never above
 *       the PO total; invoice numbers are unique per supplier.</li>
 * </ul>
 * Each hand-off notifies the next person (supplier, accountants, site engineer).
 * <p>
 * OOP concept: Abstraction - implements PurchaseOrderService.
 */
@Service
public class PurchaseOrderServiceImpl implements PurchaseOrderService {

    private static final String PO_FORMAT = "PO-%d-%04d";
    private static final Map<String, String> SORTS = Map.of(
            "orderDate", "orderDate", "poNumber", "poNumber", "status", "status", "totalAmount", "totalAmount",
            "createdAt", "createdAt");

    private final PurchaseOrderRepository orderRepository;
    private final SupplierInvoiceRepository invoiceRepository;
    private final MaterialRequestRepository requestRepository;
    private final SupplierRepository supplierRepository;
    private final UserRepository userRepository;
    private final FileStorageService fileStorage;
    private final ProjectAccess access;
    private final CurrentUserProvider currentUser;
    private final CurrentSupplierProvider currentSupplier;
    private final WorkflowEventPublisher eventPublisher;
    private final Clock clock;

    public PurchaseOrderServiceImpl(PurchaseOrderRepository orderRepository, SupplierInvoiceRepository invoiceRepository,
                                    MaterialRequestRepository requestRepository, SupplierRepository supplierRepository,
                                    UserRepository userRepository, FileStorageService fileStorage, ProjectAccess access,
                                    CurrentUserProvider currentUser, CurrentSupplierProvider currentSupplier,
                                    WorkflowEventPublisher eventPublisher, Clock clock) {
        this.orderRepository = orderRepository;
        this.invoiceRepository = invoiceRepository;
        this.requestRepository = requestRepository;
        this.supplierRepository = supplierRepository;
        this.userRepository = userRepository;
        this.fileStorage = fileStorage;
        this.access = access;
        this.currentUser = currentUser;
        this.currentSupplier = currentSupplier;
        this.eventPublisher = eventPublisher;
        this.clock = clock;
    }

    // ----- staff -------------------------------------------------------------------

    @Override
    @Transactional(readOnly = true)
    public PageResponse<PurchaseOrderResponse> search(Long projectId, PurchaseOrderStatus status, PageQuery pageQuery) {
        if (currentUser.requirePrincipal().getRole() == RoleName.SUPPLIER) {
            return supplierOrders(status, pageQuery);
        }
        if (projectId != null) {
            access.requireVisible(projectId);
        }
        Specification<PurchaseOrder> spec = Specification.where(access.<PurchaseOrder>visibleThrough("project"));
        spec = spec.and((root, q, cb) -> projectId == null ? null : cb.equal(root.get("project").get("id"), projectId));
        return page(spec, status, pageQuery);
    }

    @Override
    @Transactional(readOnly = true)
    public PurchaseOrderResponse get(Long id) {
        PurchaseOrder order = find(id);
        if (currentUser.requirePrincipal().getRole() == RoleName.SUPPLIER) {
            return supplierOrder(id);
        }
        access.requireVisible(order.getProject().getId());
        return toResponse(order);
    }

    @Override
    @Transactional
    public PurchaseOrderResponse create(Long materialRequestId, CreatePurchaseOrderRequest body) {
        MaterialRequest request = requestRepository.findById(materialRequestId)
                .orElseThrow(() -> new ResourceNotFoundException("resource.notFound", "Material request"));
        access.requireParticipant(request.getProject().getId());
        if (request.getStatus() != RequestStatus.APPROVED) {
            throw new InvalidStatusTransitionException("purchaseOrder.requestNotApproved");
        }
        Supplier supplier = supplierRepository.findById(body.getSupplierId())
                .filter(Supplier::isActive)
                .orElseThrow(() -> BusinessRuleException.forField("supplierId", "purchaseOrder.supplier.invalid"));

        LocalDate today = LocalDate.now(clock);
        String number = String.format(Locale.ROOT, PO_FORMAT, today.getYear(), orderRepository.nextNumber());
        PurchaseOrder order = new PurchaseOrder(number, request, supplier, today, body.getNotes());
        order.addItem(new PurchaseOrderItem(order, request.getMaterial(), request.getQuantity(), body.getUnitPrice()));
        orderRepository.save(order);
        request.markOrdered();

        WorkflowEvent.Builder event = WorkflowEvent.byCurrentUser(WorkflowEventType.PURCHASE_ORDER_SENT)
                .entityId(order.getId()).args(order.getPoNumber(), supplier.getName(), request.getMaterial().getName())
                .link("purchase-orders/" + order.getId());
        if (supplier.getUser() != null) {
            event.recipients(supplier.getUser().getId());
        }
        eventPublisher.publish(event.build());
        return toResponse(order);
    }

    // ----- supplier portal ---------------------------------------------------------

    @Override
    @Transactional(readOnly = true)
    public PageResponse<PurchaseOrderResponse> supplierOrders(PurchaseOrderStatus status, PageQuery pageQuery) {
        Long supplierId = currentSupplier.requireSupplier().getId();
        Specification<PurchaseOrder> spec = (root, q, cb) -> cb.equal(root.get("supplier").get("id"), supplierId);
        return page(spec, status, pageQuery);
    }

    @Override
    @Transactional(readOnly = true)
    public PurchaseOrderResponse supplierOrder(Long id) {
        return toResponse(findForSupplier(id));
    }

    @Override
    @Transactional
    public PurchaseOrderResponse accept(Long id) {
        PurchaseOrder order = findForSupplier(id);
        order.accept(LocalDateTime.now(clock));
        notifyStaff(WorkflowEventType.PURCHASE_ORDER_ACCEPTED, order, false);
        return toResponse(order);
    }

    @Override
    @Transactional
    public PurchaseOrderResponse decline(Long id, String reason) {
        PurchaseOrder order = findForSupplier(id);
        order.decline(reason, LocalDateTime.now(clock));
        order.getMaterialRequest().reopenForOrdering();
        notifyStaff(WorkflowEventType.PURCHASE_ORDER_DECLINED, order, false, order.getDeclineReason());
        return toResponse(order);
    }

    @Override
    @Transactional
    public PurchaseOrderResponse dispatch(Long id, LocalDate expectedDeliveryDate) {
        PurchaseOrder order = findForSupplier(id);
        order.dispatch(expectedDeliveryDate, LocalDate.now(clock), LocalDateTime.now(clock));
        notifyStaff(WorkflowEventType.PURCHASE_ORDER_DISPATCHED, order, true, expectedDeliveryDate);
        return toResponse(order);
    }

    @Override
    @Transactional
    public PurchaseOrderResponse uploadInvoice(Long id, InvoiceUploadForm form) {
        PurchaseOrder order = findForSupplier(id);
        if (order.getStatus() != PurchaseOrderStatus.ACCEPTED && order.getStatus() != PurchaseOrderStatus.DISPATCHED) {
            throw new InvalidStatusTransitionException("supplierInvoice.poStatus");
        }
        if (invoiceRepository.existsBySupplierIdAndInvoiceNumberIgnoreCase(order.getSupplier().getId(),
                form.getInvoiceNumber())) {
            throw DuplicateRecordException.forField("invoiceNumber", "supplierInvoice.number.duplicate");
        }
        if (form.getAmount().compareTo(order.getTotalAmount()) > 0) {
            throw BusinessRuleException.forField("amount", "supplierInvoice.amount.exceedsPo", order.getTotalAmount());
        }
        StoredFile file = fileStorage.store(form.getFile(), FileCategory.DOCUMENT, "file");
        SupplierInvoice invoice = new SupplierInvoice(order, form.getInvoiceNumber().trim().toUpperCase(Locale.ROOT),
                form.getInvoiceDate(), form.getAmount(), file);
        invoiceRepository.save(invoice);
        order.markInvoiced();

        eventPublisher.publish(WorkflowEvent.byCurrentUser(WorkflowEventType.SUPPLIER_INVOICE_SUBMITTED)
                .entityId(invoice.getId()).args(invoice.getInvoiceNumber(), order.getPoNumber(),
                        order.getSupplier().getName(), invoice.getAmount())
                .recipients(userRepository.findActiveIdsByRole(RoleName.ACCOUNTANT))
                .link("supplier-invoices").build());
        return toResponse(order);
    }

    // ----- helpers -------------------------------------------------------------------

    private void notifyStaff(WorkflowEventType type, PurchaseOrder order, boolean includeRequester, Object... extra) {
        Object[] args = new Object[2 + extra.length];
        args[0] = order.getPoNumber();
        args[1] = order.getSupplier().getName();
        System.arraycopy(extra, 0, args, 2, extra.length);
        WorkflowEvent.Builder event = WorkflowEvent.byCurrentUser(type).entityId(order.getId()).args(args)
                .recipients(userRepository.findActiveIdsByRole(RoleName.ACCOUNTANT))
                .link("purchase-orders/" + order.getId());
        if (includeRequester) {
            event.recipients(order.getMaterialRequest().getRequestedBy().getId());
        }
        eventPublisher.publish(event.build());
    }

    private PageResponse<PurchaseOrderResponse> page(Specification<PurchaseOrder> base, PurchaseOrderStatus status,
                                                     PageQuery pageQuery) {
        String search = pageQuery.getSearch();
        Specification<PurchaseOrder> spec = base
                .and((root, q, cb) -> status == null ? null : cb.equal(root.get("status"), status))
                .and((root, q, cb) -> search == null ? null
                        : cb.like(cb.lower(root.get("poNumber")), "%" + search.toLowerCase(Locale.ROOT) + "%"));
        return PageResponse.from(orderRepository.findAll(spec, pageQuery.toPageable(SORTS, "createdAt", Sort.Direction.DESC)),
                this::toResponse);
    }

    /** Orders of other suppliers are reported as not found. */
    private PurchaseOrder findForSupplier(Long id) {
        Supplier supplier = currentSupplier.requireSupplier();
        PurchaseOrder order = find(id);
        if (!order.belongsTo(supplier.getId())) {
            throw new ResourceNotFoundException("resource.notFound", "Purchase order");
        }
        return order;
    }

    private PurchaseOrder find(Long id) {
        return orderRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("resource.notFound", "Purchase order"));
    }

    private PurchaseOrderResponse toResponse(PurchaseOrder order) {
        return PurchasingMapper.toResponse(order, invoiceRepository.findByPurchaseOrderId(order.getId()).orElse(null));
    }
}
