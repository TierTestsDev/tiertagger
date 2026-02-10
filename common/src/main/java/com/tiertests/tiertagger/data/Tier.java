package com.tiertests.tiertagger.data;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum Tier {
    S("§6S", "§6HT1"),
    A_PLUS("§aA+", "§aLT1"),
    A_MINUS("§aA-", "§aHT2"),
    B_PLUS("§9B+", "§9LT2"),
    B_MINUS("§9B-", "§9HT3"),
    C_PLUS("§eC+", "§eLT3"),
    C_MINUS("§eC-", "§eHT4"),
    D_PLUS("§cD+", "§cLT4"),
    D_MINUS("§cD-", "§cHT5"),
    F("§4F", "§4LT5");

    private final String displayName;
    private final String mcTiersName;

    public static Tier fromString(String name) {
        if (name == null || name.isEmpty())
            return null;

        String normalized = name.toUpperCase().trim();

        // Handle standard tier names and MCTiers conversions
        switch (normalized) {
            // Standard tiers
            case "S": return S;
            case "A+", "A_PLUS": return A_PLUS;
            case "A-", "A_MINUS": return A_MINUS;
            case "B+", "B_PLUS": return B_PLUS;
            case "B-", "B_MINUS": return B_MINUS;
            case "C+", "C_PLUS": return C_PLUS;
            case "C-", "C_MINUS": return C_MINUS;
            case "D+", "D_PLUS": return D_PLUS;
            case "D-", "D_MINUS": return D_MINUS;
            case "F": return F;

            // MCTiers format conversions (HT1-5, LT1-5 → standard)
            case "HT1": return S;       // HT1 → S
            case "LT1": return A_PLUS;  // LT1 → A+
            case "HT2": return A_MINUS; // HT2 → A-
            case "LT2": return B_PLUS;  // LT2 → B+
            case "HT3": return B_MINUS; // HT3 → B-
            case "LT3": return C_PLUS;  // LT3 → C+
            case "HT4": return C_MINUS; // HT4 → C-
            case "LT4": return D_PLUS;  // LT4 → D+
            case "HT5": return D_MINUS; // HT5 → D-
            case "LT5": return F;       // LT5 → F

            default:
                // Try direct enum lookup as fallback
                try {
                    return Tier.valueOf(normalized.replace("+", "_PLUS").replace("-", "_MINUS"));
                } catch (IllegalArgumentException e) {
                    return null;
                }
        }
    }
}
