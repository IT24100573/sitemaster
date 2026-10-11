package com.sitemaster.audit;

import com.sitemaster.audit.dto.AuditLogFilter;
import com.sitemaster.audit.dto.AuditLogResponse;
import com.sitemaster.common.dto.PageQuery;
import com.sitemaster.common.dto.PageResponse;
import com.sitemaster.common.exception.BusinessRuleException;
import com.sitemaster.common.util.EnumLabels;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Map;

/**
 * Implementation of {@link AuditLogService}: filter by user, module, date range and text.
 * <p>
 * OOP concept: Abstraction - implements AuditLogService.
 */
@Service
@Transactional(readOnly = true)
public class AuditLogServiceImpl implements AuditLogService {

    private static final Map<String, String> SORTS = Map.of(
            "createdAt", "createdAt",
            "actorName", "actorName",
            "module", "module",
            "action", "action");

    private final AuditLogRepository repository;

    public AuditLogServiceImpl(AuditLogRepository repository) {
        this.repository = repository;
    }

    @Override
    public PageResponse<AuditLogResponse> search(AuditLogFilter filter, PageQuery pageQuery) {
        if (filter.getFrom() != null && filter.getTo() != null && filter.getTo().isBefore(filter.getFrom())) {
            throw BusinessRuleException.forField("to", "daterange.invalid");
        }
        Specification<AuditLog> spec = Specification
                .where(AuditLogSpecifications.actor(filter.getUserId()))
                .and(AuditLogSpecifications.module(filter.getModule()))
                .and(AuditLogSpecifications.from(filter.getFrom() == null ? null : filter.getFrom().atStartOfDay()))
                .and(AuditLogSpecifications.before(filter.getTo() == null ? null : filter.getTo().plusDays(1).atStartOfDay()))
                .and(AuditLogSpecifications.text(pageQuery.getSearch()));
        return PageResponse.from(
                repository.findAll(spec, pageQuery.toPageable(SORTS, "createdAt", Sort.Direction.DESC)),
                AuditLogServiceImpl::toResponse);
    }

    private static AuditLogResponse toResponse(AuditLog log) {
        return new AuditLogResponse(log.getId(), log.getCreatedAt(), log.getActorId(), log.getActorName(),
                log.getAction(), EnumLabels.humanize(log.getAction()), log.getModule().name(),
                log.getEntityType(), log.getEntityId(), log.getDescription());
    }
}
