package com.tiertests.tiertagger.util;

import com.tiertests.tiertagger.api.TierSourceFactory;
import com.tiertests.tiertagger.config.DisplayMode;
import com.tiertests.tiertagger.config.ModConfig;
import com.tiertests.tiertagger.data.GameMode;
import com.tiertests.tiertagger.data.GameModeManager;
import com.tiertests.tiertagger.data.PlayerData;
import com.tiertests.tiertagger.data.Tier;
import com.tiertests.tiertagger.manager.TierManager;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;

import java.util.Map;
import java.util.Set;
import java.util.UUID;


public class TierTagUtil {

    public static MutableComponent buildTagComponent(UUID uuid) {
        PlayerData data = TierManager.getPlayerData(uuid);
        if (data == null) return null;

        DisplayMode displayMode = ModConfig.getTierDisplayMode();

        GameMode gameMode = null;
        Tier tier = null;

        boolean useRanking = false;

        switch (displayMode) {
            case HIGHEST_FALLBACK -> {
                gameMode = GameModeManager.getFromInt(ModConfig.getGameMode());
                if (gameMode == null) gameMode = data.getHighestTierGamemode();

                tier = data.getTier(gameMode);
                if (tier == null) {
                    gameMode = data.getHighestTierGamemode();
                    tier = data.getHighestTier();
                }
            }
            case SELECTED_ONLY -> {
                gameMode = GameModeManager.getFromInt(ModConfig.getGameMode());
                tier = data.getTier(gameMode);
            }
            case RANKING -> {
                useRanking = true;
            }
            case CROSS_API_SAME_MODE -> {
                gameMode = GameModeManager.getFromInt(ModConfig.getGameMode());
                if (gameMode != null) tier = data.getTier(gameMode);
                if (tier == null && gameMode != null) {
                    Tier resolved = resolveCrossApiSameMode(uuid, gameMode);
                    if (resolved != null) {
                        tier = resolved;
                    }
                }
            }
            case CROSS_API_ANY_MODE -> {
                gameMode = GameModeManager.getFromInt(ModConfig.getGameMode());
                if (gameMode != null) tier = data.getTier(gameMode);
                if (tier == null && gameMode != null) {
                    Tier resolved = resolveCrossApiSameMode(uuid, gameMode);
                    if (resolved != null) {
                        tier = resolved;
                    }
                }
                if (tier == null) {
                    CrossApiResult result2 = resolveCrossApiAnyMode(uuid);
                    if (result2 != null) {
                        gameMode = result2.mode;
                        tier = result2.tier;
                    }
                }
                if (tier == null) {
                    gameMode = data.getHighestTierGamemode();
                    tier = data.getHighestTier();
                }
            }
            default -> {
                gameMode = data.getHighestTierGamemode();
                tier = data.getHighestTier();
            }
        }

        MutableComponent result = Component.empty();
        boolean hasContent = false;

        if (!useRanking && gameMode != null && tier != null) {
            final GameMode mode = gameMode;
            String icon = IconResolver.resolve(mode);
            if (icon != null && !icon.isEmpty()) {
                if (IconResolver.isClassicIcon(mode)) {
                    final int color = IconResolver.getClassicColor(mode) != -1 ? IconResolver.getClassicColor(mode) : mode.colorInt();
                    result.append(Component.literal(icon + " ").withStyle(s -> s.withColor(color)));
                } else {
                    result.append(Component.literal(icon + " "));
                }
            }

            String tierName = ModConfig.isUseMCTiersFormat() ? tier.getMcTiersName()
                    : tier.getDisplayName();
            result.append(Component.literal(tierName));

            if (ModConfig.isShowRegion() && data.getRegion() != null) {
                result.append(Component.literal(" §7[" + data.getRegion().shortPrefix() + "§7]"));
            }

            hasContent = true;
        }

        if (useRanking) {
            result.append(Component.literal("#" + data.getRank() + " "));
            hasContent = true;
        }

        if (data.getBadge() != null && !data.getBadge().isEmpty()) {
            if (hasContent) result.append(Component.literal(" "));
            result.append(Component.literal(data.getBadge()));
            hasContent = true;
        }

        return hasContent ? result : null;
    }

    public static MutableComponent buildTabComponent(UUID uuid) {
        PlayerData data = TierManager.getPlayerData(uuid);
        if (data == null) return null;

        DisplayMode displayMode = ModConfig.getTierDisplayMode();

        GameMode gameMode = null;
        Tier tier = null;

        switch (displayMode) {
            case HIGHEST_FALLBACK -> {
                gameMode = GameModeManager.getFromInt(ModConfig.getGameMode());
                if (gameMode == null) gameMode = data.getHighestTierGamemode();

                tier = data.getTier(gameMode);
                if (tier == null) {
                    gameMode = data.getHighestTierGamemode();
                    tier = data.getHighestTier();
                }
            }
            case SELECTED_ONLY -> {
                gameMode = GameModeManager.getFromInt(ModConfig.getGameMode());
                tier = data.getTier(gameMode);
            }
            case CROSS_API_SAME_MODE -> {
                gameMode = GameModeManager.getFromInt(ModConfig.getGameMode());
                if (gameMode != null) tier = data.getTier(gameMode);
                if (tier == null && gameMode != null) {
                    Tier resolved = resolveCrossApiSameMode(uuid, gameMode);
                    if (resolved != null) {
                        tier = resolved;
                    }
                }
            }
            case CROSS_API_ANY_MODE -> {
                gameMode = GameModeManager.getFromInt(ModConfig.getGameMode());
                if (gameMode != null) tier = data.getTier(gameMode);
                if (tier == null && gameMode != null) {
                    Tier resolved = resolveCrossApiSameMode(uuid, gameMode);
                    if (resolved != null) {
                        tier = resolved;
                    }
                }
                if (tier == null) {
                    CrossApiResult result2 = resolveCrossApiAnyMode(uuid);
                    if (result2 != null) {
                        gameMode = result2.mode;
                        tier = result2.tier;
                    }
                }
                if (tier == null) {
                    gameMode = data.getHighestTierGamemode();
                    tier = data.getHighestTier();
                }
            }
            default -> {
                gameMode = data.getHighestTierGamemode();
                tier = data.getHighestTier();
            }
        }

        if (gameMode == null || tier == null) return null;

        final GameMode mode = gameMode;
        MutableComponent result = Component.empty();

        String icon = IconResolver.resolve(mode);
        if (icon != null && !icon.isEmpty()) {
            if (IconResolver.isClassicIcon(mode)) {
                final int color = IconResolver.getClassicColor(mode) != -1 ? IconResolver.getClassicColor(mode) : mode.colorInt();
                result.append(Component.literal(icon + " ").withStyle(s -> s.withColor(color)));
            } else {
                result.append(Component.literal(icon + " "));
            }
        }

        String tierName = ModConfig.isUseMCTiersFormat() ? tier.getMcTiersName()
                : tier.getDisplayName();
        result.append(Component.literal(tierName));

        return result;
    }

    public static String buildTag(UUID uuid) {
        MutableComponent comp = buildTagComponent(uuid);
        return comp != null ? comp.getString() : "";
    }

    private static final Set<Set<String>> MODE_ALIASES = Set.of(
            Set.of("vanilla", "crystal"),
            Set.of("nethpot", "nethop", "neth_pot", "netherpot")
    );

    private static boolean isSameMode(String a, String b) {
        if (a.equalsIgnoreCase(b)) return true;
        String la = a.toLowerCase();
        String lb = b.toLowerCase();
        for (Set<String> group : MODE_ALIASES) {
            if (group.contains(la) && group.contains(lb)) return true;
        }
        return false;
    }

    private record CrossApiResult(GameMode mode, Tier tier) {}

    private static Tier resolveCrossApiSameMode(UUID uuid, GameMode targetMode) {
        if (targetMode == null) return null;
        Tier best = null;
        Map<TierSourceFactory.TierSources, PlayerData> allData = TierManager.getAllCrossSourceData(uuid);
        for (PlayerData pd : allData.values()) {
            for (Map.Entry<GameMode, Tier> entry : pd.getTiers().entrySet()) {
                if (!isSameMode(entry.getKey().name(), targetMode.name())) continue;
                Tier t = entry.getValue();
                if (t != null && (best == null || t.ordinal() < best.ordinal())) {
                    best = t;
                }
            }
        }
        return best;
    }

    private static CrossApiResult resolveCrossApiAnyMode(UUID uuid) {
        Tier best = null;
        GameMode bestMode = null;
        Map<TierSourceFactory.TierSources, PlayerData> allData = TierManager.getAllCrossSourceData(uuid);
        for (PlayerData pd : allData.values()) {
            for (Map.Entry<GameMode, Tier> entry : pd.getTiers().entrySet()) {
                Tier t = entry.getValue();
                if (t != null && (best == null || t.ordinal() < best.ordinal())) {
                    best = t;
                    bestMode = entry.getKey();
                }
            }
        }
        return best != null ? new CrossApiResult(bestMode, best) : null;
    }
}
