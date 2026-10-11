package com.sitemaster.document;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

/** Data access for project documents. */
public interface ProjectDocumentRepository extends JpaRepository<ProjectDocument, Long> {

    List<ProjectDocument> findByProjectIdOrderByTitleAsc(Long projectId);
}
