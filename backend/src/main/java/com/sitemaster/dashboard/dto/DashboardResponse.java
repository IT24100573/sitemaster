package com.sitemaster.dashboard.dto;

import java.math.BigDecimal;
import java.util.List;

/**
 * A role dashboard: headline figures, charts and short work lists. Links are
 * paths relative to the role's area (e.g. "approvals", "expenses/12").
 */
public record DashboardResponse(String title, String subtitle, List<Stat> stats, List<Chart> charts,
                                List<ItemList> lists) {

    public DashboardResponse {
        stats = List.copyOf(stats);
        charts = List.copyOf(charts);
        lists = List.copyOf(lists);
    }

    /**
     * One headline figure.
     *
     * @param format number, money or percent
     * @param tone   neutral, good, warning or danger
     */
    public record Stat(String label, BigDecimal value, String format, String tone, String link, String hint) {
    }

    /**
     * A chart; each point has one value per series.
     *
     * @param type bar, line or pie
     */
    public record Chart(String title, String type, String format, List<String> series, List<Point> points) {

        public Chart {
            series = List.copyOf(series);
            points = List.copyOf(points);
        }
    }

    /** One x-axis label (or pie slice) with its values. */
    public record Point(String label, List<BigDecimal> values) {

        public Point {
            values = List.copyOf(values);
        }
    }

    /** A short list of things needing attention. */
    public record ItemList(String title, String emptyText, String link, List<Item> items) {

        public ItemList {
            items = List.copyOf(items);
        }
    }

    /** One list entry. */
    public record Item(String title, String subtitle, String status, String link) {
    }
}
