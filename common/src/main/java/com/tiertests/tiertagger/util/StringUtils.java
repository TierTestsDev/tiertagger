package com.tiertests.tiertagger.util;

import lombok.NonNull;

public class StringUtils {
    @NonNull
    public static String sanitize(String input) {
        if (input == null || input.equalsIgnoreCase("null")) return "";
        return input;
    }


    public static String stripColorCodes(String input) {
        if (input == null) return null;
        return input.replaceAll("§[0-9a-fk-or]", "");
    }

    public static String colorize(String input) {
        if (input == null) return null;
        return input.replaceAll("&", "§");
    }

    public static String beautify(String string) {
        if (string == null || string.isEmpty()) return string;
        if (string.length() == 1) return string.toUpperCase();
        return string.substring(0, 1).toUpperCase() + string.substring(1).toLowerCase();
    }
}
