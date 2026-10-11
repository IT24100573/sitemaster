package com.sitemaster.materialrequest;

import com.sitemaster.common.enums.RequestStatus;
import com.sitemaster.project.ProjectArchiveCheck;
import org.springframework.stereotype.Component;

import java.util.EnumSet;
import java.util.Optional;

/**
 * Blocks archiving while material requests are still in progress.
 * <p>
 * OOP concept: Polymorphism - an implementation of ProjectArchiveCheck.
 */
@Component
public class MaterialRequestArchiveCheck implements ProjectArchiveCheck {

    private static final EnumSet<RequestStatus> OPEN =
            EnumSet.of(RequestStatus.PENDING, RequestStatus.APPROVED, RequestStatus.ORDERED, RequestStatus.PAID);

    private final MaterialRequestRepository repository;

    public MaterialRequestArchiveCheck(MaterialRequestRepository repository) {
        this.repository = repository;
    }

    @Override
    public Optional<String> blockingReason(Long projectId) {
        return repository.countByProjectIdAndStatusIn(projectId, OPEN) > 0
                ? Optional.of("project.archive.openRequests") : Optional.empty();
    }
}
