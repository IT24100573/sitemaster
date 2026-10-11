package com.sitemaster.report;

import com.sitemaster.common.enums.RoleName;
import com.sitemaster.common.exception.BusinessRuleException;
import com.sitemaster.common.exception.UnauthorizedActionException;
import com.sitemaster.report.dto.ReportInfo;
import com.sitemaster.report.export.ReportExporter;
import com.sitemaster.report.export.ReportExporterFactory;
import com.sitemaster.report.generator.ReportGenerator;
import com.sitemaster.security.CurrentUserProvider;
import com.sitemaster.workflow.WorkflowEvent;
import com.sitemaster.workflow.WorkflowEventPublisher;
import com.sitemaster.workflow.WorkflowEventType;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.Arrays;
import java.util.EnumMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Implementation of {@link ReportService}. Checks the role may run the report,
 * validates the period (from &lt;= to, at most 366 days, not in the future),
 * asks the matching {@link ReportGenerator} for the table and the
 * {@link ReportExporterFactory} for an exporter. Every export is audited.
 * <p>
 * OOP concept: Abstraction - implements ReportService; depends on interfaces only.
 */
@Service
public class ReportServiceImpl implements ReportService {

    private static final long MAX_DAYS = 366;

    private final Map<ReportType, ReportGenerator> generators = new EnumMap<>(ReportType.class);
    private final ReportExporterFactory exporterFactory;
    private final CurrentUserProvider currentUser;
    private final WorkflowEventPublisher eventPublisher;
    private final Clock clock;

    public ReportServiceImpl(List<ReportGenerator> generators, ReportExporterFactory exporterFactory,
                             CurrentUserProvider currentUser, WorkflowEventPublisher eventPublisher, Clock clock) {
        generators.forEach(g -> this.generators.put(g.type(), g));
        this.exporterFactory = exporterFactory;
        this.currentUser = currentUser;
        this.eventPublisher = eventPublisher;
        this.clock = clock;
    }

    @Override
    public List<ReportInfo> available() {
        RoleName role = currentUser.requirePrincipal().getRole();
        return Arrays.stream(ReportType.values()).filter(t -> t.allows(role))
                .map(t -> new ReportInfo(t.name(), t.getTitle(), t.getDescription())).toList();
    }

    @Override
    @Transactional(readOnly = true)
    public ReportTable preview(ReportType type, ReportCriteria criteria) {
        return generate(type, criteria);
    }

    @Override
    @Transactional(readOnly = true)
    public ExportedReport export(ReportType type, ReportCriteria criteria, ExportFormat format) {
        ReportTable table = generate(type, criteria);
        ReportExporter exporter = exporterFactory.create(format);
        byte[] content = exporter.export(table);
        String fileName = type.name().toLowerCase(Locale.ROOT).replace('_', '-') + "-" + criteria.from() + "-to-"
                + criteria.to() + "." + exporter.extension();
        eventPublisher.publish(WorkflowEvent.byCurrentUser(WorkflowEventType.REPORT_EXPORTED)
                .args(type.getTitle(), format.name(), criteria.from(), criteria.to()).build());
        return new ExportedReport(fileName, exporter.contentType(), content);
    }

    private ReportTable generate(ReportType type, ReportCriteria criteria) {
        if (!type.allows(currentUser.requirePrincipal().getRole())) {
            throw new UnauthorizedActionException("report.denied");
        }
        LocalDate today = LocalDate.now(clock);
        if (criteria.to().isBefore(criteria.from())) {
            throw BusinessRuleException.forField("to", "daterange.invalid");
        }
        if (criteria.from().isAfter(today)) {
            throw BusinessRuleException.forField("from", "report.from.future");
        }
        if (ChronoUnit.DAYS.between(criteria.from(), criteria.to()) >= MAX_DAYS) {
            throw BusinessRuleException.forField("to", "report.range.max", MAX_DAYS);
        }
        ReportGenerator generator = generators.get(type);
        if (generator == null) {
            throw new IllegalStateException("No generator for " + type);
        }
        return generator.generate(criteria);
    }
}
