package com.sitemaster.materialrequest;

import com.sitemaster.budget.PhaseCostSource;
import com.sitemaster.common.enums.RequestStatus;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.EnumSet;

/**
 * Money committed to a phase by approved material requests (approved, ordered,
 * paid or delivered).
 * <p>
 * OOP concept: Polymorphism - one implementation of PhaseCostSource.
 */
@Component
public class MaterialRequestCostSource implements PhaseCostSource {

    private static final EnumSet<RequestStatus> COMMITTED =
            EnumSet.of(RequestStatus.APPROVED, RequestStatus.ORDERED, RequestStatus.PAID, RequestStatus.DELIVERED);

    private final MaterialRequestRepository repository;

    public MaterialRequestCostSource(MaterialRequestRepository repository) {
        this.repository = repository;
    }

    @Override
    public BigDecimal committedCost(Long phaseId, Long excludeMaterialRequestId) {
        return repository.sumCost(phaseId, COMMITTED, excludeMaterialRequestId);
    }
}
