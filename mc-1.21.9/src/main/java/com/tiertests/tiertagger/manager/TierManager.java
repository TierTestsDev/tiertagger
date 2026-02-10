package com.tiertests.tiertagger.manager;

import com.tiertests.tiertagger.api.TierAPI;
import com.tiertests.tiertagger.api.TierSourceFactory;
import com.tiertests.tiertagger.config.ModConfig;
import com.tiertests.tiertagger.data.GameModeManager;
import com.tiertests.tiertagger.data.PlayerData;
import net.minecraft.client.player.AbstractClientPlayer;

import com.tiertests.tiertagger.TierTaggerCommon;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CompletableFuture;
import com.tiertests.tiertagger.config.DisplayMode;

public class TierManager {
    private static final Map<UUID, PlayerData> cache = new ConcurrentHashMap<>();
    private static final Map<UUID, Map<TierSourceFactory.TierSources, PlayerData>> crossSourceCache = new ConcurrentHashMap<>();
    private static final Set<String> pendingCrossFetches = ConcurrentHashMap.newKeySet();
    private static final long CACHE_DURATION = 5 * 60 * 1000;

    public static PlayerData getPlayerData(AbstractClientPlayer player) {
        return getPlayerData(player.getUUID());
    }

    public static PlayerData getPlayerData(UUID uuid) {
        PlayerData data = cache.get(uuid);
        if (data == null) return null;

        long age = System.currentTimeMillis() - data.getLastUpdated();
        if (age < CACHE_DURATION) {
            return data;
        }

        return null;
    }

    public static boolean hasPlayerData(UUID uuid) {
        PlayerData data = cache.get(uuid);
        if (data == null) return false;

        long age = System.currentTimeMillis() - data.getLastUpdated();
        return !data.isEmpty() && age < CACHE_DURATION;
    }

    public static PlayerData getCachedData(UUID uuid) {
        PlayerData data = cache.get(uuid);

        if (data != null) {
            long age = System.currentTimeMillis() - data.getLastUpdated();
            if (age < CACHE_DURATION) {
                return data;
            }
            cache.remove(uuid);
        }

        return null;
    }

    public static void cachePlayerData(UUID uuid, PlayerData data) {
        if (data != null) {
            cache.put(uuid, data);
        }
    }

    public static void fetchPlayerData(UUID uuid, String name) {
        TierAPI.fetchPlayerData(uuid, name).thenAccept(data -> {
            if (data != null && !data.isEmpty()) {
                cache.put(uuid, data);
                fetchTierTestsBadge(uuid, name, data);

                DisplayMode mode = ModConfig.getTierDisplayMode();
                if (mode == DisplayMode.CROSS_API_SAME_MODE || mode == DisplayMode.CROSS_API_ANY_MODE) {
                    fetchCrossSourceData(uuid, name, ModConfig.getTierSource());
                }
            }
        }).exceptionally(ex -> {
            TierTaggerCommon.LOGGER.error("Error fetching tier data for {}", name, ex);
            return null;
        });
    }

    private static void fetchTierTestsBadge(UUID uuid, String name, PlayerData target) {
        if (ModConfig.getTierSource() == TierSourceFactory.TierSources.TIER_TESTS) return;

        TierSourceFactory.getTierSource(TierSourceFactory.TierSources.TIER_TESTS)
                .fetchPlayerData(uuid, name)
                .thenAccept(ttData -> {
                    if (ttData != null && ttData.getBadge() != null && !ttData.getBadge().isEmpty()) {
                        target.setBadge(ttData.getBadge());
                    }
                });
    }

    public static void clearCache() {
        cache.clear();
        crossSourceCache.clear();
        pendingCrossFetches.clear();
    }

    public static void cleanupCache(List<AbstractClientPlayer> currentPlayers) {
        Set<UUID> currentUUIDs = new HashSet<>();
        for (AbstractClientPlayer player : currentPlayers) {
            if (player != null) {
                currentUUIDs.add(player.getUUID());
            }
        }

        cache.keySet().removeIf(uuid -> !currentUUIDs.contains(uuid));
        crossSourceCache.keySet().removeIf(uuid -> !currentUUIDs.contains(uuid));
    }

    public static PlayerData getCrossSourceData(UUID uuid, TierSourceFactory.TierSources source) {
        Map<TierSourceFactory.TierSources, PlayerData> sourceMap = crossSourceCache.get(uuid);
        if (sourceMap == null) return null;
        PlayerData data = sourceMap.get(source);
        if (data == null) return null;
        long age = System.currentTimeMillis() - data.getLastUpdated();
        if (age >= CACHE_DURATION) {
            sourceMap.remove(source);
            return null;
        }
        return data;
    }

    public static Map<TierSourceFactory.TierSources, PlayerData> getAllCrossSourceData(UUID uuid) {
        return crossSourceCache.getOrDefault(uuid, Collections.emptyMap());
    }

    public static void fetchCrossSourceData(UUID uuid, String name, TierSourceFactory.TierSources excludeSource) {
        for (TierSourceFactory.TierSources source : TierSourceFactory.TierSources.values()) {
            if (source == excludeSource) continue;
            String key = uuid + ":" + source.name();
            if (pendingCrossFetches.contains(key)) continue;
            if (getCrossSourceData(uuid, source) != null) continue;

            pendingCrossFetches.add(key);
            GameModeManager.loadModesForSource(source).thenCompose(v ->
                TierSourceFactory.getTierSource(source).fetchPlayerData(uuid, name)
            ).thenAccept(data -> {
                pendingCrossFetches.remove(key);
                if (data != null && !data.isEmpty()) {
                    crossSourceCache.computeIfAbsent(uuid, k -> new ConcurrentHashMap<>()).put(source, data);
                }
            });
        }
    }
}
