package com.sourabh.payment_platform.ai;

import java.util.List;
import java.util.regex.Pattern;
import org.springframework.stereotype.Component;

@Component
public class AiInputSanitizer {

    private static final List<Pattern> SECRET_PATTERNS = List.of(
            Pattern.compile("(?i)(authorization\\s*[:=]\\s*bearer\\s+)[^\\s]+"),
            Pattern.compile("(?i)(api[-_ ]?key\\s*[:=]\\s*)[^\\s,;]+"),
            Pattern.compile("(?i)(password\\s*[:=]\\s*)[^\\s,;]+"),
            Pattern.compile("(?i)(secret\\s*[:=]\\s*)[^\\s,;]+")
    );

    public String sanitize(String value) {
        if (value == null || value.isBlank()) {
            return "";
        }

        String sanitizedValue = value;
        for (Pattern pattern : SECRET_PATTERNS) {
            sanitizedValue = pattern.matcher(sanitizedValue).replaceAll("$1[REDACTED]");
        }

        return sanitizedValue;
    }
}
