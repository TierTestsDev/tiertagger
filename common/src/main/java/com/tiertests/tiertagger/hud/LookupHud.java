package com.tiertests.tiertagger.hud;

import com.mojang.blaze3d.vertex.PoseStack;
import com.tiertests.tiertagger.api.TierAPI;
import com.tiertests.tiertagger.config.ModConfig;
import com.tiertests.tiertagger.data.GameMode;
import com.tiertests.tiertagger.data.PlayerData;
import com.tiertests.tiertagger.data.Tier;
import com.tiertests.tiertagger.manager.TierManager;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;

import com.tiertests.tiertagger.util.IconResolver;
import com.tiertests.tiertagger.util.StringUtils;

import java.util.Map;
import java.util.UUID;

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

        PlayerData cached = TierManager.getPlayerData(lookupUuid);
        if (cached != null && !cached.isEmpty()) {
            lookupData = cached;
            fetching = false;
            showStartTime = System.currentTimeMillis();
            return;
        }

        TierAPI.fetchPlayerData(lookupUuid, lookupName).thenAccept(data -> {
            Minecraft.getInstance().execute(() -> {
                if (data != null && !data.isEmpty()) {
                    lookupData = data;
                    TierManager.cachePlayerData(lookupUuid, data);
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
        int panelWidth = 160;
        int panelHeight = 36;

        graphics.fill(x, y, x + panelWidth, y + panelHeight, 0xCC1a1a2e);
        renderBorder(graphics, x, y, panelWidth, panelHeight, 0xFF6c5ce7);

        graphics.drawString(mc.font, "§fLooking up §e" + lookupName + "§f...", x + PANEL_PADDING, y + PANEL_PADDING + 4, 0xFFFFFFFF, true);
    }

    private static void renderPanel(GuiGraphics graphics, float progress) {
        Minecraft mc = Minecraft.getInstance();

        if (lookupData == null) {
            int x = PANEL_MARGIN;
            int y = PANEL_MARGIN;
            int panelWidth = 160;
            int panelHeight = 36;

            graphics.fill(x, y, x + panelWidth, y + panelHeight, 0xCC1a1a2e);
            renderBorder(graphics, x, y, panelWidth, panelHeight, 0xFFe74c3c);

            graphics.drawString(mc.font, "§c" + lookupName + " §7- No tiers found", x + PANEL_PADDING, y + PANEL_PADDING + 4, 0xFFFFFFFF, true);

            int barY = y + panelHeight - 3;
            int barWidth = (int) (panelWidth * progress);
            graphics.fill(x, barY, x + barWidth, barY + 3, 0xFFe74c3c);
            return;
        }

        int tierCount = lookupData.getTiers().size();
        int entityAreaWidth = 46;
        int textAreaWidth = 130;
        int panelWidth = entityAreaWidth + textAreaWidth + PANEL_PADDING;

        int headerHeight = 24;
        int tierLineHeight = 12;
        int tiersHeight = tierCount * tierLineHeight;
        int panelHeight = PANEL_PADDING + headerHeight + tiersHeight + PANEL_PADDING + 6;
        panelHeight = Math.max(panelHeight, 70);

        int x = PANEL_MARGIN;
        int y = PANEL_MARGIN;

        // Background
        graphics.fill(x, y, x + panelWidth, y + panelHeight, 0xCC1a1a2e);
        renderBorder(graphics, x, y, panelWidth, panelHeight, 0xFF6c5ce7);

        // Paperdoll
        AbstractClientPlayer entity = findEntityByUuid();
        if (entity != null) {
            int entityX = x + entityAreaWidth / 2;
            int entityY = y + panelHeight - PANEL_PADDING - 4;
            int entitySize = Math.min(30, panelHeight - 20);

            PoseStack pose = graphics.pose();
            pose.pushPose();

            // Clip area for the entity so it doesn't overflow
            graphics.enableScissor(x + 2, y + 2, x + entityAreaWidth, y + panelHeight - 4);

            InventoryScreen.renderEntityInInventoryFollowsMouse(
                    graphics,
                    entityX - 14, y + 4,
                    entityX + 14, entityY,
                    entitySize,
                    0.0625f,
                    entityX, entityY - entitySize,
                    entity
            );

            graphics.disableScissor();
            pose.popPose();
        }

        // Text area
        int textX = x + entityAreaWidth;
        int textY = y + PANEL_PADDING;

        // Player name + region
        String nameStr = "§a" + lookupName;
        if (lookupData.getRegion() != null) {
            nameStr += " §8[" + lookupData.getRegion().shortPrefix() + "§8]";
        }
        graphics.drawString(mc.font, nameStr, textX, textY, 0xFFFFFFFF, true);

        // Rank + source
        String source = ModConfig.getTierSource().getDisplayName();
        String rankStr = "§7#" + lookupData.getRank() + " §8(" + source + ")";
        graphics.drawString(mc.font, rankStr, textX, textY + 11, 0xFFFFFFFF, true);

        // Tiers
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

        // Progress bar at bottom
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

    private static AbstractClientPlayer findEntityByUuid() {
        if (lookupUuid == null) return null;
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null) return null;

        for (AbstractClientPlayer player : mc.level.players()) {
            if (player.getUUID().equals(lookupUuid)) {
                return player;
            }
        }
        return null;
    }

    public static boolean isActive() {
        return active;
    }
}
