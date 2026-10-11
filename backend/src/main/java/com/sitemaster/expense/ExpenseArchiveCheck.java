package com.sitemaster.expense;

import com.sitemaster.common.enums.RequestStatus;
import com.sitemaster.project.ProjectArchiveCheck;
import org.springframework.stereotype.Component;

import java.util.EnumSet;
import java.util.Optional;

/**
 * Blocks archiving while expenses are pending or approved but unpaid.
 * <p>
 * OOP concept: Polymorphism - an implementation of ProjectArchiveCheck.
 */
@Component
public class ExpenseArchiveCheck implements ProjectArchiveCheck {

    private final ExpenseRepository repository;

    public ExpenseArchiveCheck(ExpenseRepository repository) {
        this.repository = repository;
    }

    @Override
    public Optional<String> blockingReason(Long projectId) {
        return repository.countByProjectIdAndStatusIn(projectId,
                EnumSet.of(RequestStatus.PENDING, RequestStatus.APPROVED)) > 0
                ? Optional.of("project.archive.openExpenses") : Optional.empty();
    }
}
