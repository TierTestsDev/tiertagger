package com.tiertests.tiertagger.util;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum Colors {
    DARK_RED("#AA0000", "4"),
    RED("#FF5555", "c"),
    GOLD("#FFAA00", "6"),
    YELLOW("#FFFF55", "e"),
    GREEN("#55FF55", "a"),
    DARK_GREEN("#00AA00", "2"),
    AQUA("#55FFFF", "b"),
    DARK_AQUA("#00AAAA", "3"),
    BLUE("#5555FF", "9"),
    LIGHT_PURPLE("#FF55FF", "d"),
    DARK_PURPLE("#AA00AA", "5"),
    WHITE("#FFFFFF", "f"),
    GRAY("#AAAAAA", "7"),
    DARK_GRAY("#555555", "8"),
    BLACK("#000000", "0");

    private final String hex;
    private final String legacy;

    public static Colors getFromName(String string) {
        for (Colors colors : values()) {
            if (colors.name().equalsIgnoreCase(string)) return colors;
        }
        return Colors.WHITE;
    }

    public String getLegacyWithSymbol() {
        return "§" + getLegacy();
    }

    public String getHexWithSymbol() {
        return "§" + getHex();
    }
}
