package com.tiertests.tiertagger.data;

public record GameMode(String name, String prefix, String hexColor, String icon) {

    public int colorInt() {
        if (hexColor == null || hexColor.isEmpty()) return 0xFFFFFF;
        String hex = hexColor.startsWith("#") ? hexColor.substring(1) : hexColor;
        if (hex.length() != 6) return 0xFFFFFF;
        try {
            return Integer.parseInt(hex, 16);
        } catch (NumberFormatException e) {
            return 0xFFFFFF;
        }
    }
}
