package com.tiertests.tiertagger.menu;

import com.tiertests.tiertagger.api.TierAPI;
import com.tiertests.tiertagger.api.TierSourceFactory;
import com.tiertests.tiertagger.data.GameMode;
import com.tiertests.tiertagger.data.GameModeManager;
import com.tiertests.tiertagger.data.PlayerData;
import com.tiertests.tiertagger.data.PlayerResult;
import com.tiertests.tiertagger.data.Tier;
import com.tiertests.tiertagger.config.ModConfig;
import lombok.NonNull;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.network.chat.Component;

import com.tiertests.tiertagger.util.IconResolver;
import com.tiertests.tiertagger.util.StringUtils;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

public class PlayerSearchScreen extends Screen {

    private final Screen parent;
    private EditBox searchField;

    private String statusMessage = "";
    private boolean searching = false;
    private int scrollOffset = 0;
    private int maxScroll = 0;

    private static String searchedPlayerName = "";
    private static Map<TierSourceFactory.TierSources, PlayerResult> searchResults = new LinkedHashMap<>();
    private static String currentInput = "";

    // Layout constants
    private static final int CARD_PADDING = 8;
    private static final int PILL_HEIGHT = 22;
    private static final int PILL_GAP = 4;
    private static final int SECTION_GAP = 12;
    private static final int HEADER_HEIGHT = 28;
    private static final int META_ROW_HEIGHT = 16;

    // Colors
    private static final int COLOR_BG_CARD = 0xE01a1a2e;
    private static final int COLOR_BORDER = 0xFF6c5ce7;
    private static final int COLOR_BORDER_DIM = 0xFF3d3570;
    private static final int COLOR_PILL_BG = 0xCC252540;
    private static final int COLOR_TEXT_DIM = 0xFF888899;
    private static final int COLOR_TEXT_BRIGHT = 0xFFFFFFFF;

    // Animation
    private float searchPulse = 0f;

    public PlayerSearchScreen(Screen parent) {
        super(Component.literal("Player Tier Search"));
        this.parent = parent;
    }

    @Override
    protected void init() {
        clearWidgets();

        int centerX = width / 2;
        int searchBarWidth = Math.min(260, width - 40);
        int fieldWidth = searchBarWidth - 56;

        searchField = new EditBox(
                font,
                centerX - searchBarWidth / 2 + 4,
                8,
                fieldWidth,
                20,
                Component.literal("Search"));
        searchField.setMaxLength(16);
        searchField.setValue(currentInput);
        searchField.setFocused(true);
        searchField.setHint(Component.literal("§7Enter username..."));

        addRenderableWidget(searchField);

        addRenderableWidget(Button.builder(
                Component.literal("Search"),
                b -> performSearch())
                .bounds(centerX - searchBarWidth / 2 + fieldWidth + 8, 8, 48, 20)
                .build());

        addRenderableWidget(Button.builder(
                Component.literal("§7\u2190 Back"),
                b -> minecraft.setScreen(parent))
                .bounds(4, 8, 50, 20)
                .build());
    }

    private void performSearch() {
        String username = searchField.getValue().trim();
        if (username.isEmpty()) return;

        if (!isValidUsername(username)) {
            statusMessage = "§cInvalid username.";
            return;
        }

        searching = true;
        statusMessage = "";
        currentInput = username;
        searchResults = new LinkedHashMap<>();
        searchedPlayerName = "";
        scrollOffset = 0;
        searchPulse = 0f;

        CompletableFuture<?>[] modeLoads = new CompletableFuture[TierSourceFactory.TierSources.values().length];
        int idx = 0;
        for (TierSourceFactory.TierSources source : TierSourceFactory.TierSources.values()) {
            modeLoads[idx++] = GameModeManager.loadModesForSource(source);
        }

        CompletableFuture.allOf(modeLoads).thenCompose(v ->
                TierAPI.fetchAllSourcesPlayerData(username)
        ).thenAccept(results -> {
            minecraft.execute(() -> {
                searching = false;
                if (results == null || results.isEmpty()) {
                    statusMessage = "§cPlayer not found on any tier list.";
                } else {
                    searchResults = results;
                    for (PlayerResult r : results.values()) {
                        if (r != null && r.name() != null) {
                            searchedPlayerName = r.name();
                            break;
                        }
                    }
                    statusMessage = "";
                }
            });
        });
    }

    private boolean isValidUsername(String username) {
        if (username.length() < 3 || username.length() > 16)
            return false;

        for (char c : username.toCharArray()) {
            if (!Character.isLetterOrDigit(c) && c != '_')
                return false;
        }
        return true;
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (searchField.keyPressed(keyCode, scanCode, modifiers)) {
            currentInput = searchField.getValue();
            return true;
        }

        if (keyCode == 257 || keyCode == 335) {
            performSearch();
            return true;
        }

        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    @Override
    public boolean charTyped(char codePoint, int modifiers) {
        if (searchField.charTyped(codePoint, modifiers)) {
            currentInput = searchField.getValue();
            return true;
        }
        return super.charTyped(codePoint, modifiers);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double horizontalAmount, double verticalAmount) {
        int scrollAmount = (int) (verticalAmount * 20);
        scrollOffset = Math.max(0, Math.min(maxScroll, scrollOffset - scrollAmount));
        return true;
    }

    @Override
    public void tick() {
        if (searching) {
            searchPulse += 0.15f;
        }
    }

    @Override
    public void render(@NonNull GuiGraphics graphics, int mouseX, int mouseY, float delta) {
        super.render(graphics, mouseX, mouseY, delta);

        int centerX = width / 2;
        int contentWidth = Math.min(340, width - 20);
        int contentLeft = centerX - contentWidth / 2;
        int contentTop = 34;

        // Clip region for scrollable content
        graphics.enableScissor(0, contentTop, width, height);

        int cursorY = contentTop - scrollOffset;

        if (searching) {
            // Animated loading indicator
            int dotCount = ((int) (searchPulse)) % 4;
            String dots = ".".repeat(dotCount);
            String loadingMsg = "§7Searching all tier lists" + dots;
            graphics.drawCenteredString(font, loadingMsg, centerX, cursorY + 20, COLOR_TEXT_DIM);

            // Pulsing bar
            int barWidth = 80;
            int barX = centerX - barWidth / 2;
            int barY = cursorY + 36;
            float pulse = (float) (Math.sin(searchPulse * 2) * 0.5 + 0.5);
            int pulseAlpha = (int) (100 + 155 * pulse);
            int pulseColor = (pulseAlpha << 24) | (0x6c5ce7 & 0x00FFFFFF);
            graphics.fill(barX, barY, barX + barWidth, barY + 3, pulseColor);

            graphics.disableScissor();
            return;
        }

        if (!statusMessage.isEmpty()) {
            graphics.drawCenteredString(font, statusMessage, centerX, cursorY + 20, COLOR_TEXT_BRIGHT);
            graphics.disableScissor();
            return;
        }

        if (searchResults.isEmpty() || searchedPlayerName.isEmpty()) {
            // Empty state
            if (currentInput.isEmpty()) {
                graphics.drawCenteredString(font, "§7Search for a player above", centerX, cursorY + 30, COLOR_TEXT_DIM);
            }
            graphics.disableScissor();
            return;
        }

        // === Player Header Card ===
        cursorY = renderPlayerHeader(graphics, contentLeft, cursorY, contentWidth, mouseX, mouseY);
        cursorY += SECTION_GAP;

        // === Source Cards ===
        for (Map.Entry<TierSourceFactory.TierSources, PlayerResult> sourceEntry : searchResults.entrySet()) {
            TierSourceFactory.TierSources source = sourceEntry.getKey();
            PlayerResult result = sourceEntry.getValue();
            PlayerData data = result.data();

            cursorY = renderSourceCard(graphics, contentLeft, cursorY, contentWidth, source, data);
            cursorY += SECTION_GAP;
        }

        // Track max scroll
        int totalContentHeight = cursorY + scrollOffset - contentTop;
        int visibleHeight = height - contentTop;
        maxScroll = Math.max(0, totalContentHeight - visibleHeight + 10);

        graphics.disableScissor();

        // Scroll indicator
        if (maxScroll > 0) {
            renderScrollbar(graphics, width - 4, contentTop, height - contentTop, scrollOffset, maxScroll);
        }
    }

    private int renderPlayerHeader(GuiGraphics graphics, int x, int y, int w, int mouseX, int mouseY) {
        int headerH = 52;

        // Card background
        fillRoundedRect(graphics, x, y, w, headerH, COLOR_BG_CARD);
        renderBorder(graphics, x, y, w, headerH, COLOR_BORDER);

        // Paperdoll
        AbstractClientPlayer entity = findPlayerByName(searchedPlayerName);
        int dollX = x + 28;
        int dollTop = y + 4;
        int dollBottom = y + headerH - 4;
        if (entity != null) {
            graphics.enableScissor(x + 2, y + 2, x + 56, y + headerH - 2);
            InventoryScreen.renderEntityInInventoryFollowsMouse(
                    graphics,
                    dollX - 14, dollTop,
                    dollX + 14, dollBottom,
                    22,
                    0.0625f,
                    mouseX, mouseY,
                    entity
            );
            graphics.disableScissor();
        }

        // Player name - large
        int textX = x + 60;
        int nameY = y + 10;
        graphics.drawString(font, "§a§l" + searchedPlayerName, textX, nameY, COLOR_TEXT_BRIGHT, true);

        // Subtitle: number of sources found
        int sourceCount = searchResults.size();
        String subtitle = "§7Found on §f" + sourceCount + "§7 tier list" + (sourceCount != 1 ? "s" : "");
        graphics.drawString(font, subtitle, textX, nameY + 14, COLOR_TEXT_DIM, false);

        // Average tier across all sources
        int totalOrdinal = 0;
        int tierCount = 0;
        for (PlayerResult pr : searchResults.values()) {
            Tier t = pr.data().getHighestTier();
            if (t != null) {
                totalOrdinal += t.ordinal();
                tierCount++;
            }
        }
        if (tierCount > 0) {
            int avgOrdinal = Math.round((float) totalOrdinal / tierCount);
            avgOrdinal = Math.max(0, Math.min(avgOrdinal, Tier.values().length - 1));
            Tier avgTier = Tier.values()[avgOrdinal];
            String tierLabel = ModConfig.isUseMCTiersFormat() ? avgTier.getMcTiersName() : avgTier.getDisplayName();
            String avgStr = "§7Avg: " + tierLabel;
            int avgWidth = font.width(StringUtils.stripColorCodes(avgStr));
            graphics.drawString(font, avgStr, x + w - avgWidth - CARD_PADDING, nameY + 4, COLOR_TEXT_BRIGHT, true);
        }

        return y + headerH;
    }

    private int renderSourceCard(GuiGraphics graphics, int x, int y, int w, TierSourceFactory.TierSources source, PlayerData data) {
        boolean isMcTiersFormat = ModConfig.isUseMCTiersFormat();

        // Calculate card height
        int tierCount = data.getTiers().size();
        int pillColumns = Math.max(1, (w - CARD_PADDING * 2 + PILL_GAP) / (getPillWidth(w) + PILL_GAP));
        int pillRows = tierCount > 0 ? (int) Math.ceil((double) tierCount / pillColumns) : 0;
        int tiersBlockHeight = pillRows * (PILL_HEIGHT + PILL_GAP);
        int cardHeight = HEADER_HEIGHT + META_ROW_HEIGHT + CARD_PADDING + tiersBlockHeight + CARD_PADDING;
        if (tierCount == 0) cardHeight = HEADER_HEIGHT + META_ROW_HEIGHT + CARD_PADDING + 16;

        // Card background
        fillRoundedRect(graphics, x, y, w, cardHeight, COLOR_BG_CARD);
        renderBorder(graphics, x, y, w, cardHeight, COLOR_BORDER_DIM);

        // === Source Header ===
        // Header bar with accent
        graphics.fill(x + 1, y + 1, x + w - 1, y + HEADER_HEIGHT, 0x40000000);
        graphics.fill(x + 1, y + HEADER_HEIGHT - 1, x + w - 1, y + HEADER_HEIGHT, COLOR_BORDER_DIM);

        // Source logo + name
        String logo = source.getLogoIcon();
        int headerTextX = x + CARD_PADDING + 2;
        if (logo != null && !logo.isEmpty()) {
            graphics.drawString(font, logo, headerTextX, y + (HEADER_HEIGHT - 8) / 2, COLOR_TEXT_BRIGHT, false);
            headerTextX += font.width(logo) + 3;
        }
        graphics.drawString(font, "§f§l" + source.getDisplayName(), headerTextX, y + (HEADER_HEIGHT - 8) / 2, COLOR_TEXT_BRIGHT, true);

        // === Meta row: Region + Overall ===
        int metaY = y + HEADER_HEIGHT + 3;

        // Region badge
        int metaX = x + CARD_PADDING + 2;
        if (data.getRegion() != null) {
            String regionIcon = "\u2691"; // flag
            String regionText = regionIcon + " " + data.getRegion().shortPrefix();
            int regionPillW = font.width(regionText) + 10;
            fillRoundedRect(graphics, metaX, metaY, regionPillW, 12, 0xCC2a2a4a);
            graphics.drawString(font, regionText, metaX + 5, metaY + 2, 0xFF9999FF, false);
            metaX += regionPillW + 4;
        }

        // Overall rank badge
        if (data.getRank() > 0) {
            String rankIcon = "\u2726"; // star
            String rankText = rankIcon + " #" + data.getRank();
            if (data.getPoints() > 0) rankText += " §8(" + data.getPoints() + "pts)";
            int rankPillW = font.width(StringUtils.stripColorCodes(rankText)) + 10;
            fillRoundedRect(graphics, metaX, metaY, rankPillW, 12, 0xCC2a2a4a);
            graphics.drawString(font, rankText, metaX + 5, metaY + 2, 0xFFe8c840, false);
        }

        // === Tier Pills Grid ===
        int pillAreaTop = metaY + META_ROW_HEIGHT + 2;
        int pillW = getPillWidth(w);

        if (tierCount == 0) {
            graphics.drawCenteredString(font, "§8No tiers found", x + w / 2, pillAreaTop + 2, 0xFF555555);
            return y + cardHeight;
        }

        List<Map.Entry<GameMode, Tier>> entries = new ArrayList<>(data.getTiers().entrySet());
        for (int i = 0; i < entries.size(); i++) {
            int col = i % pillColumns;
            int row = i / pillColumns;

            int pillX = x + CARD_PADDING + col * (pillW + PILL_GAP);
            int pillY = pillAreaTop + row * (PILL_HEIGHT + PILL_GAP);

            GameMode mode = entries.get(i).getKey();
            Tier tier = entries.get(i).getValue();
            Tier peakTier = data.getPeakTier(mode);

            renderTierPill(graphics, pillX, pillY, pillW, mode, tier, peakTier, isMcTiersFormat);
        }

        return y + cardHeight;
    }

    private void renderTierPill(GuiGraphics graphics, int x, int y, int w, GameMode mode, Tier tier, Tier peakTier, boolean mcFormat) {
        // Pill background
        int modeColor = getModeColor(mode);
        int pillBg = blendColor(COLOR_PILL_BG, modeColor, 0.15f);
        fillRoundedRect(graphics, x, y, w, PILL_HEIGHT, pillBg);

        // Left accent bar
        graphics.fill(x, y + 2, x + 2, y + PILL_HEIGHT - 2, modeColor | 0xFF000000);

        // Icon
        String icon = IconResolver.resolve(mode);
        int textX = x + 6;
        if (icon != null && !icon.isEmpty()) {
            if (IconResolver.isClassicIcon(mode)) {
                int classicColor = IconResolver.getClassicColor(mode);
                int iconColor = (classicColor != -1 ? classicColor : modeColor) | 0xFF000000;
                graphics.drawString(font, icon, textX, y + (PILL_HEIGHT - 8) / 2, iconColor, false);
            } else {
                graphics.drawString(font, icon, textX, y + (PILL_HEIGHT - 8) / 2, 0xFFFFFFFF, false);
            }
            textX += font.width(icon) + 3;
        }

        // Mode name
        String modeName = StringUtils.beautify(mode.name());
        graphics.drawString(font, modeName, textX, y + (PILL_HEIGHT - 8) / 2, modeColor | 0xFF000000, false);

        // Tier value - right aligned
        String tierName = mcFormat ? tier.getMcTiersName() : tier.getDisplayName();
        int tierWidth = font.width(StringUtils.stripColorCodes(tierName));

        // Peak indicator
        String peakStr = "";
        if (peakTier != null && peakTier.ordinal() < tier.ordinal()) {
            String peakName = mcFormat ? peakTier.getMcTiersName() : peakTier.getDisplayName();
            peakStr = " §8\u2191" + peakName;
        }

        int totalRightWidth = tierWidth + font.width(StringUtils.stripColorCodes(peakStr));
        int rightX = x + w - totalRightWidth - 6;
        graphics.drawString(font, tierName, rightX, y + (PILL_HEIGHT - 8) / 2, COLOR_TEXT_BRIGHT, true);
        if (!peakStr.isEmpty()) {
            graphics.drawString(font, peakStr, rightX + tierWidth, y + (PILL_HEIGHT - 8) / 2, COLOR_TEXT_DIM, false);
        }
    }

    private int getPillWidth(int cardWidth) {
        int available = cardWidth - CARD_PADDING * 2;
        // Try to fit 2 columns, fall back to 1
        if (available >= 2 * 120 + PILL_GAP) {
            return (available - PILL_GAP) / 2;
        }
        return available;
    }

    private int getModeColor(GameMode mode) {
        if (IconResolver.isClassicIcon(mode)) {
            int classicColor = IconResolver.getClassicColor(mode);
            if (classicColor != -1) return classicColor;
        }
        return mode.colorInt();
    }

    private int blendColor(int base, int accent, float factor) {
        int bR = (base >> 16) & 0xFF, bG = (base >> 8) & 0xFF, bB = base & 0xFF, bA = (base >> 24) & 0xFF;
        int aR = (accent >> 16) & 0xFF, aG = (accent >> 8) & 0xFF, aB = accent & 0xFF;
        int r = (int) (bR + (aR - bR) * factor);
        int g = (int) (bG + (aG - bG) * factor);
        int b = (int) (bB + (aB - bB) * factor);
        return (bA << 24) | (r << 16) | (g << 8) | b;
    }

    private void fillRoundedRect(GuiGraphics graphics, int x, int y, int w, int h, int color) {
        // Minecraft doesn't have native rounded rects, simulate with filled rects
        graphics.fill(x + 1, y, x + w - 1, y + 1, color);
        graphics.fill(x, y + 1, x + w, y + h - 1, color);
        graphics.fill(x + 1, y + h - 1, x + w - 1, y + h, color);
    }

    private void renderBorder(GuiGraphics graphics, int x, int y, int w, int h, int color) {
        // Top (inset 1px for rounded feel)
        graphics.fill(x + 1, y, x + w - 1, y + 1, color);
        // Bottom
        graphics.fill(x + 1, y + h - 1, x + w - 1, y + h, color);
        // Left
        graphics.fill(x, y + 1, x + 1, y + h - 1, color);
        // Right
        graphics.fill(x + w - 1, y + 1, x + w, y + h - 1, color);
    }

    private void renderScrollbar(GuiGraphics graphics, int x, int y, int h, int offset, int max) {
        if (max <= 0) return;
        int barHeight = Math.max(10, h * h / (h + max));
        int barY = y + (int) ((float) offset / max * (h - barHeight));
        graphics.fill(x, y, x + 2, y + h, 0x20FFFFFF);
        graphics.fill(x, barY, x + 2, barY + barHeight, 0x80FFFFFF);
    }

    private AbstractClientPlayer findPlayerByName(String name) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null) return null;
        for (AbstractClientPlayer p : mc.level.players()) {
            if (p.getName().getString().equalsIgnoreCase(name)) {
                return p;
            }
        }
        return null;
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}
