package org.example.stockitbe.hq.circularbuyer;

public final class ChosungUtils {

    private static final char HANGUL_BASE = 0xAC00;
    private static final char HANGUL_LAST = 0xD7A3;
    private static final int CHOSUNG_INTERVAL = 21 * 28;
    private static final char[] CHOSUNG = {
            'ㄱ', 'ㄲ', 'ㄴ', 'ㄷ', 'ㄸ', 'ㄹ', 'ㅁ', 'ㅂ', 'ㅃ',
            'ㅅ', 'ㅆ', 'ㅇ', 'ㅈ', 'ㅉ', 'ㅊ', 'ㅋ', 'ㅌ', 'ㅍ', 'ㅎ'
    };

    private ChosungUtils() {
    }

    public static String toChosung(String input) {
        if (input == null || input.isBlank()) return "";
        StringBuilder sb = new StringBuilder(input.length());
        for (int i = 0; i < input.length(); i++) {
            char ch = input.charAt(i);
            if (ch >= HANGUL_BASE && ch <= HANGUL_LAST) {
                int index = (ch - HANGUL_BASE) / CHOSUNG_INTERVAL;
                if (index >= 0 && index < CHOSUNG.length) {
                    sb.append(CHOSUNG[index]);
                }
                continue;
            }
            if (!Character.isWhitespace(ch) && Character.isLetterOrDigit(ch)) {
                sb.append(Character.toLowerCase(ch));
            }
        }
        return sb.toString();
    }
}

