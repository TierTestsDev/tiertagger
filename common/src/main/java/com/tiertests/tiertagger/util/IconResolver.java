package com.tiertests.tiertagger.util;

import com.tiertests.tiertagger.config.IconType;
import com.tiertests.tiertagger.config.ModConfig;
import com.tiertests.tiertagger.data.GameMode;

import java.util.Map;
import java.util.Set;

public class IconResolver {

    // MCTiers-style PNG icons (PUA chars mapped to textures in default.json)
    private static final Map<String, String> MCTIERS_ICONS = Map.ofEntries(
            // MCTiers / PVPTiers modes
            Map.entry("axe", "\uE701"),
            Map.entry("mace", "\uE702"),
            Map.entry("neth_pot", "\uE703"),
            Map.entry("nethop", "\uE703"),
            Map.entry("netherpot", "\uE703"),
            Map.entry("pot", "\uE704"),
            Map.entry("smp", "\uE705"),
            Map.entry("sword", "\uE706"),
            Map.entry("uhc", "\uE707"),
            Map.entry("vanilla", "\uE708"),
            Map.entry("crystal", "\uE708"),
            Map.entry("diamond", "\uE708"),
            // SubTiers modes
            Map.entry("bed", "\uE801"),
            Map.entry("bow", "\uE802"),
            Map.entry("creeper", "\uE803"),
            Map.entry("debuff", "\uE804"),
            Map.entry("dia_crystal", "\uE805"),
            Map.entry("dia_smp", "\uE806"),
            Map.entry("elytra", "\uE807"),
            Map.entry("manhunt", "\uE808"),
            Map.entry("minecart", "\uE809"),
            Map.entry("og_vanilla", "\uE810"),
            Map.entry("speed", "\uE811"),
            Map.entry("trident", "\uE812")
    );

    // Classic Unicode icons (colored text symbols)
    private static final Map<String, String> CLASSIC_ICONS = Map.ofEntries(
            // MCTiers / PVPTiers modes
            Map.entry("axe", "\u2694"),
            Map.entry("mace", "\u2692"),
            Map.entry("neth_pot", "\u2620"),
            Map.entry("nethop", "\u2620"),
            Map.entry("netherpot", "\u2620"),
            Map.entry("pot", "\u2764"),
            Map.entry("smp", "\u2726"),
            Map.entry("sword", "\u2694"),
            Map.entry("uhc", "\u2764"),
            Map.entry("vanilla", "\u2726"),
            Map.entry("crystal", "\u2726"),
            Map.entry("diamond", "\u2726"),
            // SubTiers modes
            Map.entry("bed", "\u2B50"),
            Map.entry("bow", "\u27B3"),
            Map.entry("creeper", "\u2622"),
            Map.entry("debuff", "\u2623"),
            Map.entry("dia_crystal", "\u2756"),
            Map.entry("dia_smp", "\u2756"),
            Map.entry("elytra", "\u2708"),
            Map.entry("manhunt", "\u2316"),
            Map.entry("minecart", "\u26CF"),
            Map.entry("og_vanilla", "\u2726"),
            Map.entry("speed", "\u26A1"),
            Map.entry("trident", "\u2693")
    );

    // Override colors for classic icons (ensures no white/black icons)
    private static final Map<String, Integer> CLASSIC_COLORS = Map.ofEntries(
            Map.entry("axe", 0x55FF55),
            Map.entry("mace", 0xAAAAAA),
            Map.entry("neth_pot", 0x7d4a40),
            Map.entry("nethop", 0x7d4a40),
            Map.entry("netherpot", 0x7d4a40),
            Map.entry("pot", 0xFF5555),
            Map.entry("smp", 0xeccb45),
            Map.entry("sword", 0xa4fdf0),
            Map.entry("uhc", 0xFF5555),
            Map.entry("vanilla", 0xFF55FF),
            Map.entry("crystal", 0xFF55FF),
            Map.entry("diamond", 0x55FFFF),
            Map.entry("bed", 0xFF5555),
            Map.entry("bow", 0xAA6633),
            Map.entry("creeper", 0x55FF55),
            Map.entry("debuff", 0xAA55AA),
            Map.entry("dia_crystal", 0x55FFFF),
            Map.entry("dia_smp", 0xAA55AA),
            Map.entry("elytra", 0x8d8db1),
            Map.entry("manhunt", 0xFF5555),
            Map.entry("minecart", 0xAAAAAA),
            Map.entry("og_vanilla", 0xFFAA00),
            Map.entry("speed", 0x55FFFF),
            Map.entry("trident", 0x55AA55)
    );

    // Aliases: modes that should resolve to the same icon
    private static final Set<Set<String>> ICON_ALIASES = Set.of(
            Set.of("vanilla", "crystal"),
            Set.of("nethpot", "nethop", "neth_pot", "netherpot")
    );

    /**
     * Resolves the icon for a given game mode based on the current IconType setting.
     * If MCTiers is selected but no PNG icon exists, falls back to classic.
     */
    public static String resolve(GameMode mode) {
        if (mode == null) return "";

        IconType iconType = ModConfig.getIconType();
        String key = normalizeKey(mode);

        if (iconType == IconType.MCTIERS) {
            String icon = lookupIcon(MCTIERS_ICONS, key);
            if (icon != null) return icon;
            // Fallback to classic if no MCTiers icon exists
            icon = lookupIcon(CLASSIC_ICONS, key);
            if (icon != null) return icon;
        } else {
            String icon = lookupIcon(CLASSIC_ICONS, key);
            if (icon != null) return icon;
        }

        // Fallback to mode's own icon
        String modeIcon = mode.icon();
        return modeIcon != null ? modeIcon : "";
    }

    /**
     * Returns the color override for classic icons, or -1 if none.
     * Call sites should use this when IconType is CLASSIC or when falling back to classic.
     */
    public static int getClassicColor(GameMode mode) {
        if (mode == null) return -1;
        String key = normalizeKey(mode);
        Integer color = CLASSIC_COLORS.get(key);
        if (color != null) return color;
        for (Set<String> group : ICON_ALIASES) {
            if (group.contains(key)) {
                for (String alias : group) {
                    color = CLASSIC_COLORS.get(alias);
                    if (color != null) return color;
                }
            }
        }
        return -1;
    }

    /**
     * Returns true if the resolved icon is a classic (Unicode) icon.
     */
    public static boolean isClassicIcon(GameMode mode) {
        if (mode == null) return false;
        IconType iconType = ModConfig.getIconType();
        if (iconType == IconType.CLASSIC) return true;
        // MCTiers mode but no MCTiers icon → fell back to classic
        String key = normalizeKey(mode);
        return lookupIcon(MCTIERS_ICONS, key) == null;
    }

    private static String lookupIcon(Map<String, String> iconMap, String key) {
        String icon = iconMap.get(key);
        if (icon != null) return icon;
        for (Set<String> group : ICON_ALIASES) {
            if (group.contains(key)) {
                for (String alias : group) {
                    icon = iconMap.get(alias);
                    if (icon != null) return icon;
                }
            }
        }
        return null;
    }

    private static String normalizeKey(GameMode mode) {
        // Use prefix first (it's the lowercase key), then name
        String key = mode.prefix();
        if (key != null && !key.isEmpty()) return key.toLowerCase();
        return mode.name().toLowerCase();
    }
}
