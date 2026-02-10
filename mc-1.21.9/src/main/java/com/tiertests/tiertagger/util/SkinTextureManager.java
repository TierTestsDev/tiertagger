package com.tiertests.tiertagger.util;

import com.mojang.blaze3d.platform.NativeImage;
import com.tiertests.tiertagger.TierTaggerCommon;
import com.tiertests.tiertagger.api.TierAPI;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.resources.Identifier;
import org.joml.Matrix3x2fStack;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URI;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class SkinTextureManager {

    private static final String BASE_URL = "https://skins.mcstats.com/";
    private static final Map<String, Identifier> cache = new ConcurrentHashMap<>();
    private static final Map<String, int[]> dimensions = new ConcurrentHashMap<>();
    private static final Set<String> pending = ConcurrentHashMap.newKeySet();
    private static final Set<String> failed = ConcurrentHashMap.newKeySet();

    public static void fetchBodyFront(UUID uuid) {
        String key = "body_" + noDashes(uuid);
        fetch(BASE_URL + "body/front/" + noDashes(uuid), key);
    }

    public static void fetchSkull(String nameOrUuid) {
        String key = "skull_" + nameOrUuid.toLowerCase();
        fetch(BASE_URL + "skull/" + nameOrUuid, key);
    }

    public static boolean hasBodyFront(UUID uuid) {
        return cache.containsKey("body_" + noDashes(uuid));
    }

    public static boolean hasSkull(String nameOrUuid) {
        return cache.containsKey("skull_" + nameOrUuid.toLowerCase());
    }

    public static void renderBodyFront(GuiGraphics graphics, UUID uuid, int x, int y, int maxW, int maxH) {
        render(graphics, "body_" + noDashes(uuid), x, y, maxW, maxH);
    }

    public static void renderSkull(GuiGraphics graphics, String nameOrUuid, int x, int y, int size) {
        render(graphics, "skull_" + nameOrUuid.toLowerCase(), x, y, size, size);
    }

    private static void render(GuiGraphics graphics, String key, int x, int y, int maxW, int maxH) {
        Identifier loc = cache.get(key);
        int[] dims = dimensions.get(key);
        if (loc == null || dims == null) return;

        float scale = Math.min((float) maxW / dims[0], (float) maxH / dims[1]);
        int drawW = (int) (dims[0] * scale);
        int drawH = (int) (dims[1] * scale);
        int drawX = x + (maxW - drawW) / 2;
        int drawY = y + (maxH - drawH) / 2;

        Matrix3x2fStack pose = graphics.pose();
        pose.pushMatrix();
        pose.translate(drawX, drawY);
        pose.scale(scale, scale);
        graphics.blit(RenderPipelines.GUI_TEXTURED, loc, 0, 0, 0, 0, dims[0], dims[1], dims[0], dims[1]);
        pose.popMatrix();
    }

    private static void fetch(String url, String key) {
        if (cache.containsKey(key) || pending.contains(key) || failed.contains(key)) return;
        pending.add(key);

        TierAPI.getExecutor().submit(() -> {
            try {
                HttpURLConnection conn = (HttpURLConnection) new URI(url).toURL().openConnection();
                conn.setRequestMethod("GET");
                conn.setRequestProperty("User-Agent", "TierTagger Minecraft Mod");
                conn.setRequestProperty("Accept", "image/png");
                conn.setInstanceFollowRedirects(true);
                conn.setConnectTimeout(5000);
                conn.setReadTimeout(5000);

                int code = conn.getResponseCode();
                if (code != 200) {
                    TierTaggerCommon.LOGGER.warn("Skin fetch returned {} for {}", code, url);
                    failed.add(key);
                    pending.remove(key);
                    return;
                }

                try (InputStream is = conn.getInputStream()) {
                    ByteArrayOutputStream baos = new ByteArrayOutputStream();
                    byte[] buf = new byte[8192];
                    int n;
                    while ((n = is.read(buf)) != -1) baos.write(buf, 0, n);
                    byte[] data = baos.toByteArray();
                    NativeImage image = NativeImage.read(new ByteArrayInputStream(data));
                    int w = image.getWidth();
                    int h = image.getHeight();

                    Minecraft.getInstance().execute(() -> {
                        DynamicTexture tex = new DynamicTexture(() -> "tiertagger_" + key, image);
                        Identifier loc = Identifier.fromNamespaceAndPath("tiertagger", "dynamic/" + key);
                        Minecraft.getInstance().getTextureManager().register(loc, tex);
                        cache.put(key, loc);
                        dimensions.put(key, new int[]{w, h});
                        pending.remove(key);
                    });
                }
            } catch (Exception e) {
                TierTaggerCommon.LOGGER.warn("Failed to fetch skin: {}", e.getMessage());
                failed.add(key);
                pending.remove(key);
            }
        });
    }

    public static void clearCache() {
        cache.values().forEach(loc ->
                Minecraft.getInstance().getTextureManager().release(loc));
        cache.clear();
        dimensions.clear();
        pending.clear();
        failed.clear();
    }

    private static String noDashes(UUID uuid) {
        return uuid.toString().replace("-", "");
    }
}
