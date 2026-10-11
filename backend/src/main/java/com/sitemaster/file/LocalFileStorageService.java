package com.sitemaster.file;

import com.sitemaster.common.config.AppProperties;
import com.sitemaster.common.exception.BusinessRuleException;
import com.sitemaster.common.exception.ResourceNotFoundException;
import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.FileSystemResource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.UUID;

/**
 * Stores files in the configured upload directory (outside the source tree)
 * with a random UUID name. Files are only served through authorised controller
 * endpoints, never as static resources.
 * <p>
 * OOP concepts: Abstraction / Polymorphism - an implementation of
 * FileStorageService; Encapsulation - path handling is private.
 */
@Service
public class LocalFileStorageService implements FileStorageService {

    private static final Logger log = LoggerFactory.getLogger(LocalFileStorageService.class);
    private static final int MAX_ORIGINAL_NAME = 255;

    private final Path root;
    private final StoredFileRepository repository;
    private final FileValidationService validator;

    public LocalFileStorageService(AppProperties properties, StoredFileRepository repository,
                                   FileValidationService validator) {
        this.root = Paths.get(properties.uploadDir()).toAbsolutePath().normalize();
        this.repository = repository;
        this.validator = validator;
    }

    @PostConstruct
    void createRoot() throws IOException {
        Files.createDirectories(root);
        log.info("Uploads are stored in {}", root);
    }

    @Override
    @Transactional
    public StoredFile store(MultipartFile file, FileCategory category, String field) {
        validator.findProblem(file, category).ifPresent(key -> {
            throw BusinessRuleException.forField(field, key, category.extensionList(),
                    validator.maxSize(category).toMegabytes());
        });

        String extension = FileValidationService.extensionOf(file.getOriginalFilename());
        String storedName = UUID.randomUUID() + "." + extension;
        Path target = resolve(storedName);
        try (InputStream in = file.getInputStream()) {
            Files.copy(in, target, StandardCopyOption.REPLACE_EXISTING);
        } catch (IOException ex) {
            throw new UncheckedIOException("Could not store upload", ex);
        }
        StoredFile stored = new StoredFile(storedName, safeOriginalName(file.getOriginalFilename(), extension),
                validator.detectMimeType(file), file.getSize(), category);
        return repository.save(stored);
    }

    @Override
    public FileDownload open(StoredFile file) {
        Path path = resolve(file.getStoredName());
        if (!Files.isReadable(path)) {
            throw new ResourceNotFoundException("resource.notFound", "File");
        }
        return new FileDownload(file.getContentType(), file.getOriginalName(), file.getSizeBytes(),
                new FileSystemResource(path));
    }

    @Override
    @Transactional
    public void delete(StoredFile file) {
        repository.delete(file);
        try {
            Files.deleteIfExists(resolve(file.getStoredName()));
        } catch (IOException ex) {
            log.warn("Could not delete file {}: {}", file.getStoredName(), ex.getMessage());
        }
    }

    /** Resolves a stored name inside the root and refuses anything that escapes it. */
    private Path resolve(String storedName) {
        Path path = root.resolve(storedName).normalize();
        if (!path.startsWith(root)) {
            throw new ResourceNotFoundException("resource.notFound", "File");
        }
        return path;
    }

    /** Keeps only the last path segment and safe characters, for display in downloads. */
    private static String safeOriginalName(String original, String extension) {
        String name = original == null ? "" : original.replace('\\', '/');
        name = name.substring(name.lastIndexOf('/') + 1).replaceAll("[^A-Za-z0-9._ -]", "_").trim();
        if (name.isEmpty()) {
            name = "file." + extension;
        }
        return name.length() > MAX_ORIGINAL_NAME ? name.substring(name.length() - MAX_ORIGINAL_NAME) : name;
    }
}
