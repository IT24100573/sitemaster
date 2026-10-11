package com.sitemaster.file;

import com.sitemaster.common.config.AppProperties;
import org.apache.tika.Tika;
import org.springframework.stereotype.Service;
import org.springframework.util.unit.DataSize;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.util.Locale;
import java.util.Optional;

/**
 * Single source of the upload rules, used both by the @ValidFile annotation and
 * by the storage service (defence in depth):
 * not empty, within the size limit, allowed extension, and the REAL content
 * type detected by Apache Tika must match the extension.
 * <p>
 * OOP concept: Encapsulation - callers get a message key back and never see Tika.
 */
@Service
public class FileValidationService {

    private final Tika tika = new Tika();
    private final AppProperties properties;

    public FileValidationService(AppProperties properties) {
        this.properties = properties;
    }

    /**
     * Checks a file against a category.
     *
     * @return empty when the file is acceptable, otherwise the message key of the problem
     */
    public Optional<String> findProblem(MultipartFile file, FileCategory category) {
        if (file == null || file.isEmpty() || file.getSize() == 0) {
            return Optional.of("file.empty");
        }
        if (file.getSize() > maxSize(category).toBytes()) {
            return Optional.of(category == FileCategory.IMAGE ? "file.image.tooLarge" : "file.document.tooLarge");
        }
        String extension = extensionOf(file.getOriginalFilename());
        if (!category.allowsExtension(extension)) {
            return Optional.of(category == FileCategory.IMAGE ? "file.image.type" : "file.document.type");
        }
        String detected = detectMimeType(file);
        if (detected == null || !category.allows(extension, detected)) {
            return Optional.of("file.content.mismatch");
        }
        return Optional.empty();
    }

    public DataSize maxSize(FileCategory category) {
        return category == FileCategory.IMAGE
                ? properties.files().imageMaxSize()
                : properties.files().documentMaxSize();
    }

    /** Real MIME type from the file's bytes (the name is only a hint for sub-types such as docx). */
    public String detectMimeType(MultipartFile file) {
        try (InputStream in = file.getInputStream()) {
            return tika.detect(in, file.getOriginalFilename());
        } catch (IOException ex) {
            return null;
        }
    }

    public static String extensionOf(String filename) {
        if (filename == null) {
            return "";
        }
        int dot = filename.lastIndexOf('.');
        return dot < 0 ? "" : filename.substring(dot + 1).toLowerCase(Locale.ROOT);
    }
}
