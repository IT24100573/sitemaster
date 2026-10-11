package com.sitemaster.common.util;

import java.util.Locale;

/** Turns constant names into labels: "DAILY_WAGE" -&gt; "Daily wage". */
public final class EnumLabels {

    private EnumLabels() {
        // utility class
    }

    public static String humanize(String constant) {
        if (constant == null || constant.isEmpty()) {
            return "";
        }
        String text = constant.replace('_', ' ').toLowerCase(Locale.ROOT);
        return Character.toUpperCase(text.charAt(0)) + text.substring(1);
    }

    public static String humanize(Enum<?> value) {
        return value == null ? "" : humanize(value.name());
    }
}
