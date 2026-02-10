package com.tiertests.tiertagger.config;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum DisplayType {
    PREFIX("Prefix"),
    SUFFIX("Suffix");

    private final String displayName;
}
