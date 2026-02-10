package com.tiertests.tiertagger.data;

import com.tiertests.tiertagger.api.TierAPI;
import com.tiertests.tiertagger.api.TierSourceFactory;
import com.tiertests.tiertagger.config.ModConfig;

import java.util.ArrayList;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;

public class GameModeManager {
    private static volatile Map<String, GameMode> GAMEMODES = new TreeMap<>();
    private static final Map<TierSourceFactory.TierSources, Map<String, GameMode>> CACHE = new ConcurrentHashMap<>();

    private static final java.util.concurrent.atomic.AtomicBoolean LOADING = new java.util.concurrent.atomic.AtomicBoolean(
            false);

    public static void init() {
        updateModes();
    }

    public static CompletableFuture<Void> updateModes() {
        TierSourceFactory.TierSources source = ModConfig.getTierSource();

        if (CACHE.containsKey(source)) {
            GAMEMODES = new TreeMap<>(CACHE.get(source));
            return CompletableFuture.completedFuture(null);
        }

        LOADING.set(true);
        return TierAPI.getAllModes().thenAccept(modesObject -> {
            if (modesObject == null) {
                LOADING.set(false);
                return;
            }

            CACHE.put(source, modesObject);
            GAMEMODES = new TreeMap<>(modesObject);
            LOADING.set(false);
        });
    }

    public static GameMode getFromInt(int i) {
        if (LOADING.get())
            return null;

        ArrayList<GameMode> list = new ArrayList<>(GAMEMODES.values());
        if (list.isEmpty() || i >= list.size())
            return null;
        return list.get(i);
    }

    public static int getGameModeSize() {
        return GAMEMODES.size();
    }

    public static boolean isLoading() {
        return LOADING.get();
    }

    public static GameMode getFromName(String gameMode) {
        if (LOADING.get())
            return null;

        for (Map.Entry<String, GameMode> entry : GAMEMODES.entrySet()) {
            if (entry.getKey().equalsIgnoreCase(gameMode))
                return entry.getValue();
        }

        for (Map.Entry<String, GameMode> entry : GAMEMODES.entrySet()) {
            if (isSameModeName(entry.getKey(), gameMode) || isSameModeName(entry.getValue().name(), gameMode))
                return entry.getValue();
        }

        for (Map<String, GameMode> modes : CACHE.values()) {
            for (Map.Entry<String, GameMode> entry : modes.entrySet()) {
                if (entry.getKey().equalsIgnoreCase(gameMode) || entry.getValue().name().equalsIgnoreCase(gameMode))
                    return entry.getValue();
            }
        }

        for (Map<String, GameMode> modes : CACHE.values()) {
            for (Map.Entry<String, GameMode> entry : modes.entrySet()) {
                if (isSameModeName(entry.getKey(), gameMode) || isSameModeName(entry.getValue().name(), gameMode))
                    return entry.getValue();
            }
        }

        return null;
    }

    private static final Set<Set<String>> MODE_ALIASES = Set.of(
            Set.of("vanilla", "crystal"),
            Set.of("nethpot", "nethop", "neth_pot", "netherpot")
    );

    private static boolean isSameModeName(String a, String b) {
        if (a.equalsIgnoreCase(b)) return true;
        String la = a.toLowerCase();
        String lb = b.toLowerCase();
        for (Set<String> group : MODE_ALIASES) {
            if (group.contains(la) && group.contains(lb)) return true;
        }
        return false;
    }

    public static GameMode getFromNameForSource(String gameMode, TierSourceFactory.TierSources source) {
        Map<String, GameMode> modes = CACHE.get(source);
        if (modes == null) return null;
        for (Map.Entry<String, GameMode> entry : modes.entrySet()) {
            if (entry.getKey().equalsIgnoreCase(gameMode))
                return entry.getValue();
        }
        return null;
    }

    public static CompletableFuture<Void> loadModesForSource(TierSourceFactory.TierSources source) {
        if (CACHE.containsKey(source)) {
            return CompletableFuture.completedFuture(null);
        }
        return TierSourceFactory.getTierSource(source).getAllModes().thenAccept(modes -> {
            if (modes != null) {
                CACHE.put(source, modes);
            }
        });
    }
}
