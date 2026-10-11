package com.sitemaster.project;

import java.util.Optional;

/**
 * Something that can stop a project from being archived, e.g. pending material
 * requests (Phase 4) or unpaid payments (Phase 6). Each module contributes its own
 * implementation; ProjectServiceImpl asks all of them.
 * <p>
 * OOP concepts: Abstraction and Polymorphism - the project module does not know
 * which checks exist, only this interface.
 */
public interface ProjectArchiveCheck {

    /**
     * @return a message key explaining why the project cannot be archived, or empty if it can
     */
    Optional<String> blockingReason(Long projectId);
}
