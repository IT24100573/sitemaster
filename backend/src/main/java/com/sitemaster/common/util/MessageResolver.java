package com.sitemaster.common.util;

import org.springframework.context.MessageSource;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.stereotype.Component;

/**
 * Looks up user-facing text in ValidationMessages.properties so that no
 * message is hard-coded in Java. Unknown keys fall back to the key itself,
 * which makes a missing entry obvious during development.
 * <p>
 * OOP concept: Encapsulation - hides MessageSource and locale handling.
 */
@Component
public class MessageResolver {

    private final MessageSource messageSource;

    public MessageResolver(MessageSource messageSource) {
        this.messageSource = messageSource;
    }

    public String get(String key, Object... args) {
        return messageSource.getMessage(key, args, key, LocaleContextHolder.getLocale());
    }
}
