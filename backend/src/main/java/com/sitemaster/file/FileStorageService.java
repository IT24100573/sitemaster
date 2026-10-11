package com.sitemaster.file;

import org.springframework.web.multipart.MultipartFile;

/**
 * Stores and retrieves uploaded files.
 * <p>
 * OOP concept: Abstraction - callers do not know whether files go to the local
 * disk (LocalFileStorageService) or anywhere else.
 */
public interface FileStorageService {

    /**
     * Validates and stores a file.
     *
     * @param field name of the request field, used to attach the error to the form
     * @throws com.sitemaster.common.exception.BusinessRuleException when the file breaks an upload rule
     */
    StoredFile store(MultipartFile file, FileCategory category, String field);

    /** Opens a stored file for download. */
    FileDownload open(StoredFile file);

    /** Removes the bytes and the metadata row. */
    void delete(StoredFile file);
}
