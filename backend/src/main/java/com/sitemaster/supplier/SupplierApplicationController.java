package com.sitemaster.supplier;

import com.sitemaster.common.dto.PageQuery;
import com.sitemaster.common.dto.PageResponse;
import com.sitemaster.common.enums.ApplicationStatus;
import com.sitemaster.file.FileResponses;
import com.sitemaster.supplier.dto.ReviewRequest;
import com.sitemaster.supplier.dto.SupplierApplicationResponse;
import jakarta.validation.Valid;
import org.springframework.core.io.Resource;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/** Admin review of supplier applications. */
@RestController
@RequestMapping("/api/admin/supplier-applications")
public class SupplierApplicationController {

    private final SupplierApplicationService applicationService;

    public SupplierApplicationController(SupplierApplicationService applicationService) {
        this.applicationService = applicationService;
    }

    @GetMapping
    @PreAuthorize("hasAuthority('SUPPLIER:VIEW')")
    public PageResponse<SupplierApplicationResponse> list(@RequestParam(required = false) ApplicationStatus status,
                                                          @Valid @ModelAttribute PageQuery pageQuery) {
        return applicationService.search(status, pageQuery);
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAuthority('SUPPLIER:VIEW')")
    public SupplierApplicationResponse get(@PathVariable Long id) {
        return applicationService.get(id);
    }

    @GetMapping("/{id}/document")
    @PreAuthorize("hasAuthority('SUPPLIER:VIEW')")
    public ResponseEntity<Resource> document(@PathVariable Long id) {
        return FileResponses.attachment(applicationService.openDocument(id));
    }

    @PostMapping("/{id}/approve")
    @PreAuthorize("hasAuthority('SUPPLIER:APPROVE')")
    public SupplierApplicationResponse approve(@PathVariable Long id, @Valid @RequestBody ReviewRequest request) {
        return applicationService.approve(id, request.getRemarks());
    }

    @PostMapping("/{id}/reject")
    @PreAuthorize("hasAuthority('SUPPLIER:APPROVE')")
    public SupplierApplicationResponse reject(@PathVariable Long id, @Valid @RequestBody ReviewRequest request) {
        return applicationService.reject(id, request.getRemarks());
    }
}
