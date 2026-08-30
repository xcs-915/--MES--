package com.tns.mes.common.i18n;

import org.springframework.util.StringUtils;
import org.springframework.web.servlet.LocaleResolver;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;

/** Resolves a request language from ?lang first, then Accept-Language. */
public class RequestLocaleResolver implements LocaleResolver {
    private static final Locale DEFAULT = Locale.SIMPLIFIED_CHINESE;
    private static final List<Locale> SUPPORTED = Arrays.asList(Locale.SIMPLIFIED_CHINESE, Locale.ENGLISH, Locale.forLanguageTag("ar-TN"));

    @Override
    public Locale resolveLocale(HttpServletRequest request) {
        String parameter = request.getParameter("lang");
        if (StringUtils.hasText(parameter)) return normalize(Locale.forLanguageTag(parameter));
        String header = request.getHeader("Accept-Language");
        if (StringUtils.hasText(header)) {
            for (Locale candidate : Locale.LanguageRange.parse(header).stream().map(range -> Locale.forLanguageTag(range.getRange())).collect(java.util.stream.Collectors.toList())) {
                Locale normalized = normalize(candidate);
                if (SUPPORTED.contains(normalized) || "ar".equalsIgnoreCase(normalized.getLanguage())) return normalized;
            }
        }
        return DEFAULT;
    }

    @Override
    public void setLocale(HttpServletRequest request, HttpServletResponse response, Locale locale) {
        // API language is selected per request; no server-side session state is required.
    }

    private Locale normalize(Locale locale) {
        if (locale == null || !StringUtils.hasText(locale.getLanguage())) return DEFAULT;
        if ("zh".equalsIgnoreCase(locale.getLanguage())) return Locale.SIMPLIFIED_CHINESE;
        if ("ar".equalsIgnoreCase(locale.getLanguage())) return Locale.forLanguageTag("ar-TN");
        if ("en".equalsIgnoreCase(locale.getLanguage())) return Locale.ENGLISH;
        return DEFAULT;
    }
}
