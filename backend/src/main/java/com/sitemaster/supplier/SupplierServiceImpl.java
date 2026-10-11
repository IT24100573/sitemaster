package com.sitemaster.supplier;

import com.sitemaster.common.dto.PageQuery;
import com.sitemaster.common.dto.PageResponse;
import com.sitemaster.common.enums.RoleName;
import com.sitemaster.common.exception.ResourceNotFoundException;
import com.sitemaster.common.exception.UnauthorizedActionException;
import com.sitemaster.security.CurrentUserProvider;
import com.sitemaster.security.SiteMasterUserPrincipal;
import com.sitemaster.supplier.dto.SupplierResponse;
import com.sitemaster.workflow.WorkflowEvent;
import com.sitemaster.workflow.WorkflowEventPublisher;
import com.sitemaster.workflow.WorkflowEventType;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Locale;
import java.util.Map;
import java.util.Objects;

/**
 * Implementation of {@link SupplierService}. Deactivating a supplier also
 * deactivates its sign-in account.
 * <p>
 * OOP concept: Abstraction - implements SupplierService.
 */
@Service
public class SupplierServiceImpl implements SupplierService {

    private static final Map<String, String> SORTS = Map.of("name", "name", "createdAt", "createdAt");

    private final SupplierRepository supplierRepository;
    private final CurrentUserProvider currentUser;
    private final WorkflowEventPublisher eventPublisher;

    public SupplierServiceImpl(SupplierRepository supplierRepository, CurrentUserProvider currentUser,
                               WorkflowEventPublisher eventPublisher) {
        this.supplierRepository = supplierRepository;
        this.currentUser = currentUser;
        this.eventPublisher = eventPublisher;
    }

    @Override
    @Transactional(readOnly = true)
    public PageResponse<SupplierResponse> search(Boolean active, String category, PageQuery pageQuery) {
        SiteMasterUserPrincipal me = currentUser.requirePrincipal();
        String search = pageQuery.getSearch();
        Specification<Supplier> spec = (root, query, cb) -> me.getRole() == RoleName.SUPPLIER
                ? cb.equal(root.get("user").get("id"), me.getUserId()) : null;
        spec = spec.and((root, query, cb) -> active == null ? null : cb.equal(root.get("active"), active))
                .and((root, query, cb) -> category == null ? null
                        : cb.like(cb.lower(root.get("materialCategories")), "%" + category.toLowerCase(Locale.ROOT) + "%"))
                .and((root, query, cb) -> {
                    if (search == null) {
                        return null;
                    }
                    String like = "%" + search.toLowerCase(Locale.ROOT) + "%";
                    return cb.or(cb.like(cb.lower(root.get("name")), like), cb.like(cb.lower(root.get("email")), like));
                });
        return PageResponse.from(supplierRepository.findAll(spec, pageQuery.toPageable(SORTS, "name", Sort.Direction.ASC)),
                SupplierMapper::toResponse);
    }

    @Override
    @Transactional(readOnly = true)
    public SupplierResponse get(Long id) {
        Supplier supplier = find(id);
        SiteMasterUserPrincipal me = currentUser.requirePrincipal();
        if (me.getRole() == RoleName.SUPPLIER
                && (supplier.getUser() == null || !Objects.equals(supplier.getUser().getId(), me.getUserId()))) {
            throw new ResourceNotFoundException("resource.notFound", "Supplier");
        }
        return SupplierMapper.toResponse(supplier);
    }

    @Override
    @Transactional
    public SupplierResponse setActive(Long id, boolean active) {
        if (currentUser.requirePrincipal().getRole() == RoleName.SUPPLIER) {
            throw new UnauthorizedActionException("error.forbidden");
        }
        Supplier supplier = find(id);
        if (active) {
            supplier.activate();
            if (supplier.getUser() != null) {
                supplier.getUser().activate();
            }
        } else {
            supplier.deactivate();
            if (supplier.getUser() != null) {
                supplier.getUser().deactivate();
            }
        }
        eventPublisher.publish(WorkflowEvent.byCurrentUser(WorkflowEventType.SUPPLIER_STATUS_CHANGED)
                .entityId(id).args(supplier.getName(), active ? "active" : "inactive").build());
        return SupplierMapper.toResponse(supplier);
    }

    private Supplier find(Long id) {
        return supplierRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("resource.notFound", "Supplier"));
    }
}
