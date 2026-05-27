package com.yuki.test.util;

import java.util.Locale;

public final class YukiAnswerUtils {

    private YukiAnswerUtils() {
    }

    public static String normalize(String answer) {
        if (answer == null || answer.isBlank()) {
            return "";
        }
        return answer.trim().toUpperCase(Locale.ROOT);
    }

    public static void requireOption(String answer) {
        String normalized = normalize(answer);
        if (!normalized.matches("[ABCD]")) {
            throw new IllegalArgumentException("答案必须是 A/B/C/D");
        }
    }
}
