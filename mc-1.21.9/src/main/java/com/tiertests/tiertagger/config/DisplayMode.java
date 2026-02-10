package com.tiertests.tiertagger.config;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum DisplayMode {
    HIGHEST_FALLBACK("Highest Fallback"),
    HIGHEST_ALWAYS("Highest Always"),
    SELECTED_ONLY("Selected Only"),
    RANKING("Ranking"),
    CROSS_API_SAME_MODE("Cross-API Same Mode"),
    CROSS_API_ANY_MODE("Cross-API Any Mode");

    private final String displayName;
}
