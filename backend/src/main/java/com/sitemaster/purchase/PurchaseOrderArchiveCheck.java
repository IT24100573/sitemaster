package com.sitemaster.purchase;

import com.sitemaster.common.enums.PurchaseOrderStatus;
import com.sitemaster.project.ProjectArchiveCheck;
import org.springframework.stereotype.Component;

import java.util.EnumSet;
import java.util.Optional;

/**
 * Blocks archiving while purchase orders are open or invoiced but unpaid.
 * <p>
 * OOP concept: Polymorphism - an implementation of ProjectArchiveCheck.
 */
@Component
public class PurchaseOrderArchiveCheck implements ProjectArchiveCheck {

    private static final EnumSet<PurchaseOrderStatus> OPEN = EnumSet.of(PurchaseOrderStatus.SENT,
            PurchaseOrderStatus.ACCEPTED, PurchaseOrderStatus.DISPATCHED, PurchaseOrderStatus.INVOICED);

    private final PurchaseOrderRepository repository;

    public PurchaseOrderArchiveCheck(PurchaseOrderRepository repository) {
        this.repository = repository;
    }

    @Override
    public Optional<String> blockingReason(Long projectId) {
        return repository.countByProjectIdAndStatusIn(projectId, OPEN) > 0
                ? Optional.of("project.archive.unpaid") : Optional.empty();
    }
}
