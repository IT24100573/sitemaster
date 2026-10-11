package com.sitemaster.file;

import java.util.Map;
import java.util.Set;

/**
 * Kinds of upload SiteMaster accepts. Each category lists the allowed file
 * extensions and the real MIME types (detected from the bytes by Apache Tika)
 * that each extension may contain. Size limits come from application.yml.
 * <p>
 * OOP concept: Encapsulation - the allow-lists live with the category and are
 * only reachable through {@link #allows(String, String)}.
 */
public enum FileCategory {

    /** Photos and profile pictures: jpg, jpeg, png, webp. */
    IMAGE(Map.of(
            "jpg", Set.of("image/jpeg"),
            "jpeg", Set.of("image/jpeg"),
            "png", Set.of("image/png"),
            "webp", Set.of("image/webp"))),

    /** Documents and receipts: pdf, docx, xlsx, jpg, png. */
    DOCUMENT(Map.of(
            "pdf", Set.of("application/pdf"),
            "docx", Set.of("application/vnd.openxmlformats-officedocument.wordprocessingml.document"),
            "xlsx", Set.of("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"),
            "jpg", Set.of("image/jpeg"),
            "png", Set.of("image/png")));

    private final Map<String, Set<String>> mimeTypesByExtension;

    FileCategory(Map<String, Set<String>> mimeTypesByExtension) {
        this.mimeTypesByExtension = mimeTypesByExtension;
    }

    public boolean allowsExtension(String extension) {
        return mimeTypesByExtension.containsKey(extension);
    }

    /** True when the extension is allowed and the detected content matches it. */
    public boolean allows(String extension, String detectedMimeType) {
        Set<String> mimeTypes = mimeTypesByExtension.get(extension);
        return mimeTypes != null && mimeTypes.contains(detectedMimeType);
    }

    /** Human-readable list such as "pdf, docx, xlsx, jpg, png" for messages. */
    public String extensionList() {
        return String.join(", ", mimeTypesByExtension.keySet().stream().sorted().toList());
    }
}
