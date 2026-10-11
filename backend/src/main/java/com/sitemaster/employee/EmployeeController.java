package com.sitemaster.employee;

import com.sitemaster.common.dto.PageQuery;
import com.sitemaster.common.dto.PageResponse;
import com.sitemaster.common.enums.EmploymentType;
import com.sitemaster.employee.dto.EmployeeCreateRequest;
import com.sitemaster.employee.dto.EmployeeListItem;
import com.sitemaster.employee.dto.EmployeeResponse;
import com.sitemaster.employee.dto.EmployeeUpdateRequest;
import com.sitemaster.file.FileCategory;
import com.sitemaster.file.FileResponses;
import com.sitemaster.validation.ValidFile;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

/**
 * Employee records. HR manages them; Admin can view (permission based,
 * see the EMPLOYEE row of the permission matrix).
 */
@RestController
@RequestMapping("/api/employees")
public class EmployeeController {

    private final EmployeeService employeeService;

    public EmployeeController(EmployeeService employeeService) {
        this.employeeService = employeeService;
    }

    @GetMapping
    @PreAuthorize("hasAuthority('EMPLOYEE:VIEW')")
    public PageResponse<EmployeeListItem> list(@RequestParam(required = false) EmploymentType employmentType,
                                               @RequestParam(required = false) @Size(max = 50, message = "{trade.size}") String trade,
                                               @RequestParam(required = false) Boolean active,
                                               @Valid @ModelAttribute PageQuery pageQuery) {
        return employeeService.search(employmentType, trade == null || trade.isBlank() ? null : trade.trim(),
                active, pageQuery);
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAuthority('EMPLOYEE:VIEW')")
    public EmployeeResponse get(@PathVariable Long id) {
        return employeeService.get(id);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasAuthority('EMPLOYEE:CREATE')")
    public EmployeeResponse create(@Valid @RequestBody EmployeeCreateRequest request) {
        return employeeService.create(request);
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAuthority('EMPLOYEE:EDIT')")
    public EmployeeResponse update(@PathVariable Long id, @Valid @RequestBody EmployeeUpdateRequest request) {
        return employeeService.update(id, request);
    }

    @PostMapping(value = "/{id}/documents", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("hasAuthority('EMPLOYEE:EDIT')")
    public EmployeeResponse addDocument(@PathVariable Long id,
                                        @RequestParam("documentType") @NotNull(message = "{field.required}") EmployeeDocumentType documentType,
                                        @RequestParam("title") @NotBlank(message = "{field.required}")
                                        @Size(max = 100, message = "{title.size}") String title,
                                        @RequestPart("file") @ValidFile(category = FileCategory.DOCUMENT) MultipartFile file) {
        return employeeService.addDocument(id, documentType, title.trim(), file);
    }

    @DeleteMapping("/{id}/documents/{documentId}")
    @PreAuthorize("hasAuthority('EMPLOYEE:EDIT')")
    public EmployeeResponse removeDocument(@PathVariable Long id, @PathVariable Long documentId) {
        return employeeService.removeDocument(id, documentId);
    }

    @GetMapping("/{id}/documents/{documentId}/download")
    @PreAuthorize("hasAuthority('EMPLOYEE:VIEW')")
    public ResponseEntity<Resource> download(@PathVariable Long id, @PathVariable Long documentId) {
        return FileResponses.attachment(employeeService.openDocument(id, documentId));
    }
}
