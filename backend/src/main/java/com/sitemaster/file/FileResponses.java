package com.sitemaster.file;

import org.springframework.core.io.Resource;
import org.springframework.http.CacheControl;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;

import java.nio.charset.StandardCharsets;

/**
 * Builds safe download responses: real content type, the original file name,
 * no MIME sniffing and private, revalidated caching.
 */
public final class FileResponses {

    private FileResponses() {
        // utility class
    }

    /** For images shown in the page (photos, logo). */
    public static ResponseEntity<Resource> inline(FileDownload file) {
        return build(file, ContentDisposition.inline());
    }

    /** For documents the browser should save. */
    public static ResponseEntity<Resource> attachment(FileDownload file) {
        return build(file, ContentDisposition.attachment());
    }

    private static ResponseEntity<Resource> build(FileDownload file, ContentDisposition.Builder disposition) {
        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(file.contentType()))
                .contentLength(file.sizeBytes())
                .cacheControl(CacheControl.noCache().cachePrivate())
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        disposition.filename(file.filename(), StandardCharsets.UTF_8).build().toString())
                .header("X-Content-Type-Options", "nosniff")
                .body(file.resource());
    }
}
