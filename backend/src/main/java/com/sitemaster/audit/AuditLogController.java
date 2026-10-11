package com.sitemaster.audit;

import com.sitemaster.audit.dto.AuditLogFilter;
import com.sitemaster.audit.dto.AuditLogResponse;
import com.sitemaster.common.dto.PageQuery;
import com.sitemaster.common.dto.PageResponse;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** Admin audit log (filter by user, module and date). */
@RestController
@RequestMapping("/api/admin/audit-logs")
public class AuditLogController {

    private final AuditLogService auditLogService;

    public AuditLogController(AuditLogService auditLogService) {
        this.auditLogService = auditLogService;
    }

    @GetMapping
    @PreAuthorize("hasAuthority('AUDIT_LOG:VIEW')")
    public PageResponse<AuditLogResponse> list(@ModelAttribute AuditLogFilter filter,
                                               @Valid @ModelAttribute PageQuery pageQuery) {
        return auditLogService.search(filter, pageQuery);
    }
}
