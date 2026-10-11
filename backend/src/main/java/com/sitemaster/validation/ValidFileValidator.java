package com.sitemaster.validation;

import com.sitemaster.file.FileCategory;
import com.sitemaster.file.FileValidationService;
import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;
import org.hibernate.validator.constraintvalidation.HibernateConstraintValidatorContext;
import org.springframework.web.multipart.MultipartFile;

/**
 * Checks {@link ValidFile} by delegating to FileValidationService, so the
 * annotation and the storage service apply exactly the same rules.
 * <p>
 * OOP concept: Polymorphism - a ConstraintValidator implementation.
 */
public class ValidFileValidator implements ConstraintValidator<ValidFile, MultipartFile> {

    private final FileValidationService fileValidationService;
    private FileCategory category;
    private boolean required;

    public ValidFileValidator(FileValidationService fileValidationService) {
        this.fileValidationService = fileValidationService;
    }

    @Override
    public void initialize(ValidFile annotation) {
        this.category = annotation.category();
        this.required = annotation.required();
    }

    @Override
    public boolean isValid(MultipartFile value, ConstraintValidatorContext context) {
        if (!required && (value == null || value.isEmpty())) {
            return true;
        }
        return fileValidationService.findProblem(value, category)
                .map(key -> {
                    HibernateConstraintValidatorContext hibernateContext =
                            context.unwrap(HibernateConstraintValidatorContext.class);
                    hibernateContext.disableDefaultConstraintViolation();
                    hibernateContext
                            .addMessageParameter("types", category.extensionList())
                            .addMessageParameter("maxMb", fileValidationService.maxSize(category).toMegabytes())
                            .buildConstraintViolationWithTemplate("{" + key + ".field}")
                            .addConstraintViolation();
                    return false;
                })
                .orElse(true);
    }
}
