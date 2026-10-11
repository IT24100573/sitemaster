package com.sitemaster.document;

import org.springframework.data.jpa.repository.JpaRepository;

/** Data access for document versions. */
public interface ProjectDocumentVersionRepository extends JpaRepository<ProjectDocumentVersion, Long> {
}
