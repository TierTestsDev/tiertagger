package com.tiertests.tiertagger.api;

import com.google.gson.*;
import com.tiertests.tiertagger.data.GameMode;
import com.tiertests.tiertagger.data.PlayerData;
import com.tiertests.tiertagger.data.PlayerResult;
import lombok.Getter;

import com.tiertests.tiertagger.TierTaggerCommon;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URL;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import com.tiertests.tiertagger.config.ModConfig;

public class TierAPI {
    @Getter
    private static final ExecutorService executor = Executors.newFixedThreadPool(4);
    private static final Gson gson = new Gson();

    @Getter
    private static String discordInvite = "https://discord.gg/tiertests";

    public static CompletableFuture<PlayerData> fetchPlayerData(UUID uuid, String name) {
        return TierSourceFactory.getTierSource(ModConfig.getTierSource()).fetchPlayerData(uuid, name);
    }

    public static CompletableFuture<Map<String, GameMode>> getAllModes() {
        return TierSourceFactory.getTierSource(ModConfig.getTierSource()).getAllModes();
    }

    public static CompletableFuture<PlayerResult> fetchPlayerDataByName(String username) {
        return TierSourceFactory.getTierSource(ModConfig.getTierSource()).fetchPlayerDataByName(username);
    }

    public static CompletableFuture<Map<TierSourceFactory.TierSources, PlayerResult>> fetchAllSourcesPlayerData(String username) {
        Map<TierSourceFactory.TierSources, CompletableFuture<PlayerResult>> futures = new LinkedHashMap<>();
        for (TierSourceFactory.TierSources source : TierSourceFactory.TierSources.values()) {
            TierSource tierSource = TierSourceFactory.getTierSource(source);
            if (tierSource != null) {
                futures.put(source, tierSource.fetchPlayerDataByNameFresh(username).exceptionally(ex -> {
                    TierTaggerCommon.LOGGER.error("Error fetching from {}: {}", source.getDisplayName(), ex.getMessage());
                    return null;
                }));
            }
        }

        return CompletableFuture.allOf(futures.values().toArray(new CompletableFuture[0]))
                .thenApply(v -> {
                    Map<TierSourceFactory.TierSources, PlayerResult> results = new LinkedHashMap<>();
                    for (Map.Entry<TierSourceFactory.TierSources, CompletableFuture<PlayerResult>> entry : futures.entrySet()) {
                        try {
                            PlayerResult result = entry.getValue().join();
                            if (result != null && result.data() != null && !result.data().isEmpty()) {
                                results.put(entry.getKey(), result);
                            }
                        } catch (Exception ignored) {}
                    }
                    return results;
                });
    }

    private static String makeRequest(String endpoint) {
        HttpURLConnection conn = null;
        try {
            URL url = URI.create(endpoint).toURL();
            conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("GET");
            conn.setRequestProperty("User-Agent", "TierTagger/" + TierTaggerCommon.VERSION);
            conn.setRequestProperty("Accept", "application/json");
            conn.setConnectTimeout(15000);
            conn.setReadTimeout(15000);

            if (conn.getResponseCode() == 200) {
                try (BufferedReader in = new BufferedReader(new InputStreamReader(conn.getInputStream()))) {
                    StringBuilder response = new StringBuilder();
                    String line;
                    while ((line = in.readLine()) != null) {
                        response.append(line);
                    }
                    return response.toString();
                }
            }
        } catch (Exception e) {
            TierTaggerCommon.LOGGER.error("Request error for {}", endpoint, e);
        } finally {
            if (conn != null) conn.disconnect();
        }
        return null;
    }

    public static void refreshDiscordInvite() {
        fetchDiscordInvite().thenAccept(url -> {
            if (url != null && !url.isEmpty()) {
                discordInvite = url;
            }
        });
    }

    public static CompletableFuture<Boolean> checkUpdate(String currentVersion) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                String response = makeRequest("https://api.modrinth.com/v2/project/tiertests/version");
                if (response == null)
                    return false;

                JsonArray array = gson.fromJson(response, JsonArray.class);
                for (JsonElement element : array) {
                    JsonObject object = element.getAsJsonObject();
                    if (object == null) continue;
                    List<JsonElement> loaderElements = object.get("loaders").getAsJsonArray().asList();
                    boolean containsFabric = false;
                    for (JsonElement loaderElement : loaderElements) {
                        if (loaderElement.getAsString().equalsIgnoreCase("fabric")) {
                            containsFabric = true;
                            break;
                        }
                    }
                    if (!containsFabric) continue;

                    return object.has("version_number")
                            && !object.get("version_number").getAsString().equalsIgnoreCase(currentVersion);
                }
                return false;
            } catch (Exception e) {
                return false;
            }
        }, executor);
    }
    
    public static CompletableFuture<String> fetchDiscordInvite() {
        return CompletableFuture.supplyAsync(() -> {
            try {
                String response = makeRequest("https://api.tiertests.com/v1/config/modern.guild_invite");
                if (response == null) {
                    return null;
                }
                JsonObject obj = gson.fromJson(response, JsonObject.class);
                if (obj.has("url") && !obj.get("url").isJsonNull()) {
                    return obj.get("url").getAsString();
                }
                return null;
            } catch (Exception e) {
                return null;
            }
        }, executor);
    }

}
