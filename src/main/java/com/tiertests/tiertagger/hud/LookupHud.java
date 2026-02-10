package com.tiertests.tiertagger.hud;

import com.tiertests.tiertagger.api.TierAPI;
import com.tiertests.tiertagger.api.TierSource;
import com.tiertests.tiertagger.api.TierSourceFactory;
import com.tiertests.tiertagger.config.ModConfig;
import com.tiertests.tiertagger.data.GameMode;
import com.tiertests.tiertagger.data.PlayerData;
import com.tiertests.tiertagger.data.Tier;
import com.tiertests.tiertagger.manager.TierManager;
import com.tiertests.tiertagger.util.IconResolver;
import com.tiertests.tiertagger.util.SkinTextureManager;
import com.tiertests.tiertagger.util.StringUtils;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.player.LocalPlayer;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

public class LookupHud {

    private static final long DISPLAY_DURATION_MS = 6000;
    private static final int PANEL_PADDING = 8;
    private static final int PANEL_MARGIN = 6;

    private static String lookupName = null;
    private static PlayerData lookupData = null;
    private static UUID lookupUuid = null;
    private static long showStartTime = 0;
    private static boolean active = false;
    private static boolean fetching = false;

    public static void triggerLookup(AbstractClientPlayer target) {
        if (target == null) return;

        lookupUuid = target.getUUID();
        lookupName = target.getName().getString();
        fetching = true;
        active = true;
        lookupData = null;
        showStartTime = System.currentTimeMillis();

        SkinTextureManager.fetchBodyFront(lookupUuid);

        PlayerData cached = TierManager.getPlayerData(lookupUuid);
        if (cached != null && !cached.isEmpty()) {
            lookupData = cached;
            fetching = false;
            showStartTime = System.currentTimeMillis();
            return;
        }

        TierAPI.fetchPlayerData(lookupUuid, lookupName).thenAccept(data -> {
            if (data != null && !data.isEmpty()) {
                Minecraft.getInstance().execute(() -> {
                    lookupData = data;
                    TierManager.cachePlayerData(lookupUuid, data);
                    fetching = false;
                    showStartTime = System.currentTimeMillis();
                });
            } else {
                fetchBestFallbackSource(lookupUuid, lookupName);
            }
        });
    }

    private static void fetchBestFallbackSource(UUID uuid, String name) {
        CompletableFuture<?>[] futures = new CompletableFuture[TierSourceFactory.TierSources.values().length];
        PlayerData[] results = new PlayerData[futures.length];
        TierSourceFactory.TierSources[] sources = TierSourceFactory.TierSources.values();

        for (int i = 0; i < sources.length; i++) {
            if (sources[i] == ModConfig.getTierSource()) {
                futures[i] = CompletableFuture.completedFuture(null);
                continue;
            }
            TierSource src = TierSourceFactory.getTierSource(sources[i]);
            if (src == null) {
                futures[i] = CompletableFuture.completedFuture(null);
                continue;
            }
            final int idx = i;
            futures[i] = src.fetchPlayerData(uuid, name)
                    .thenAccept(d -> results[idx] = d)
                    .exceptionally(ex -> null);
        }

        CompletableFuture.allOf(futures).thenRun(() -> {
            PlayerData best = null;
            for (PlayerData d : results) {
                if (d == null || d.isEmpty()) continue;
                Tier highest = d.getHighestTier();
                if (highest == null) continue;
                if (best == null || highest.ordinal() < best.getHighestTier().ordinal()) {
                    best = d;
                }
            }
            final PlayerData finalBest = best;
            Minecraft.getInstance().execute(() -> {
                if (finalBest != null) {
                    lookupData = finalBest;
                    TierManager.cachePlayerData(uuid, finalBest);
                } else {
                    lookupData = null;
                }
                fetching = false;
                showStartTime = System.currentTimeMillis();
            });
        });
    }

    public static AbstractClientPlayer findClosestPlayer() {
        Minecraft mc = Minecraft.getInstance();
        LocalPlayer local = mc.player;
        if (local == null || mc.level == null) return null;

        AbstractClientPlayer closest = null;
        double closestDist = Double.MAX_VALUE;

        for (AbstractClientPlayer player : mc.level.players()) {
            if (player == local) continue;

            double dist = local.distanceTo(player);
            if (dist < closestDist) {
                closestDist = dist;
                closest = player;
            }
        }

        return closest;
    }

    public static void render(GuiGraphics graphics, DeltaTracker deltaTracker) {
        if (!active) return;

        long elapsed = System.currentTimeMillis() - showStartTime;

        if (fetching) {
            renderFetchingPanel(graphics);
            return;
        }

        if (elapsed > DISPLAY_DURATION_MS) {
            active = false;
            lookupData = null;
            lookupName = null;
            lookupUuid = null;
            return;
        }

        float progress = 1.0f - (float) elapsed / DISPLAY_DURATION_MS;
        renderPanel(graphics, progress);
    }

    private static void renderFetchingPanel(GuiGraphics graphics) {
        Minecraft mc = Minecraft.getInstance();
        int x = PANEL_MARGIN;
        int y = PANEL_MARGIN;

        String text = "§fLooking up §e" + lookupName + "§f...";
        int textWidth = mc.font.width(StringUtils.stripColorCodes(text));
        int panelWidth = Math.max(160, textWidth + PANEL_PADDING * 2);
        int panelHeight = 36;

        graphics.fill(x, y, x + panelWidth, y + panelHeight, 0xCC1a1a2e);
        renderBorder(graphics, x, y, panelWidth, panelHeight, 0xFF6c5ce7);

        graphics.drawString(mc.font, text, x + PANEL_PADDING, y + PANEL_PADDING + 4, 0xFFFFFFFF, true);
    }

    private static void renderPanel(GuiGraphics graphics, float progress) {
        Minecraft mc = Minecraft.getInstance();

        if (lookupData == null) {
            int x = PANEL_MARGIN;
            int y = PANEL_MARGIN;

            String noTierText = "§c" + lookupName + " §7- No tiers found";
            int noTierTextWidth = mc.font.width(StringUtils.stripColorCodes(noTierText));
            int panelWidth = Math.max(160, noTierTextWidth + PANEL_PADDING * 2);
            int panelHeight = 36;

            graphics.fill(x, y, x + panelWidth, y + panelHeight, 0xCC1a1a2e);
            renderBorder(graphics, x, y, panelWidth, panelHeight, 0xFFe74c3c);

            graphics.drawString(mc.font, noTierText, x + PANEL_PADDING, y + PANEL_PADDING + 4, 0xFFFFFFFF, true);

            int barY = y + panelHeight - 3;
            int barWidth = (int) (panelWidth * progress);
            graphics.fill(x, barY, x + barWidth, barY + 3, 0xFFe74c3c);
            return;
        }

        int tierCount = lookupData.getTiers().size();
        int entityAreaWidth = 46;

        int minTextWidth = 130;
        String nameStr = "§a" + lookupName;
        if (ModConfig.isShowRegion() && lookupData.getRegion() != null) {
            nameStr += " §8[" + lookupData.getRegion().shortPrefix() + "§8]";
        }
        int nameWidth = mc.font.width(StringUtils.stripColorCodes(nameStr));

        String source = ModConfig.getTierSource().getDisplayName();
        String rankStr = "§7#" + lookupData.getRank() + " §8(" + source + ")";
        int rankWidth = mc.font.width(StringUtils.stripColorCodes(rankStr));

        int maxTierLineWidth = 0;
        for (Map.Entry<GameMode, Tier> entry : lookupData.getTiers().entrySet()) {
            GameMode mode = entry.getKey();
            Tier tier = entry.getValue();
            String tierName = ModConfig.isUseMCTiersFormat() ? tier.getMcTiersName() : tier.getDisplayName();
            String modeName = StringUtils.beautify(mode.name());
            String icon = IconResolver.resolve(mode);
            String iconPrefix = (icon != null && !icon.isEmpty()) ? icon + " " : "";
            Tier peakTier = lookupData.getPeakTier(mode);
            String peakSuffix = "";
            if (peakTier != null && peakTier.ordinal() < tier.ordinal()) {
                String peakName = ModConfig.isUseMCTiersFormat() ? peakTier.getMcTiersName() : peakTier.getDisplayName();
                peakSuffix = " §7(Peak: " + peakName + "§7)";
            }
            int lineWidth = mc.font.width(StringUtils.stripColorCodes(iconPrefix + modeName + " " + tierName + peakSuffix));
            if (lineWidth > maxTierLineWidth) maxTierLineWidth = lineWidth;
        }

        int textAreaWidth = Math.max(minTextWidth, Math.max(nameWidth, Math.max(rankWidth, maxTierLineWidth)) + 4);
        int panelWidth = entityAreaWidth + textAreaWidth + PANEL_PADDING;

        int headerHeight = 24;
        int tierLineHeight = 12;
        int tiersHeight = tierCount * tierLineHeight;
        int panelHeight = PANEL_PADDING + headerHeight + tiersHeight + PANEL_PADDING + 6;
        panelHeight = Math.max(panelHeight, 70);

        int x = PANEL_MARGIN;
        int y = PANEL_MARGIN;

        graphics.fill(x, y, x + panelWidth, y + panelHeight, 0xCC1a1a2e);
        renderBorder(graphics, x, y, panelWidth, panelHeight, 0xFF6c5ce7);

        if (lookupUuid != null) {
            if (SkinTextureManager.hasBodyFront(lookupUuid)) {
                int skinX = x + 2;
                int skinY = y + 4;
                int skinW = entityAreaWidth - 4;
                int skinH = panelHeight - PANEL_PADDING - 8;
                graphics.enableScissor(x + 2, y + 2, x + entityAreaWidth, y + panelHeight - 4);
                SkinTextureManager.renderBodyFront(graphics, lookupUuid, skinX, skinY, skinW, skinH);
                graphics.disableScissor();
            }
        }

        int textX = x + entityAreaWidth;
        int textY = y + PANEL_PADDING;

        graphics.drawString(mc.font, nameStr, textX, textY, 0xFFFFFFFF, true);
        graphics.drawString(mc.font, rankStr, textX, textY + 11, 0xFFFFFFFF, true);

        int tierY = textY + headerHeight;
        int count = 0;
        for (Map.Entry<GameMode, Tier> entry : lookupData.getTiers().entrySet()) {
            GameMode mode = entry.getKey();
            Tier tier = entry.getValue();

            String tierName = ModConfig.isUseMCTiersFormat() ? tier.getMcTiersName()
                    : tier.getDisplayName();

            String modeName = StringUtils.beautify(mode.name());
            String icon = IconResolver.resolve(mode);
            String iconPrefix = (icon != null && !icon.isEmpty()) ? icon + " " : "";
            int modeColor;
            if (IconResolver.isClassicIcon(mode)) {
                int classicColor = IconResolver.getClassicColor(mode);
                modeColor = (classicColor != -1 ? classicColor : mode.colorInt()) | 0xFF000000;
            } else {
                modeColor = mode.colorInt() | 0xFF000000;
            }

            Tier peakTier = lookupData.getPeakTier(mode);
            String peakSuffix = "";
            if (peakTier != null && peakTier.ordinal() < tier.ordinal()) {
                String peakName = ModConfig.isUseMCTiersFormat() ? peakTier.getMcTiersName()
                        : peakTier.getDisplayName();
                peakSuffix = " §7(Peak: " + peakName + "§7)";
            }

            int drawX = textX;
            int drawY = tierY + (count * tierLineHeight);
            if (!iconPrefix.isEmpty() && !IconResolver.isClassicIcon(mode)) {
                graphics.drawString(mc.font, iconPrefix, drawX, drawY, 0xFFFFFFFF, true);
                drawX += mc.font.width(iconPrefix);
                graphics.drawString(mc.font, modeName, drawX, drawY, modeColor, true);
                int nameOffset = mc.font.width(iconPrefix + modeName + " ");
                graphics.drawString(mc.font, tierName + peakSuffix, textX + nameOffset, drawY, 0xFFFFFFFF, true);
            } else {
                graphics.drawString(mc.font, iconPrefix + modeName, drawX, drawY, modeColor, true);
                int nameOffset = mc.font.width(iconPrefix + modeName + " ");
                graphics.drawString(mc.font, tierName + peakSuffix, textX + nameOffset, drawY, 0xFFFFFFFF, true);
            }
            count++;
        }

        int barY = y + panelHeight - 3;
        int barWidth = (int) (panelWidth * progress);
        graphics.fill(x, barY, x + barWidth, barY + 3, 0xFF6c5ce7);
    }

    private static void renderBorder(GuiGraphics graphics, int x, int y, int w, int h, int color) {
        graphics.fill(x, y, x + w, y + 1, color);           // top
        graphics.fill(x, y + h - 1, x + w, y + h, color);   // bottom
        graphics.fill(x, y, x + 1, y + h, color);            // left
        graphics.fill(x + w - 1, y, x + w, y + h, color);   // right
    }

    public static boolean isActive() {
        return active;
    }
}
