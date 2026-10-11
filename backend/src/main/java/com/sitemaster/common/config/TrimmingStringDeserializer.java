package com.sitemaster.common.config;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.deser.std.StdScalarDeserializer;
import com.fasterxml.jackson.databind.deser.std.StringDeserializer;

import java.io.IOException;

/**
 * Trims every incoming JSON string and turns blank strings into null, so
 * "   " fails @NotBlank / @NotNull exactly like a missing value.
 * <p>
 * OOP concept: Inheritance - extends Jackson's StdScalarDeserializer and
 * overrides deserialize().
 */
public class TrimmingStringDeserializer extends StdScalarDeserializer<String> {

    public TrimmingStringDeserializer() {
        super(String.class);
    }

    @Override
    public String deserialize(JsonParser parser, DeserializationContext context) throws IOException {
        String value = StringDeserializer.instance.deserialize(parser, context);
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }
}
