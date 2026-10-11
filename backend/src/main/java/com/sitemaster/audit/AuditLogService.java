package com.sitemaster.audit;

import com.sitemaster.audit.dto.AuditLogFilter;
import com.sitemaster.audit.dto.AuditLogResponse;
import com.sitemaster.common.dto.PageQuery;
import com.sitemaster.common.dto.PageResponse;

/**
 * Read access to the audit trail (entries are written by AuditLogObserver).
 * <p>
 * OOP concept: Abstraction - controllers depend on this interface.
 */
public interface AuditLogService {

    PageResponse<AuditLogResponse> search(AuditLogFilter filter, PageQuery pageQuery);
}
