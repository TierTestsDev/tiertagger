package com.tiertests.tiertagger.menu;

import com.tiertests.tiertagger.TierTaggerCommon;
import com.tiertests.tiertagger.api.TierAPI;
import com.tiertests.tiertagger.config.ModConfig;
import com.tiertests.tiertagger.data.GameMode;
import com.tiertests.tiertagger.data.PlayerData;
import com.tiertests.tiertagger.data.Tier;
import com.tiertests.tiertagger.manager.TierManager;
import com.tiertests.tiertagger.util.IconResolver;
import com.tiertests.tiertagger.util.StringUtils;
import java.net.URI;
import java.util.Map;
import java.util.UUID;
import lombok.NonNull;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.resources.ResourceLocation;

public class ConfigScreen extends Screen {

    private final Screen parent;

    private static long lastRefreshTime = 0;
    private static final long REFRESH_COOLDOWN = 15_000;

    private static final ResourceLocation DISCORD_ICON =
            ResourceLocation.fromNamespaceAndPath("tiertagger", "textures/gui/discord.png");

    private Button refreshButton;
    private PlayerData selfData = null;
    private boolean fetchingSelf = false;

    public ConfigScreen(Screen parent) {
        super(Component.literal("Tier Tagger Configuration"));
        this.parent = parent;
    }

    public ConfigScreen() {
        this(null);
    }

    @Override
    protected void init() {
        int buttonWidth = 200;
        int buttonHeight = 20;
        int spacing = 24;
        int centerX = width / 2;

        // Two columns of settings, centered
        int leftCol = centerX - buttonWidth - 4;
        int rightCol = centerX + 4;

        int startY = 28;

        // === LEFT COLUMN: Core settings ===

        // Tier Source
        addRenderableWidget(Button.builder(
                Component.literal("Tier Source: " + ModConfig.getTierSource().getDisplayName()),
                b -> {
                    ModConfig.nextTierSource();
                    ModConfig.setGamemode(0);
                    b.setMessage(Component.literal("Tier Source: " + ModConfig.getTierSource().getDisplayName()));
                    fetchSelfData();
                    rebuildWidgets();
                })
                .bounds(leftCol, startY, buttonWidth, buttonHeight)
                .tooltip(Tooltip.create(Component.literal("The tier list to source the tiers from")))
                .build());

        // Gamemode
        addRenderableWidget(Button.builder(
                Component.literal("Gamemode: " + ModConfig.getSelectedGameModeName()),
                b -> {
                    ModConfig.nextGameMode();
                    b.setMessage(Component.literal("Gamemode: " + ModConfig.getSelectedGameModeName()));
                })
                .bounds(leftCol, startY + spacing, buttonWidth, buttonHeight)
                .tooltip(Tooltip.create(Component.literal("The gamemode to display tiers for.")))
                .build());

        // Display Mode
        addRenderableWidget(Button.builder(
                Component.literal("Display: " + ModConfig.getTierDisplayMode().getDisplayName()),
                b -> {
                    ModConfig.nextTierDisplayMode();
                    b.setMessage(Component.literal("Display: " + ModConfig.getTierDisplayMode().getDisplayName()));
                })
                .bounds(leftCol, startY + spacing * 2, buttonWidth, buttonHeight)
                .tooltip(Tooltip.create(Component.literal(
                        "Highest Fallback: Show selected, fallback to highest.\n" +
                                "Highest Always: Always show highest.\n" +
                                "Selected Only: Only show selected gamemode.\n" +
                                "Ranking: Show the players ranking overall.\n" +
                                "Cross-API Same Mode: Fallback to other APIs for the same mode.\n" +
                                "Cross-API Any Mode: Fallback to other APIs for any mode.")))
                .build());

        // Show Region
        addRenderableWidget(Button.builder(
                Component.literal("Show Region: " + bool(ModConfig.isShowRegion())),
                b -> {
                    ModConfig.toggleShowRegion();
                    b.setMessage(Component.literal("Show Region: " + bool(ModConfig.isShowRegion())));
                })
                .bounds(leftCol, startY + spacing * 3, buttonWidth, buttonHeight)
                .tooltip(Tooltip.create(Component.literal("Show player's region after their tier.")))
                .build());

        // === RIGHT COLUMN: Display settings ===

        // Show in Tab
        addRenderableWidget(Button.builder(
                Component.literal("Show in Tab: " + bool(ModConfig.isShowInTab())),
                b -> {
                    ModConfig.toggleShowInTab();
                    b.setMessage(Component.literal("Show in Tab: " + bool(ModConfig.isShowInTab())));
                })
                .bounds(rightCol, startY, buttonWidth, buttonHeight)
                .tooltip(Tooltip.create(Component.literal("Show tier tags in the player tab list.")))
                .build());

        // Icon Type
        addRenderableWidget(Button.builder(
                Component.literal("Icon Type: " + ModConfig.getIconType().getDisplayName()),
                b -> {
                    ModConfig.nextIconType();
                    b.setMessage(Component.literal("Icon Type: " + ModConfig.getIconType().getDisplayName()));
                })
                .bounds(rightCol, startY + spacing, buttonWidth, buttonHeight)
                .tooltip(Tooltip.create(Component.literal("Classic: Colored Unicode symbols.\nMCTiers: Custom PNG icons.")))
                .build());

        // MC Tiers format
        addRenderableWidget(Button.builder(
                Component.literal("MCTiers Format: " + bool(ModConfig.isUseMCTiersFormat())),
                b -> {
                    ModConfig.toggleUseMCTiersFormat();
                    b.setMessage(Component.literal("MCTiers Format: " + bool(ModConfig.isUseMCTiersFormat())));
                })
                .bounds(rightCol, startY + spacing * 2, buttonWidth, buttonHeight)
                .tooltip(Tooltip.create(Component.literal("Use MCTiers format (LT5, HT1) or standard (F, S).")))
                .build());

        // Search Player
        addRenderableWidget(Button.builder(
                Component.literal("\uD83D\uDD0D Search Player"),
                b -> minecraft.setScreen(new PlayerSearchScreen(this)))
                .bounds(rightCol, startY + spacing * 3, buttonWidth, buttonHeight)
                .build());

        // === BOTTOM ROW: Action buttons ===
        int bottomRowY = height - 28;
        int actionWidth = 90;
        int refreshWidth = 30;
        int discordWidth = 22;
        int totalActionWidth = refreshWidth + 4 + discordWidth; // refresh + discord
        if (TierTaggerCommon.isUpdateAvailable()) totalActionWidth += actionWidth + 4;
        int actionStartX = centerX - totalActionWidth / 2;
        int btnX = actionStartX;

        // Refresh button (icon drawn in render(), label empty)
        refreshButton = Button.builder(
                Component.literal(""),
                b -> {
                    if (!isRefreshOnCooldown()) {
                        TierManager.clearCache();
                        lastRefreshTime = System.currentTimeMillis();
                        fetchSelfData();
                    }
                })
                .bounds(btnX, bottomRowY, refreshWidth, buttonHeight)
                .tooltip(Tooltip.create(Component.literal("Refresh tier data")))
                .build();
        refreshButton.active = !isRefreshOnCooldown();
        addRenderableWidget(refreshButton);
        btnX += refreshWidth + 4;

        // Update button (only if available)
        if (TierTaggerCommon.isUpdateAvailable()) {
            addRenderableWidget(Button.builder(
                    Component.literal("§e⬆ Update"),
                    b -> openUrl("https://modrinth.com/mod/tiertests"))
                    .bounds(btnX, bottomRowY, actionWidth, buttonHeight)
                    .tooltip(Tooltip.create(Component.literal("A new version is available on Modrinth!")))
                    .build());
            btnX += actionWidth + 4;
        }

        // Discord icon button (small square)
        addRenderableWidget(Button.builder(
                Component.literal(""),
                b -> openUrl(TierAPI.getDiscordInvite()))
                .bounds(btnX, bottomRowY, discordWidth, buttonHeight)
                .tooltip(Tooltip.create(Component.literal("Join the Discord support server")))
                .build());

        // Fetch self tier data
        fetchSelfData();
    }

    private void fetchSelfData() {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null) return;

        UUID uuid = mc.player.getUUID();
        String name = mc.player.getName().getString();

        // Check cache first
        PlayerData cached = TierManager.getPlayerData(uuid);
        if (cached != null && !cached.isEmpty()) {
            selfData = cached;
            return;
        }

        fetchingSelf = true;
        selfData = null;
        TierAPI.fetchPlayerData(uuid, name).thenAccept(data -> {
            mc.execute(() -> {
                fetchingSelf = false;
                if (data != null && !data.isEmpty()) {
                    selfData = data;
                    TierManager.cachePlayerData(uuid, data);
                }
            });
        });
    }

    @Override
    public void render(@NonNull GuiGraphics graphics, int mouseX, int mouseY, float delta) {
        super.render(graphics, mouseX, mouseY, delta);

        // Title
        graphics.drawCenteredString(font, "§lTier Tagger", width / 2, 10, 0xFF6c5ce7);

        // Version
        graphics.drawString(font, "§7v" + TierTaggerCommon.VERSION, 4, height - 12, 0xFFAAAAAA, false);

        // Render discord icon on the discord button
        int actionWidth = 90;
        int refreshWidth = 30;
        int discordWidth = 22;
        int totalActionWidth = refreshWidth + 4 + discordWidth;
        if (TierTaggerCommon.isUpdateAvailable()) totalActionWidth += actionWidth + 4;
        int actionStartX = width / 2 - totalActionWidth / 2;
        int discordBtnX = actionStartX + refreshWidth + 4;
        if (TierTaggerCommon.isUpdateAvailable()) discordBtnX += actionWidth + 4;
        int discordBtnY = height - 28;
        graphics.blit(RenderType::guiTextured, DISCORD_ICON, discordBtnX + 2, discordBtnY + 3, 0, 0, 14, 14, 16, 16);

        // Render refresh icon scaled to button size
        int refreshBtnX = actionStartX;
        int refreshBtnY = height - 28;
        String refreshStr = isRefreshOnCooldown() ? "§7\u21BB" : "\u21BB";
        int strW = font.width("\u21BB");
        int strH = font.lineHeight;
        float scale = Math.min((float)(refreshWidth - 4) / strW, (float)(20 - 4) / strH);
        graphics.pose().pushPose();
        graphics.pose().translate(
                refreshBtnX + refreshWidth / 2f - (strW * scale) / 2f,
                refreshBtnY + 10f - (strH * scale) / 2f,
                0);
        graphics.pose().scale(scale, scale, 1f);
        graphics.drawString(font, refreshStr, 0, 0, 0xFFFFFFFF, true);
        graphics.pose().popPose();

        // Render self player panel centered below settings
        renderSelfPanel(graphics, mouseX, mouseY);
    }

    private void renderSelfPanel(GuiGraphics graphics, int mouseX, int mouseY) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null) return;

        int panelWidth = 200;
        int panelX = width / 2 - panelWidth / 2;
        int panelY = 130;

        // Panel background
        graphics.fill(panelX, panelY, panelX + panelWidth, panelY + getPanelHeight(), 0xCC1a1a2e);
        renderBorder(graphics, panelX, panelY, panelWidth, getPanelHeight(), 0xFF6c5ce7);

        // Paperdoll - centered at top of panel
        int entityCenterX = panelX + panelWidth / 2;
        int entityTopY = panelY + 4;
        int entityBottomY = panelY + 60;
        int entitySize = 28;

        graphics.enableScissor(panelX + 2, panelY + 2, panelX + panelWidth - 2, entityBottomY);
        InventoryScreen.renderEntityInInventoryFollowsMouse(
                graphics,
                entityCenterX - 20, entityTopY,
                entityCenterX + 20, entityBottomY,
                entitySize,
                0.0625f,
                mouseX, mouseY,
                mc.player
        );
        graphics.disableScissor();

        // Player name
        String playerName = mc.player.getName().getString();
        int textY = entityBottomY + 4;

        if (fetchingSelf) {
            graphics.drawCenteredString(font, "§7Loading tiers...", panelX + panelWidth / 2, textY, 0xFFFFFFFF);
            return;
        }

        if (selfData == null || selfData.isEmpty()) {
            graphics.drawCenteredString(font, "§a" + playerName, panelX + panelWidth / 2, textY, 0xFFFFFFFF);
            graphics.drawCenteredString(font, "§7No tiers found", panelX + panelWidth / 2, textY + 12, 0xFFAAAAAA);
            return;
        }

        // Highest tier above name
        Tier highest = selfData.getHighestTier();
        if (highest != null) {
            String tierLabel = ModConfig.isUseMCTiersFormat() ? highest.getMcTiersName() : highest.getDisplayName();
            graphics.drawCenteredString(font, tierLabel, panelX + panelWidth / 2, textY, 0xFFFFFFFF);
            textY += 12;
        }

        // Name + region
        String nameStr = "§a" + playerName;
        if (selfData.getRegion() != null) {
            nameStr += " §8[" + selfData.getRegion().shortPrefix() + "§8]";
        }
        graphics.drawCenteredString(font, nameStr, panelX + panelWidth / 2, textY, 0xFFFFFFFF);
        textY += 12;

        // Rank
        String source = ModConfig.getTierSource().getDisplayName();
        graphics.drawCenteredString(font, "§7#" + selfData.getRank() + " §8(" + source + ")", panelX + panelWidth / 2, textY, 0xFFFFFFFF);
        textY += 14;

        // Tier list
        for (Map.Entry<GameMode, Tier> entry : selfData.getTiers().entrySet()) {
            GameMode mode = entry.getKey();
            Tier tier = entry.getValue();

            String tierName = ModConfig.isUseMCTiersFormat() ? tier.getMcTiersName() : tier.getDisplayName();
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

            Tier peakTier = selfData.getPeakTier(mode);
            String peakSuffix = "";
            if (peakTier != null && peakTier.ordinal() < tier.ordinal()) {
                String peakName = ModConfig.isUseMCTiersFormat() ? peakTier.getMcTiersName()
                        : peakTier.getDisplayName();
                peakSuffix = " §7(Peak: " + peakName + "§7)";
            }

            String line = iconPrefix + modeName + " §f" + tierName + peakSuffix;
            if (!iconPrefix.isEmpty() && !IconResolver.isClassicIcon(mode)) {
                String rest = modeName + " §f" + tierName + peakSuffix;
                int restWidth = font.width(rest);
                int iconWidth = font.width(iconPrefix);
                int startX = panelX + panelWidth / 2 - restWidth / 2;
                graphics.drawString(font, iconPrefix, startX - iconWidth, textY, 0xFFFFFFFF, true);
                graphics.drawString(font, rest, startX, textY, modeColor, true);
            } else {
                graphics.drawCenteredString(font, line, panelX + panelWidth / 2, textY, modeColor);
            }
            textY += 12;

            if (textY > height - 50) break;
        }
    }

    private int getPanelHeight() {
        int baseHeight = 60 + 4 + 12 + 12 + 14; // entity + gap + tier + name + rank
        if (selfData != null && !selfData.isEmpty()) {
            int tierCount = Math.min(selfData.getTiers().size(), 8);
            baseHeight += 12 + tierCount * 12; // highest tier label + tier lines
        } else {
            baseHeight += 12; // "No tiers found"
        }
        return baseHeight + 8; // padding
    }

    private void renderBorder(GuiGraphics graphics, int x, int y, int w, int h, int color) {
        graphics.fill(x, y, x + w, y + 1, color);
        graphics.fill(x, y + h - 1, x + w, y + h, color);
        graphics.fill(x, y, x + 1, y + h, color);
        graphics.fill(x + w - 1, y, x + w, y + h, color);
    }

    @Override
    public void tick() {
        if (refreshButton != null) {
            refreshButton.active = !isRefreshOnCooldown();
        }

        children().stream()
                .filter(w -> w instanceof Button b && b.getMessage().getString().contains("Loading Modes..."))
                .forEach(w -> {
                    Button b = (Button) w;
                    b.setMessage(Component.literal("Gamemode: " + ModConfig.getSelectedGameModeName()));
                });
    }

    private boolean isRefreshOnCooldown() {
        return System.currentTimeMillis() - lastRefreshTime < REFRESH_COOLDOWN;
    }

    private String bool(boolean value) {
        return value ? "§aYes" : "§cNo";
    }

    private void openUrl(String url) {
        try {
            java.awt.Desktop.getDesktop().browse(new URI(url));
        } catch (Exception ignored) {}
    }

    @Override
    public void onClose() {
        minecraft.setScreen(parent);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}
