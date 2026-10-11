package com.sitemaster.common.util;

import com.sitemaster.common.exception.BusinessRuleException;

import java.util.Locale;

/** Converts a client-supplied name to an enum constant, reporting a field error if unknown. */
public final class EnumParser {

    private EnumParser() {
        // utility class
    }

    public static <E extends Enum<E>> E parse(Class<E> type, String value, String field) {
        try {
            return Enum.valueOf(type, value.trim().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException | NullPointerException ex) {
            throw BusinessRuleException.forField(field, "status.invalid");
        }
    }
}
