package com.tiertests.tiertagger.menu;

import com.tiertests.tiertagger.TierTaggerCommon;
import com.tiertests.tiertagger.api.TierAPI;
import com.tiertests.tiertagger.config.ModConfig;
import com.tiertests.tiertagger.data.GameMode;
import com.tiertests.tiertagger.data.GameModeManager;
import com.tiertests.tiertagger.data.PlayerData;
import com.tiertests.tiertagger.data.Tier;
import com.tiertests.tiertagger.manager.TierManager;
import com.tiertests.tiertagger.util.IconResolver;
import java.util.UUID;
import lombok.NonNull;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import com.tiertests.tiertagger.util.SkinTextureManager;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.resources.Identifier;
public class ConfigScreen extends Screen {

    private final Screen parent;

    private static long lastRefreshTime = 0;
    private static final long REFRESH_COOLDOWN = 15_000;

    private static final Identifier DISCORD_ICON =
            Identifier.fromNamespaceAndPath("tiertagger", "textures/gui/discord.png");

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

        int leftCol = centerX - buttonWidth - 4;
        int rightCol = centerX + 4;

        int startY = 28;

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

        addRenderableWidget(Button.builder(
                Component.literal("Gamemode: " + ModConfig.getSelectedGameModeName()),
                b -> {
                    ModConfig.nextGameMode();
                    b.setMessage(Component.literal("Gamemode: " + ModConfig.getSelectedGameModeName()));
                })
                .bounds(leftCol, startY + spacing, buttonWidth, buttonHeight)
                .tooltip(Tooltip.create(Component.literal("The gamemode to display tiers for.")))
                .build());

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

        addRenderableWidget(Button.builder(
                Component.literal("Icon Type: " + ModConfig.getIconType().getDisplayName()),
                b -> {
                    ModConfig.nextIconType();
                    b.setMessage(Component.literal("Icon Type: " + ModConfig.getIconType().getDisplayName()));
                })
                .bounds(leftCol, startY + spacing * 3, buttonWidth, buttonHeight)
                .tooltip(Tooltip.create(Component.literal("Classic: Colored Unicode symbols.\nMCTiers: Custom PNG icons.")))
                .build());

        addRenderableWidget(Button.builder(
                Component.literal("Show Region: " + bool(ModConfig.isShowRegion())),
                b -> {
                    ModConfig.toggleShowRegion();
                    b.setMessage(Component.literal("Show Region: " + bool(ModConfig.isShowRegion())));
                })
                .bounds(rightCol, startY, buttonWidth, buttonHeight)
                .tooltip(Tooltip.create(Component.literal("Show player's region after their tier.")))
                .build());

        addRenderableWidget(Button.builder(
                Component.literal("Show in Tab: " + bool(ModConfig.isShowInTab())),
                b -> {
                    ModConfig.toggleShowInTab();
                    b.setMessage(Component.literal("Show in Tab: " + bool(ModConfig.isShowInTab())));
                })
                .bounds(rightCol, startY + spacing, buttonWidth, buttonHeight)
                .tooltip(Tooltip.create(Component.literal("Show tier tags in the player tab list.")))
                .build());

        addRenderableWidget(Button.builder(
                Component.literal("MCTiers Format: " + bool(ModConfig.isUseMCTiersFormat())),
                b -> {
                    ModConfig.toggleUseMCTiersFormat();
                    b.setMessage(Component.literal("MCTiers Format: " + bool(ModConfig.isUseMCTiersFormat())));
                })
                .bounds(rightCol, startY + spacing * 2, buttonWidth, buttonHeight)
                .tooltip(Tooltip.create(Component.literal("Use MCTiers format (LT5, HT1) or standard (F, S).")))
                .build());

        addRenderableWidget(Button.builder(
                Component.literal("\uD83D\uDD0D Search Player"),
                b -> minecraft.setScreen(new PlayerSearchScreen(this)))
                .bounds(rightCol, startY + spacing * 3, buttonWidth, buttonHeight)
                .build());

        int bottomRowY = height - 28;
        int actionWidth = 90;
        int refreshWidth = 30;
        int discordWidth = 22;
        int totalActionWidth = refreshWidth + 4 + discordWidth; // refresh + discord
        if (TierTaggerCommon.isUpdateAvailable()) totalActionWidth += actionWidth + 4;
        int actionStartX = centerX - totalActionWidth / 2;
        int btnX = actionStartX;

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

        if (TierTaggerCommon.isUpdateAvailable()) {
            addRenderableWidget(Button.builder(
                    Component.literal("§e⬆ Update"),
                    b -> openUrl("https://modrinth.com/mod/tiertests"))
                    .bounds(btnX, bottomRowY, actionWidth, buttonHeight)
                    .tooltip(Tooltip.create(Component.literal("A new version is available on Modrinth!")))
                    .build());
            btnX += actionWidth + 4;
        }

        addRenderableWidget(Button.builder(
                Component.literal(""),
                b -> openUrl(TierAPI.getDiscordInvite()))
                .bounds(btnX, bottomRowY, discordWidth, buttonHeight)
                .tooltip(Tooltip.create(Component.literal("Join the Discord support server")))
                .build());

        fetchSelfData();
    }

    private void fetchSelfData() {
        if (fetchingSelf) return;

        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null) return;

        UUID uuid = mc.player.getUUID();
        String name = mc.player.getName().getString();

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

        graphics.drawCenteredString(font, "§lTier Tagger", width / 2, 10, 0xFF6c5ce7);
        graphics.drawString(font, "§7v" + TierTaggerCommon.VERSION, 4, height - 12, 0xFFAAAAAA, false);
        int actionWidth = 90;
        int refreshWidth = 30;
        int discordWidth = 22;
        int totalActionWidth = refreshWidth + 4 + discordWidth;
        if (TierTaggerCommon.isUpdateAvailable()) totalActionWidth += actionWidth + 4;
        int actionStartX = width / 2 - totalActionWidth / 2;
        int discordBtnX = actionStartX + refreshWidth + 4;
        if (TierTaggerCommon.isUpdateAvailable()) discordBtnX += actionWidth + 4;
        int discordBtnY = height - 28;
        graphics.blit(RenderPipelines.GUI_TEXTURED, DISCORD_ICON, discordBtnX + 3, discordBtnY + 2, 0, 0, 16, 16, 16, 16);

        int refreshBtnX = actionStartX;
        int refreshBtnY = height - 28;
        int buttonHeight2 = 20;
        String refreshStr = isRefreshOnCooldown() ? "§7\u21BB" : "\u21BB";
        int strW = font.width("\u21BB");
        int drawX = refreshBtnX + (refreshWidth - strW) / 2;
        int drawY = refreshBtnY + (buttonHeight2 - font.lineHeight) / 2;
        graphics.drawString(font, refreshStr, drawX, drawY, 0xFFFFFFFF, true);

        renderSelfPanel(graphics, mouseX, mouseY);
    }

    private void renderSelfPanel(GuiGraphics graphics, int mouseX, int mouseY) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null) return;

        UUID selfUuid = mc.player.getUUID();
        String playerName = mc.player.getName().getString();
        int centerX = width / 2;

        MutableComponent nametagComp;
        if (fetchingSelf) {
            nametagComp = Component.literal("Loading...").withStyle(s -> s.withColor(0xAAAAAA));
        } else if (selfData == null || selfData.isEmpty()) {
            nametagComp = Component.literal(playerName).withStyle(s -> s.withColor(0x55FF55));
        } else {
            GameMode resolvedMode = null;
            Tier resolvedTier = null;
            switch (ModConfig.getTierDisplayMode()) {
                case HIGHEST_FALLBACK -> {
                    resolvedMode = GameModeManager.getFromInt(ModConfig.getGameMode());
                    if (resolvedMode != null) resolvedTier = selfData.getTier(resolvedMode);
                    if (resolvedTier == null) {
                        resolvedMode = selfData.getHighestTierGamemode();
                        resolvedTier = selfData.getHighestTier();
                    }
                }
                case SELECTED_ONLY -> {
                    resolvedMode = GameModeManager.getFromInt(ModConfig.getGameMode());
                    if (resolvedMode != null) resolvedTier = selfData.getTier(resolvedMode);
                }
                default -> {
                    resolvedMode = selfData.getHighestTierGamemode();
                    resolvedTier = selfData.getHighestTier();
                }
            }
            nametagComp = Component.empty();
            if (resolvedMode != null && resolvedTier != null) {
                String icon = IconResolver.resolve(resolvedMode);
                if (icon != null && !icon.isEmpty()) {
                    if (IconResolver.isClassicIcon(resolvedMode)) {
                        int iconColor = IconResolver.getClassicColor(resolvedMode);
                        if (iconColor == -1) iconColor = resolvedMode.colorInt();
                        final int c = iconColor;
                        nametagComp.append(Component.literal(icon + " ").withStyle(s -> s.withColor(c)));
                    } else {
                        nametagComp.append(Component.literal(icon + " "));
                    }
                }
                String tierName = ModConfig.isUseMCTiersFormat() ? resolvedTier.getMcTiersName() : resolvedTier.getDisplayName();
                nametagComp.append(Component.literal(tierName));
            }
            nametagComp.append(Component.literal(" | ").withStyle(s -> s.withColor(0xAAAAAA)));
            nametagComp.append(Component.literal(playerName).withStyle(s -> s.withColor(0x55FF55)));
            if (ModConfig.isShowRegion() && selfData.getRegion() != null) {
                nametagComp.append(Component.literal(" [").withStyle(s -> s.withColor(0x555555)));
                nametagComp.append(Component.literal(selfData.getRegion().shortPrefix()));
                nametagComp.append(Component.literal("]").withStyle(s -> s.withColor(0x555555)));
            }
        }

        int areaTop = 126;
        int areaBottom = height - 32;

        int skinAvailH = (areaBottom - areaTop) / 2;
        int skinW = Math.max(20, skinAvailH / 2);
        int skinH = skinAvailH;
        int skinX = centerX - skinW / 2;
        int skinY = areaBottom - skinH;

        float nametagScale = 1.5f;
        int rawTextW = font.width(nametagComp);
        int nametagPad = 6;
        int scaledW = (int)(rawTextW * nametagScale) + nametagPad * 2;
        int scaledH = (int)(font.lineHeight * nametagScale) + 6;
        int nametagX = centerX - scaledW / 2;
        int nametagY = skinY - scaledH + 2;

        graphics.fill(nametagX, nametagY, nametagX + scaledW, nametagY + scaledH, 0x40000000);
        org.joml.Matrix3x2fStack pose = graphics.pose();
        pose.pushMatrix();
        pose.translate(centerX, nametagY + 3);
        pose.scale(nametagScale, nametagScale);
        graphics.drawCenteredString(font, nametagComp, 0, 0, 0xFFFFFFFF);
        pose.popMatrix();

        SkinTextureManager.fetchBodyFront(selfUuid);
        if (SkinTextureManager.hasBodyFront(selfUuid)) {
            SkinTextureManager.renderBodyFront(graphics, selfUuid, skinX, skinY, skinW, skinH);
        }
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
            minecraft.keyboardHandler.setClipboard(url);
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
