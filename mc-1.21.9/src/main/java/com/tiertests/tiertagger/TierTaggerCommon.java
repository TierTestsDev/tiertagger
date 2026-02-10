package com.tiertests.tiertagger;

import com.mojang.blaze3d.platform.InputConstants;
import com.tiertests.tiertagger.api.TierAPI;
import com.tiertests.tiertagger.api.TierSourceFactory;
import com.tiertests.tiertagger.config.ModConfigLoader;
import com.tiertests.tiertagger.data.GameModeManager;
import com.tiertests.tiertagger.manager.TierManager;
import com.tiertests.tiertagger.hud.LookupHud;
import com.tiertests.tiertagger.menu.ConfigScreen;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback;
import net.fabricmc.loader.api.FabricLoader;
import lombok.Getter;
import net.minecraft.client.KeyMapping;
import net.minecraft.resources.Identifier;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.lwjgl.glfw.GLFW;

import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class TierTaggerCommon {
    public static final String MOD_ID = "tiertagger";
    public static final String VERSION = FabricLoader.getInstance()
            .getModContainer(MOD_ID)
            .map(c -> c.getMetadata().getVersion().getFriendlyString())
            .orElse("unknown");
    public static final Logger LOGGER = LogManager.getLogger(MOD_ID);

    private static final Set<UUID> pendingFetches = ConcurrentHashMap.newKeySet();
    private static ClientLevel lastWorld = null;
    private static int tickCounter = 0;
    @Getter
    private static boolean updateAvailable = false;

    private static KeyMapping configKey;
    private static KeyMapping lookupKey;
    private static KeyMapping lastHitLookupKey;
    private static long lastLookupTime = 0;
    private static final long LOOKUP_COOLDOWN_MS = 500;

    private static Player lastHitPlayer = null;

    public static void setLastHitPlayer(Player player) {
        lastHitPlayer = player;
    }

    public static void clearPendingFetches() {
        pendingFetches.clear();
    }

    public static void init() {
        LOGGER.info("Starting Tier Tagger Mod");

        try {
            ModConfigLoader.load();
            TierSourceFactory.init();
            GameModeManager.init();
        } catch (Exception e) {
            LOGGER.error("Error during mod initialization:", e);
            return;
        }

        try {
            TierAPI.refreshDiscordInvite();
            TierAPI.checkUpdate(VERSION).thenAccept(available -> {
                updateAvailable = available;
            });
        } catch (Exception e) {
            LOGGER.error("Error checking for updates or refreshing Discord invite:", e);
        }

        try {
            configKey = new KeyMapping(
                    "key.tiertagger.openconfig",
                    InputConstants.Type.KEYSYM,
                    GLFW.GLFW_KEY_N,
                    new KeyMapping.Category(Identifier.fromNamespaceAndPath("tiertagger", "general")));
            KeyBindingHelper.registerKeyBinding(configKey);

            lookupKey = new KeyMapping(
                    "key.tiertagger.lookup",
                    InputConstants.Type.KEYSYM,
                    GLFW.GLFW_KEY_R,
                    new KeyMapping.Category(Identifier.fromNamespaceAndPath("tiertagger", "general")));
            KeyBindingHelper.registerKeyBinding(lookupKey);

            lastHitLookupKey = new KeyMapping(
                    "key.tiertagger.lookup_last_hit",
                    InputConstants.Type.KEYSYM,
                    GLFW.GLFW_KEY_C,
                    new KeyMapping.Category(Identifier.fromNamespaceAndPath("tiertagger", "general")));
            KeyBindingHelper.registerKeyBinding(lastHitLookupKey);
        } catch (Exception e) {
            LOGGER.error("Error registering key binding:", e);
        }

        HudRenderCallback.EVENT.register(LookupHud::render);

        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            while (configKey.consumeClick()) {
                if (client.player != null) {
                    client.setScreen(new ConfigScreen());
                }
            }

            if (lookupKey.consumeClick()) {
                long now = System.currentTimeMillis();
                if (now - lastLookupTime >= LOOKUP_COOLDOWN_MS && client.player != null) {
                    lastLookupTime = now;
                    AbstractClientPlayer closest = LookupHud.findClosestPlayer();
                    if (closest != null) {
                        LookupHud.triggerLookup(closest);
                    }
                }
            }

            if (lastHitLookupKey.consumeClick()) {
                long now = System.currentTimeMillis();
                if (now - lastLookupTime >= LOOKUP_COOLDOWN_MS && client.player != null && lastHitPlayer != null) {
                    if (lastHitPlayer instanceof AbstractClientPlayer acp && lastHitPlayer.isAlive()) {
                        lastLookupTime = now;
                        LookupHud.triggerLookup(acp);
                    }
                }
            }
        });

        ClientTickEvents.START_CLIENT_TICK.register(minecraft -> {
            if (minecraft.player == null)
                return;
            LocalPlayer player = minecraft.player;

            try {
                Level level = player.level();
                if (!level.isClientSide() || !(level instanceof ClientLevel clientLevel))
                    return;

                if (lastWorld != clientLevel) {
                    lastWorld = clientLevel;
                    pendingFetches.clear();
                }

                tickCounter++;

                if (tickCounter % 40 == 0) {
                    for (AbstractClientPlayer p : clientLevel.players()) {
                        UUID uuid = p.getUUID();
                        if (!TierManager.hasPlayerData(uuid) && pendingFetches.add(uuid)) {
                            TierManager.fetchPlayerData(uuid, p.getName().getString());
                        }
                    }
                    pendingFetches.removeIf(TierManager::hasPlayerData);
                }

                if (tickCounter % 600 == 0) {
                    pendingFetches.removeIf(uuid ->
                            clientLevel.players().stream()
                                    .noneMatch(p -> p.getUUID().equals(uuid))
                    );
                }

                if (tickCounter % 2400 == 0) {
                    TierManager.cleanupCache(clientLevel.players());
                    tickCounter = 0;
                }
            } catch (Exception e) {
                LOGGER.error("Error checking world change:", e);
            }
        });
    }
}
