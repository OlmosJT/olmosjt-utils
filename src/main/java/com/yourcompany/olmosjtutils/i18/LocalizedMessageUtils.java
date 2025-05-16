package com.yourcompany.olmosjtutils.i18;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.MessageSource;
import org.springframework.context.NoSuchMessageException;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.stereotype.Component;

import java.util.Locale;

@Slf4j
@Component
@RequiredArgsConstructor
public class LocalizedMessageUtils {

    private final MessageSource messageSource;

    /**
     * Retrieves a localized message for the given code and arguments,
     * using the current locale from LocaleContextHolder.
     *
     * @param code The message code to look up (e.g., "user.not.found").
     * @param args Array of arguments that will be filled in for params within the message
     * (params look like "{0}", "{1,date}", "{2,time}" within a message),
     * or null if none.
     * @return The resolved localized message. If the message is not found for the current locale,
     * it attempts to resolve it without a locale (which might return a default message
     * or the code itself depending on MessageSource configuration).
     * If still not found, it returns the message code itself as a fallback.
     */
    public String getMessage(String code, Object... args) {
        Locale locale = LocaleContextHolder.getLocale();
        try {
            return messageSource.getMessage(code, args, locale);
        } catch (NoSuchMessageException e) {
            e.printStackTrace();
            return fallbackMessage(code, locale);
        }
    }

    /**
     * Retrieves a localized message for the given code, arguments, and a specific locale.
     *
     * @param code The message code to look up.
     * @param locale The specific locale to use for message resolution.
     * @param args Array of arguments for message parameters.
     * @return The resolved localized message. Returns the code if not found.
     */
    public String getMessageForLocale(String code, Locale locale, Object... args) {
        if (locale == null) {
            log.warn("No locale provided for locale 'null'");
            return getMessage(code, args);
        }
        try {
            return messageSource.getMessage(code, args, locale);
        } catch (NoSuchMessageException e) {
            e.printStackTrace();
            return fallbackMessage(code, locale);
        }
    }

    private String fallbackMessage(String code, Locale locale) {
        log.warn("Message not found for code '{}' and locale '{}'", code, locale);
        return "[" + code + "]";
    }

    /**
     * Retrieves a localized message for the given code and arguments,
     * providing a default message if the code is not found.
     *
     * @param code The message code to look up.
     * @param defaultMessage The default message to return if the lookup fails.
     * @param args Array of arguments for message parameters.
     * @return The resolved localized message or the defaultMessage if not found.
     */
    public String getMessageOrDefault(String code, String defaultMessage, Object... args) {
        Locale locale = LocaleContextHolder.getLocale();
        return messageSource.getMessage(code, args, defaultMessage, locale);
    }

}
