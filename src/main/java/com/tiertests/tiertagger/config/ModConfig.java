package com.tiertests.tiertagger.config;

import com.tiertests.tiertagger.TierTaggerCommon;
import com.tiertests.tiertagger.api.TierSourceFactory;
import com.tiertests.tiertagger.data.GameMode;
import com.tiertests.tiertagger.data.GameModeManager;
import com.tiertests.tiertagger.manager.TierManager;
import lombok.Getter;

import java.util.Objects;

public class ModConfig {

    @Getter
    static int gameMode = 0;

    @Getter
    static DisplayMode tierDisplayMode = DisplayMode.HIGHEST_FALLBACK;

    @Getter
    static boolean useMCTiersFormat = true;

    @Getter
    static boolean showRegion = true;

    @Getter
    static boolean showInTab = true;

    @Getter
    static DisplayType displayType = DisplayType.PREFIX;

    @Getter
    static IconType iconType = IconType.MCTIERS;

    @Getter
    static TierSourceFactory.TierSources tierSource = TierSourceFactory.TierSources.TIER_TESTS;

    public static GameMode getSelectedGameMode() {
        GameMode mode = GameModeManager.getFromInt(gameMode);
        if (mode != null)
            return mode;

        if (GameModeManager.getGameModeSize() > 0) {
            gameMode = 0;
            return GameModeManager.getFromInt(0);
        }

        return null;
    }

    public static String getSelectedGameModeName() {
        if (GameModeManager.isLoading())
            return "Loading Modes...";

        GameMode mode = GameModeManager.getFromInt(gameMode);
        if (mode != null)
            return mode.name();

        if (GameModeManager.getGameModeSize() > 0) {
            gameMode = 0;
            return Objects.requireNonNull(GameModeManager.getFromInt(0)).name();
        }

        return "Not Found";
    }

    public static void nextGameMode() {
        int size = GameModeManager.getGameModeSize();
        if (size <= 0)
            return;

        gameMode = Math.floorMod(gameMode + 1, size);
        ModConfigLoader.save();
    }

    public static void nextTierDisplayMode() {
        if (tierDisplayMode == null) {
            tierDisplayMode = DisplayMode.values()[0];
            return;
        }

        DisplayMode[] values = DisplayMode.values();
        tierDisplayMode = values[(tierDisplayMode.ordinal() + 1) % values.length];
        ModConfigLoader.save();
    }

    public static void nextTierDisplayType() {
        if (displayType == null) {
            displayType = DisplayType.values()[0];
            return;
        }

        DisplayType[] values = DisplayType.values();
        displayType = values[(displayType.ordinal() + 1) % values.length];
        ModConfigLoader.save();
    }

    public static void toggleUseMCTiersFormat() {
        useMCTiersFormat = !useMCTiersFormat;
        ModConfigLoader.save();
    }

    public static void toggleShowRegion() {
        showRegion = !showRegion;
        ModConfigLoader.save();
    }

    public static void toggleShowInTab() {
        showInTab = !showInTab;
        ModConfigLoader.save();
    }

    public static void nextIconType() {
        IconType[] values = IconType.values();
        iconType = values[(iconType.ordinal() + 1) % values.length];
        ModConfigLoader.save();
    }

    public static void nextTierSource() {
        TierSourceFactory.TierSources[] sources = TierSourceFactory.TierSources.values();
        int nextOrdinal = (tierSource.ordinal() + 1) % sources.length;
        tierSource = sources[nextOrdinal];
        ModConfigLoader.save();
        GameModeManager.updateModes().thenRun(() -> {
            gameMode = 0;
            ModConfigLoader.save();
        });
        TierManager.clearCache();
        TierTaggerCommon.clearPendingFetches();
    }

    public static void setGamemode(int i) {
        gameMode = i;
    }
}
