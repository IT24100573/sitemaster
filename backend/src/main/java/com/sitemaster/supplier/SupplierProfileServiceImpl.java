package com.sitemaster.supplier;

import com.sitemaster.common.exception.BusinessRuleException;
import com.sitemaster.settings.SystemSettingsManager;
import com.sitemaster.supplier.dto.SupplierProfileRequest;
import com.sitemaster.supplier.dto.SupplierResponse;
import com.sitemaster.workflow.WorkflowEvent;
import com.sitemaster.workflow.WorkflowEventPublisher;
import com.sitemaster.workflow.WorkflowEventType;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.LinkedHashSet;
import java.util.List;

/**
 * Implementation of {@link SupplierProfileService}. Categories must come from
 * System Settings; the contact name and phone are also copied to the sign-in account.
 * <p>
 * OOP concept: Abstraction - implements SupplierProfileService.
 */
@Service
public class SupplierProfileServiceImpl implements SupplierProfileService {

    private final CurrentSupplierProvider currentSupplier;
    private final WorkflowEventPublisher eventPublisher;

    public SupplierProfileServiceImpl(CurrentSupplierProvider currentSupplier, WorkflowEventPublisher eventPublisher) {
        this.currentSupplier = currentSupplier;
        this.eventPublisher = eventPublisher;
    }

    @Override
    @Transactional(readOnly = true)
    public SupplierResponse mine() {
        return SupplierMapper.toResponse(currentSupplier.requireSupplier());
    }

    @Override
    @Transactional
    public SupplierResponse updateMine(SupplierProfileRequest request) {
        Supplier supplier = currentSupplier.requireSupplier();
        List<String> allowed = SystemSettingsManager.getInstance().getSettings().materialCategories();
        List<String> categories = List.copyOf(new LinkedHashSet<>(request.getMaterialCategories()));
        if (!allowed.containsAll(categories)) {
            throw BusinessRuleException.forField("materialCategories", "materialCategories.unknown");
        }
        supplier.updateContact(request.getContactPerson(), supplier.getEmail(), request.getPhone(),
                request.getAddress(), categories);
        if (supplier.getUser() != null) {
            supplier.getUser().updateDetails(request.getContactPerson(), request.getPhone());
        }
        eventPublisher.publish(WorkflowEvent.byCurrentUser(WorkflowEventType.SUPPLIER_PROFILE_UPDATED)
                .entityId(supplier.getId()).args(supplier.getName()).build());
        return SupplierMapper.toResponse(supplier);
    }
}
