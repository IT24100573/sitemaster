package com.sitemaster.dashboard;

import com.sitemaster.dashboard.dto.DashboardResponse;
import jakarta.persistence.EntityManager;
import jakarta.persistence.TypedQuery;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.List;

/**
 * Small JPQL helpers used by the role dashboards: counts, sums and lists with
 * named parameters given as name/value pairs.
 */
@Component
public class DashboardQueries {

    private final EntityManager entityManager;

    public DashboardQueries(EntityManager entityManager) {
        this.entityManager = entityManager;
    }

    public BigDecimal count(String jpql, Object... params) {
        return BigDecimal.valueOf(query(jpql, Long.class, params).getSingleResult());
    }

    public BigDecimal sum(String jpql, Object... params) {
        Object value = query(jpql, Object.class, params).getSingleResult();
        return value == null ? BigDecimal.ZERO : new BigDecimal(value.toString());
    }

    public <T> List<T> list(String jpql, Class<T> type, int max, Object... params) {
        return query(jpql, type, params).setMaxResults(max).getResultList();
    }

    public List<Object[]> rows(String jpql, Object... params) {
        return query(jpql, Object[].class, params).getResultList();
    }

    private <T> TypedQuery<T> query(String jpql, Class<T> type, Object... params) {
        TypedQuery<T> query = entityManager.createQuery(jpql, type);
        for (int i = 0; i + 1 < params.length; i += 2) {
            query.setParameter((String) params[i], params[i + 1]);
        }
        return query;
    }

    // ----- builders --------------------------------------------------------------------

    public static DashboardResponse.Stat stat(String label, BigDecimal value, String format, String tone, String link,
                                              String hint) {
        return new DashboardResponse.Stat(label, value, format, tone, link, hint);
    }

    /** Tone that turns to warning when there is something waiting. */
    public static String attention(BigDecimal value) {
        return value.signum() > 0 ? "warning" : "neutral";
    }
}
