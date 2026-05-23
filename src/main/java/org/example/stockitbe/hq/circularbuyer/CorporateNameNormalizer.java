package org.example.stockitbe.hq.circularbuyer;

import java.util.regex.Pattern;

public final class CorporateNameNormalizer {

    private static final Pattern LEADING_CORP_MARKER = Pattern.compile(
            "^(?:\\(\\s*[주유사]\\s*\\)|㈜|㈔|주식회사|유한회사|사단법인)\\s*"
    );

    private CorporateNameNormalizer() {
    }

    public static String stripLeadingMarker(String input) {
        if (input == null || input.isBlank()) {
            return "";
        }
        String normalized = input.trim();
        while (true) {
            String stripped = LEADING_CORP_MARKER.matcher(normalized).replaceFirst("").trim();
            if (stripped.equals(normalized)) {
                return stripped;
            }
            normalized = stripped;
        }
    }
}
