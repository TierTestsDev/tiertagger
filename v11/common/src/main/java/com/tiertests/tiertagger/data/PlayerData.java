package com.tiertests.tiertagger.data;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;

import java.util.HashMap;
import java.util.Map;

@Getter
@Setter
@RequiredArgsConstructor
public class PlayerData {
    private final Map<GameMode, Tier> tiers = new HashMap<>();
    private final Map<GameMode, Tier> peakTiers = new HashMap<>();
    private Region region;
    private String badge;
    private long lastUpdated;
    private int rank;
    private int points;

    public void addTier(GameMode gamemode, Tier tier) {
        tiers.put(gamemode, tier);
    }

    public void addPeakTier(GameMode gamemode, Tier tier) {
        peakTiers.put(gamemode, tier);
    }

    public Tier getPeakTier(GameMode gamemode) {
        return peakTiers.get(gamemode);
    }

    public Tier getTier(GameMode gamemode) {
        return tiers.get(gamemode);
    }

    public Tier getHighestTier() {
        Tier highest = null;
        for (Tier tier : tiers.values()) {
            if (tier == null)
                continue;
            if (highest == null || tier.ordinal() < highest.ordinal()) {
                highest = tier;
            }
        }
        return highest;
    }

    public GameMode getHighestTierGamemode() {
        Tier highest = null;
        GameMode highestGameMode = null;
        for (Map.Entry<GameMode, Tier> entry : tiers.entrySet()) {
            Tier tier = entry.getValue();
            if (tier == null)
                continue;
            if (highest == null || tier.ordinal() < highest.ordinal()) {
                highest = tier;
                highestGameMode = entry.getKey();
            }
        }
        return highestGameMode;
    }

    public boolean isEmpty() {
        return tiers.isEmpty();
    }
}
