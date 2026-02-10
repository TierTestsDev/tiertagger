package com.tiertests.tiertagger.api.impl;

import com.tiertests.tiertagger.TierTaggerCommon;
import com.tiertests.tiertagger.api.TierSource;
import com.tiertests.tiertagger.data.*;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.tiertests.tiertagger.util.StringUtils;

import java.util.*;
import java.util.concurrent.CompletableFuture;

public class TierTestsSource extends TierSource {
    @Override
    public String getName() {
        return "TierTests";
    }

    @Override
    public String getBase() {
        return "https://api.tiertests.com/v1";
    }

    @Override
    public CompletableFuture<PlayerData> fetchPlayerData(UUID uuid, String name) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                PlayerData playerData = new PlayerData();

                String data = makeRequest("GET", "/tiers/current/all?minecraftUuid=" + uuid + "&version=MODERN", null);
                if (data == null)
                    return null;

                JsonObject response = JsonParser.parseString(data).getAsJsonObject();
                if (response.isJsonNull() || !response.get("success").getAsBoolean())
                    return null;

                JsonArray dataObj = response.getAsJsonArray("data");
                if (dataObj == null || dataObj.isEmpty()) return null;

                JsonObject firstEntry = dataObj.get(0).getAsJsonObject();
                JsonObject userObj = firstEntry.getAsJsonObject("user");
                JsonElement regionElement = userObj != null ? userObj.get("region") : null;
                if (regionElement != null && !regionElement.isJsonNull()) {
                    String regionStr = regionElement.getAsString();
                    Region region = new Region(regionStr, regionStr);
                    playerData.setRegion(region);
                }

                for (JsonElement element : dataObj.asList()) {
                    JsonObject obj = element.getAsJsonObject();

                    if (!obj.get("gamemode").getAsJsonObject().get("version").getAsString().equalsIgnoreCase("MODERN"))
                        continue;

                    String gameModeStr = obj.get("gamemode").getAsJsonObject().get("name").getAsString();
                    String tierStr = obj.get("tier").getAsString();

                    GameMode mode = GameModeManager.getFromName(gameModeStr);
                    Tier tier = Tier.fromString(tierStr);
                    if (mode == null || tier == null)
                        continue;

                    playerData.addTier(mode, tier);
                }

                JsonObject badge = firstEntry.getAsJsonObject("badge");
                if (badge != null) {
                    String badgeColor = badge.get("legacyColor").getAsString();
                    String badgeEmoji = badge.get("emoji").getAsString();
                    badgeColor = StringUtils.sanitize(badgeColor);
                    String colorized = "";
                    if (!badgeColor.isBlank()) {
                        colorized = "&" + badgeColor;
                        colorized = colorized
                                .replace("§§", "§")
                                .replace("&&", "&");
                        colorized = StringUtils.colorize(colorized);
                    }
                    badgeEmoji = StringUtils.sanitize(badgeEmoji);
                    playerData.setBadge(colorized + badgeEmoji);
                }

                JsonObject rank = firstEntry.getAsJsonObject("rankModern");
                if (rank != null) {
                    int rankNum = rank.get("rank").getAsInt();
                    playerData.setRank(rankNum);
                    int points = rank.get("points").getAsInt();
                    playerData.setPoints(points);
                }

                playerData.setLastUpdated(System.currentTimeMillis());

                JsonElement userElement = firstEntry.get("user");
                if (userElement != null && !userElement.isJsonNull()) {
                    JsonElement discordIdEl = userElement.getAsJsonObject().get("discordId");
                    if (discordIdEl != null && !discordIdEl.isJsonNull()) {
                        long discordId = discordIdEl.getAsLong();
                        fetchPeakTiersAsync(playerData, discordId);
                    }
                }

                return playerData;
            } catch (Exception ex) {
                TierTaggerCommon.LOGGER.error("Error fetching player data from TierTests", ex);
            }
            return null;
        }, executor);
    }

    private void fetchPeakTiersAsync(PlayerData playerData, long discordId) {
        for (Map.Entry<GameMode, Tier> entry : playerData.getTiers().entrySet()) {
            GameMode mode = entry.getKey();
            CompletableFuture.runAsync(() -> {
                try {
                    String endpoint = "/tiers/history/" + discordId + "/" + mode.name();
                    String historyData = makeRequest("GET", endpoint, null);
                    if (historyData == null) return;

                    JsonObject historyResponse = JsonParser.parseString(historyData).getAsJsonObject();
                    if (historyResponse.isJsonNull() || !historyResponse.get("success").getAsBoolean()) return;

                    JsonArray historyArray = historyResponse.getAsJsonArray("data");
                    if (historyArray == null || historyArray.isEmpty()) return;

                    Tier peak = null;
                    for (JsonElement el : historyArray) {
                        JsonObject audit = el.getAsJsonObject();
                        String tierStr = audit.get("tier").getAsString();
                        Tier tier = Tier.fromString(tierStr);
                        if (tier == null) continue;
                        if (peak == null || tier.ordinal() < peak.ordinal()) {
                            peak = tier;
                        }
                    }

                    if (peak != null) {
                        playerData.addPeakTier(mode, peak);
                    }
                } catch (Exception e) {
                    TierTaggerCommon.LOGGER.error("Error fetching peak tier for {}/{}", discordId, mode.name(), e);
                }
            }, executor);
        }
    }

    @Override
    public CompletableFuture<Map<String, GameMode>> getAllModes() {
        return CompletableFuture.supplyAsync(() -> {
            try {
                String modesResult = makeRequest("GET", "/gamemodes/modern", null);
                if (modesResult == null) {
                    TierTaggerCommon.LOGGER.error("Modes not found. Try relaunching again soon.");
                    return null;
                }

                JsonObject modesObject = JsonParser.parseString(modesResult).getAsJsonObject();
                if (modesObject == null)
                    return null;

                JsonArray modes = modesObject.getAsJsonArray("data");
                Map<String, GameMode> gamemodes = new HashMap<>();

                for (JsonElement element : modes) {
                    JsonObject data = element.getAsJsonObject();
                    if (data == null)
                        continue;

                    String name = data.get("name").getAsString();
                    String color = data.get("color").getAsString();
                    String icon = data.get("unicode").getAsString();
                    String prefix = data.get("beautifiedName").getAsString();
                    String hexColor = color != null ? com.tiertests.tiertagger.util.Colors.getFromName(color).getHex() : "#FFFFFF";
                    GameMode gamemode = new GameMode(name, prefix, hexColor, icon);
                    gamemodes.put(name, gamemode);
                }

                return gamemodes;
            } catch (Exception e) {
                TierTaggerCommon.LOGGER.error("Error fetching game modes from TierTests", e);
                return null;
            }
        }, executor);
    }
}
