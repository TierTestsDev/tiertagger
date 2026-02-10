package com.tiertests.tiertagger.config;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum IconType {
    CLASSIC("Classic"),
    MCTIERS("MCTiers");

    private final String displayName;
}
