package com.sitemaster.file;

import org.springframework.data.jpa.repository.JpaRepository;

/** Data access for uploaded file metadata. */
public interface StoredFileRepository extends JpaRepository<StoredFile, Long> {
}
