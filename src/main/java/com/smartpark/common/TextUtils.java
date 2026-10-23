package com.smartpark.common;

public final class TextUtils {

    private TextUtils() {}

    /** Trims and collapses runs of whitespace: "  Juan   Dela Cruz " → "Juan Dela Cruz". */
    public static String collapseSpaces(String value) {
        return value == null ? null : value.trim().replaceAll("\\s+", " ");
    }
}
