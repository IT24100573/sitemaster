package com.sitemaster.file;

import org.springframework.core.io.Resource;

/**
 * A file ready to stream to the client, with the metadata copied out of the
 * entity so it can be used after the transaction has closed.
 *
 * @param contentType  real MIME type detected at upload
 * @param filename     original file name for Content-Disposition
 * @param sizeBytes    size in bytes
 * @param resource     the bytes
 */
public record FileDownload(String contentType, String filename, long sizeBytes, Resource resource) {
}
