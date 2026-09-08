package com.pulse.auth.user;

import java.nio.charset.StandardCharsets;

/**
 * Draws the placeholder avatar for users who never uploaded a picture: a disc
 * with their initial, in a colour derived from the alias.
 *
 * <p>Serving this instead of a 404 means every {@code <img>} in the feed
 * resolves, so clients need no fallback logic and the browser console stays
 * clean. The hue is a pure function of the alias, so the same person always
 * gets the same colour.
 */
final class GeneratedAvatar {

    private GeneratedAvatar() {
    }

    static byte[] forAlias(String alias) {
        int hue = hueOf(alias);
        char initial = initialOf(alias);

        String svg = """
                <svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 100 100" width="100" height="100" \
                role="img" aria-label="Avatar">\
                <rect width="100" height="100" rx="50" fill="hsl(%d 42%% 26%%)"/>\
                <text x="50" y="52" text-anchor="middle" dominant-baseline="central" \
                font-family="Inter, Segoe UI, Helvetica, Arial, sans-serif" font-size="46" \
                font-weight="600" fill="hsl(%d 55%% 84%%)">%s</text>\
                </svg>
                """.formatted(hue, hue, initial);
        return svg.getBytes(StandardCharsets.UTF_8);
    }

    /** Same hashing the web client uses for its offline fallback, so colours match. */
    private static int hueOf(String alias) {
        int hash = 0;
        for (int i = 0; i < alias.length(); i++) {
            hash = hash * 31 + alias.charAt(i);
        }
        return Math.abs(hash % 360);
    }

    /** Letters and digits only: anything else would need XML escaping. */
    private static char initialOf(String alias) {
        String trimmed = alias.trim();
        char first = trimmed.isEmpty() ? '?' : Character.toUpperCase(trimmed.charAt(0));
        return Character.isLetterOrDigit(first) ? first : '?';
    }
}
