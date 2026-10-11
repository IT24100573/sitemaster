package com.sitemaster.report.generator;

import com.sitemaster.project.Project;
import com.sitemaster.project.ProjectAccess;
import com.sitemaster.report.ReportCriteria;
import com.sitemaster.report.ReportTable;
import jakarta.persistence.EntityManager;
import jakarta.persistence.TypedQuery;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.Root;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.function.Function;

/** Helpers shared by report generators: visible project ids, subtitles, totals. */
@Component
public class ReportSupport {

    /** Reports never return more rows than this. */
    public static final int MAX_ROWS = 5000;

    private static final DateTimeFormatter DATE = DateTimeFormatter.ofPattern("d MMM yyyy", Locale.ENGLISH);

    private final EntityManager entityManager;
    private final ProjectAccess access;

    public ReportSupport(EntityManager entityManager, ProjectAccess access) {
        this.entityManager = entityManager;
        this.access = access;
    }

    public EntityManager em() {
        return entityManager;
    }

    /**
     * Ids of the projects the report may cover: the chosen project (checked), or
     * every visible project, or null meaning "all projects" for Admin/Accountant/HR.
     */
    public List<Long> projectScope(ReportCriteria criteria) {
        if (criteria.projectId() != null) {
            access.requireVisible(criteria.projectId());
            return List.of(criteria.projectId());
        }
        Specification<Project> visible = access.visibleProjects();
        if (visible == null) {
            return null;
        }
        CriteriaBuilder cb = entityManager.getCriteriaBuilder();
        CriteriaQuery<Long> query = cb.createQuery(Long.class);
        Root<Project> root = query.from(Project.class);
        query.select(root.get("id")).where(visible.toPredicate(root, query, cb));
        List<Long> ids = entityManager.createQuery(query).getResultList();
        return ids.isEmpty() ? List.of(-1L) : ids;
    }

    public <T> List<T> limited(TypedQuery<T> query) {
        return query.setMaxResults(MAX_ROWS).getResultList();
    }

    public String period(ReportCriteria criteria) {
        return "From " + criteria.from().format(DATE) + " to " + criteria.to().format(DATE);
    }

    public static <T> BigDecimal sum(List<T> items, Function<T, BigDecimal> value) {
        return items.stream().map(value).reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    public static List<Object> row(Object... values) {
        List<Object> row = new ArrayList<>(values.length);
        Collections.addAll(row, values);
        return row;
    }

    public static ReportTable.Column col(String header, ReportTable.Kind kind) {
        return new ReportTable.Column(header, kind);
    }
}
