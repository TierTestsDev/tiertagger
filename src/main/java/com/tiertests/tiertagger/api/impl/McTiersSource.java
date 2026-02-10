package com.tiertests.tiertagger.api.impl;

import com.tiertests.tiertagger.TierTaggerCommon;
import com.tiertests.tiertagger.api.TierSource;
import com.tiertests.tiertagger.data.*;
import com.google.common.reflect.TypeToken;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.tiertests.tiertagger.util.StringUtils;

import java.lang.reflect.Type;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

public class McTiersSource extends TierSource {
    private final Type modesType = new TypeToken<Map<String, Map<String, JsonElement>>>() {
    }.getType();

    @Override
    public String getName() {
        return "MCTiers";
    }

    @Override
    public String getBase() {
        return "https://mctiers.com/api/v2";
    }

    @Override
    public CompletableFuture<PlayerData> fetchPlayerData(UUID uuid, String name) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                PlayerData playerData = new PlayerData();

                String data = makeRequest("GET", "/profile/" + uuid, null);
                if (data == null || data.isEmpty())
                    return null;

                if (!data.trim().startsWith("{") && !data.trim().startsWith("[")) {
                    TierTaggerCommon.LOGGER.error("Invalid JSON response from MCTiers API: {}", data.substring(0, Math.min(100, data.length())));
                    return null;
                }

                JsonObject dataObj = JsonParser.parseString(data).getAsJsonObject();

                JsonElement regionEl = dataObj.get("region");
                if (regionEl != null && !regionEl.isJsonNull()) {
                    String regionStr = regionEl.getAsString();
                    playerData.setRegion(new Region(regionStr, regionStr));
                }

                JsonElement rankingsEl = dataObj.get("rankings");
                if (rankingsEl == null || rankingsEl.isJsonNull()) return playerData;
                JsonObject rankingsObj = rankingsEl.getAsJsonObject();
                Map<String, Map<String, JsonElement>> rankings = gson.fromJson(rankingsObj, modesType);
                if (rankings != null) {
                    for (Map.Entry<String, Map<String, JsonElement>> rank : rankings.entrySet()) {
                        String gameModeStr = rank.getKey();
                        Map<String, JsonElement> obj = rank.getValue();

                        String tierStr = (obj.get("pos").getAsInt() == 1 ? "L" : "H") + "T"
                                + obj.get("tier").getAsInt();

                        GameMode mode = GameModeManager.getFromName(gameModeStr);
                        Tier tier = Tier.fromString(tierStr);

                        if (mode == null || tier == null)
                            continue;

                        playerData.addTier(mode, tier);
                    }
                }

                JsonArray badge = dataObj.getAsJsonArray("badges");
                if (badge != null && !badge.isEmpty()) {
                    JsonObject badgeObj = badge.get(0).getAsJsonObject();
                    String badgeStr = badgeObj.get("title").getAsString();
                    badgeStr = StringUtils.sanitize(badgeStr);
                    playerData.setBadge(badgeStr);
                } else {
                    playerData.setBadge("");
                }

                JsonElement ranking = dataObj.get("overall");
                if (ranking != null && !ranking.isJsonNull()) {
                    playerData.setRank(ranking.getAsInt());
                    JsonElement pointsEl = dataObj.get("points");
                    if (pointsEl != null && !pointsEl.isJsonNull()) {
                        playerData.setPoints(pointsEl.getAsInt());
                    }
                }

                playerData.setLastUpdated(System.currentTimeMillis());

                return playerData;
            } catch (Exception ex) {
                TierTaggerCommon.LOGGER.error("Error fetching player data from MCTiers", ex);
            }
            return null;
        }, executor);
    }

    @Override
    public CompletableFuture<Map<String, GameMode>> getAllModes() {
        return CompletableFuture.supplyAsync(() -> {
            try {
                String modesResult = makeRequest("GET", "/mode/list", null);
                if (modesResult == null) {
                    TierTaggerCommon.LOGGER.error("Modes not found. Try relaunching again soon.");
                    return null;
                }

                Map<String, Map<String, JsonElement>> modes = gson.fromJson(modesResult, modesType);

                if (modes == null || modes.isEmpty()) {
                    TierTaggerCommon.LOGGER.error("Modes are empty for MC Tiers.");
                    return null;
                }

                Map<String, GameMode> gamemodes = new HashMap<>();
                for (Map.Entry<String, Map<String, JsonElement>> entry : modes.entrySet()) {
                    String mode = entry.getKey();
                    Map<String, JsonElement> data = entry.getValue();

                    String title = data.get("title").getAsString();
                    String modeKey = mode.toLowerCase();
                    String icon = icons.getOrDefault(modeKey, "");
                    String color = modeColors.getOrDefault(modeKey, "#FFFFFF");
                    GameMode gamemode = new GameMode(title, modeKey, color, icon);

                    gamemodes.put(mode, gamemode);
                }

                return gamemodes;
            } catch (Exception e) {
                TierTaggerCommon.LOGGER.error("Error fetching game modes from MCTiers", e);
                return null;
            }
        }, executor);
    }
}
