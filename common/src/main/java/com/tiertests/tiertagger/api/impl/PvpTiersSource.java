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

public class PvpTiersSource extends TierSource {
    private final Type modesType = new TypeToken<Map<String, Map<String, JsonElement>>>() {
    }.getType();

    @Override
    public String getName() {
        return "PVPTiers";
    }

    @Override
    public String getBase() {
        return "https://pvptiers.com/api";
    }

    @Override
    public CompletableFuture<PlayerData> fetchPlayerData(UUID uuid, String name) {
        return fetchPlayerDataByIGN(name);
    }

    private CompletableFuture<PlayerData> fetchPlayerDataByIGN(String ign) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                PlayerData playerData = new PlayerData();

                String data = makeRequest("GET", "/search_profile/" + ign, null);
                if (data == null)
                    return null;

                JsonObject dataObj = JsonParser.parseString(data).getAsJsonObject();

                if (dataObj.has("region") && !dataObj.get("region").isJsonNull()) {
                    JsonElement region = dataObj.get("region");
                    String regionStr = region.getAsString();

                    playerData.setRegion(new Region(regionStr, regionStr));
                }

                if (dataObj.has("rankings")) {
                    JsonObject rankingsObj = dataObj.get("rankings").getAsJsonObject();
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
                }

                if (dataObj.has("badges")) {
                    JsonArray badge = dataObj.getAsJsonArray("badges");
                    if (badge != null && !badge.isEmpty()) {
                        JsonObject badgeObj = badge.get(0).getAsJsonObject();
                        String badgeStr = badgeObj.get("title").getAsString();
                        badgeStr = StringUtils.sanitize(badgeStr);
                        playerData.setBadge(badgeStr);
                    } else {
                        playerData.setBadge("");
                    }
                } else {
                    playerData.setBadge("");
                }

                if (dataObj.has("overall") && !dataObj.get("overall").isJsonNull()) {
                    playerData.setRank(dataObj.get("overall").getAsInt());
                    if (dataObj.has("points")) {
                        playerData.setPoints(dataObj.get("points").getAsInt());
                    }
                }

                playerData.setLastUpdated(System.currentTimeMillis());

                return playerData;
            } catch (Exception ex) {
                TierTaggerCommon.LOGGER.error("Error fetching player data from PVPTiers", ex);
            }
            return null;
        }, executor);
    }

    @Override
    public CompletableFuture<Map<String, GameMode>> getAllModes() {
        return CompletableFuture.supplyAsync(() -> {
            Map<String, GameMode> gamemodes = new HashMap<>();

            gamemodes.put("Sword", new GameMode("Sword", "sword", "#a4fdf0", "\uE706"));
            gamemodes.put("UHC", new GameMode("UHC", "uhc", "#FF5555", "\uE707"));
            gamemodes.put("Pot", new GameMode("Pot", "pot", "#ff0000", "\uE704"));
            gamemodes.put("SMP", new GameMode("SMP", "smp", "#eccb45", "\uE705"));
            gamemodes.put("Axe", new GameMode("Axe", "axe", "#55FF55", "\uE701"));
            gamemodes.put("NetherPot", new GameMode("NetherPot", "netherpot", "#7d4a40", "\uE703"));
            gamemodes.put("Diamond", new GameMode("Diamond", "diamond", "#55FFFF", ""));
            gamemodes.put("Crystal", new GameMode("Crystal", "crystal", "#FF55FF", ""));
            gamemodes.put("Vanilla", new GameMode("Vanilla", "vanilla", "#FF55FF", "\uE708"));

            return gamemodes;
        }, executor);
    }
}
