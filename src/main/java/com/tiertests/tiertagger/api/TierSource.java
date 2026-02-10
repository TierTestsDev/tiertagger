package com.tiertests.tiertagger.api;

import com.tiertests.tiertagger.data.GameMode;
import com.tiertests.tiertagger.data.NameResult;
import com.tiertests.tiertagger.data.PlayerData;
import com.tiertests.tiertagger.data.PlayerResult;
import com.tiertests.tiertagger.manager.TierManager;
import com.google.gson.Gson;
import com.google.gson.JsonObject;

import com.tiertests.tiertagger.TierTaggerCommon;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;

public abstract class TierSource {
    protected final ExecutorService executor = TierAPI.getExecutor();
    protected static final Gson gson = new Gson();
    protected static final Map<String, NameResult> nameCache = new ConcurrentHashMap<>();

    protected Map<String, String> icons = Map.ofEntries(
            Map.entry("axe", "\uE701"),
            Map.entry("mace", "\uE702"),
            Map.entry("neth_pot", "\uE703"),
            Map.entry("nethop", "\uE703"),
            Map.entry("pot", "\uE704"),
            Map.entry("smp", "\uE705"),
            Map.entry("sword", "\uE706"),
            Map.entry("uhc", "\uE707"),
            Map.entry("vanilla", "\uE708")
    );

    protected Map<String, String> modeColors = Map.ofEntries(
            Map.entry("axe", "#55FF55"),
            Map.entry("mace", "#AAAAAA"),
            Map.entry("neth_pot", "#7d4a40"),
            Map.entry("nethop", "#7d4a40"),
            Map.entry("pot", "#ff0000"),
            Map.entry("smp", "#eccb45"),
            Map.entry("sword", "#a4fdf0"),
            Map.entry("uhc", "#FF5555"),
            Map.entry("vanilla", "#FF55FF")
    );

    public abstract String getName();

    public abstract String getBase();

    public abstract CompletableFuture<PlayerData> fetchPlayerData(UUID uuid, String name);

    public abstract CompletableFuture<Map<String, GameMode>> getAllModes();

    protected CompletableFuture<PlayerResult> fetchPlayerDataByName(String username) {
        return fetchPlayerDataByName(username, false);
    }

    public CompletableFuture<PlayerResult> fetchPlayerDataByNameFresh(String username) {
        return fetchPlayerDataByName(username, true);
    }

    private CompletableFuture<PlayerResult> fetchPlayerDataByName(String username, boolean skipCache) {
        String lowerName = username.toLowerCase();
        NameResult cachedName = nameCache.get(lowerName);
        if (cachedName != null) {
            if (!skipCache) {
                PlayerData cachedData = TierManager.getCachedData(cachedName.uuid());
                if (cachedData != null) {
                    return CompletableFuture.completedFuture(new PlayerResult(cachedName.name(), cachedName.uuid(), cachedData));
                }
            }
            return fetchPlayerData(cachedName.uuid(), username).thenApply(data -> {
                return new PlayerResult(cachedName.name(), cachedName.uuid(), data);
            });
        }

        return getUUID(username).thenCompose(result -> {
            if (result == null) {
                return CompletableFuture.completedFuture(null);
            }
            if (!skipCache) {
                PlayerData cachedData = TierManager.getCachedData(result.uuid());
                if (cachedData != null) {
                    return CompletableFuture.completedFuture(new PlayerResult(result.name(), result.uuid(), cachedData));
                }
            }
            return fetchPlayerData(result.uuid(), username).thenApply(data -> {
                return new PlayerResult(result.name(), result.uuid(), data);
            });
        });
    }

    protected String makeRequest(String method, String endpoint, String body) {
        String fullUrl = getBase() + endpoint;

        try {
            URL url = URI.create(fullUrl).toURL();

            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod(method);
            conn.setRequestProperty("User-Agent", "TierTagger/" + TierTaggerCommon.VERSION);
            conn.setRequestProperty("Accept", "application/json");
            conn.setConnectTimeout(15000);
            conn.setReadTimeout(15000);

            if (body != null) {
                conn.setRequestProperty("Content-Type", "application/json");
                conn.setDoOutput(true);
                try (OutputStream os = conn.getOutputStream()) {
                    byte[] input = body.getBytes(StandardCharsets.UTF_8);
                    os.write(input, 0, input.length);
                }
            }

            int responseCode = conn.getResponseCode();
            if (responseCode < 200 || responseCode >= 300) {
                conn.disconnect();
                return null;
            }

            InputStream stream = conn.getInputStream();
            if (stream == null) {
                conn.disconnect();
                return null;
            }

            try (BufferedReader reader = new BufferedReader(
                    new InputStreamReader(stream, StandardCharsets.UTF_8))) {
                StringBuilder response = new StringBuilder();
                String line;
                while ((line = reader.readLine()) != null) {
                    response.append(line);
                }
                return response.toString();
            } finally {
                conn.disconnect();
            }
        } catch (Exception e) {
            TierTaggerCommon.LOGGER.debug("Request error for {}{}: {}", getBase(), endpoint, e.getMessage());
            return null;
        }
    }

    protected CompletableFuture<NameResult> getUUID(String username) {
        final String lowerName = username.toLowerCase();
        if (nameCache.containsKey(lowerName)) {
            return CompletableFuture.completedFuture(nameCache.get(lowerName));
        }

        return CompletableFuture.supplyAsync(() -> {
            try {
                String url = "https://api.mojang.com/users/profiles/minecraft/" + username;
                URL mojangUrl = URI.create(url).toURL();
                HttpURLConnection conn = (HttpURLConnection) mojangUrl.openConnection();
                conn.setRequestMethod("GET");
                conn.setConnectTimeout(5000);
                conn.setReadTimeout(5000);

                if (conn.getResponseCode() != 200) {
                    return null;
                }

                try (BufferedReader reader = new BufferedReader(new InputStreamReader(conn.getInputStream()))) {
                    JsonObject obj = gson.fromJson(reader, JsonObject.class);
                    String correctName = obj.get("name").getAsString();
                    String id = obj.get("id").getAsString();
                    String dashed = id.replaceFirst(
                            "(\\p{XDigit}{8})(\\p{XDigit}{4})(\\p{XDigit}{4})(\\p{XDigit}{4})(\\p{XDigit}{12})",
                            "$1-$2-$3-$4-$5");
                    UUID uuid = UUID.fromString(dashed);
                    NameResult result = new NameResult(correctName, uuid);
                    nameCache.put(lowerName, result);
                    return result;
                }
            } catch (Exception e) {
                return null;
            }
        }, executor);
    }
}
