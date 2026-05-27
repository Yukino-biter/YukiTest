package com.yuki.test.util;

public final class YukiTimeUtils {

    private YukiTimeUtils() {
    }

    public static int getExamLimitMinutes(String level) {
        if (level == null) {
            throw new IllegalArgumentException("JLPT level cannot be null");
        }

        return switch (level.trim().toUpperCase()) {
            case "N1" -> 175;
            case "N2" -> 165;
            case "N3" -> 140;
            case "N4" -> 125;
            case "N5" -> 105;
            default -> throw new IllegalArgumentException("Unsupported JLPT level: " + level);
        };
    }

    public static int getMinutes(String level) {
        return getExamLimitMinutes(level);
    }

    public static void validateLevel(String level) {
        getExamLimitMinutes(level);
    }
}
