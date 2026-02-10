package com.tiertests.tiertagger;

import com.mojang.blaze3d.platform.InputConstants;
import com.tiertests.tiertagger.api.TierAPI;
import com.tiertests.tiertagger.api.TierSourceFactory;
import com.tiertests.tiertagger.config.ModConfigLoader;
import com.tiertests.tiertagger.data.GameModeManager;
import com.tiertests.tiertagger.manager.TierManager;
import com.tiertests.tiertagger.hud.LookupHud;
import com.tiertests.tiertagger.menu.ConfigScreen;
import dev.architectury.event.events.client.ClientGuiEvent;
import dev.architectury.event.events.client.ClientTickEvent;
import dev.architectury.registry.client.keymappings.KeyMappingRegistry;
import lombok.Getter;
import net.minecraft.client.KeyMapping;
import net.minecraft.resources.Identifier;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.level.Level;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.lwjgl.glfw.GLFW;

import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class TierTaggerCommon {
    public static final String MOD_ID = "tiertagger";
    public static final String VERSION = "1.1.0";
    public static final Logger LOGGER = LogManager.getLogger(MOD_ID);

    private static final Set<UUID> pendingFetches = ConcurrentHashMap.newKeySet();
    private static ClientLevel lastWorld = null;
    private static int tickCounter = 0;
    @Getter
    private static boolean updateAvailable = false;

    private static KeyMapping configKey;
    private static KeyMapping lookupKey;
    private static long lastLookupTime = 0;
    private static final long LOOKUP_COOLDOWN_MS = 500;

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
            KeyMappingRegistry.register(configKey);

            lookupKey = new KeyMapping(
                    "key.tiertagger.lookup",
                    InputConstants.Type.KEYSYM,
                    GLFW.GLFW_KEY_B,
                    new KeyMapping.Category(Identifier.fromNamespaceAndPath("tiertagger", "general")));
            KeyMappingRegistry.register(lookupKey);
        } catch (Exception e) {
            LOGGER.error("Error registering key binding:", e);
        }

        ClientGuiEvent.RENDER_HUD.register(LookupHud::render);

        ClientTickEvent.CLIENT_POST.register(client -> {
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
        });

        ClientTickEvent.CLIENT_PRE.register(minecraft -> {
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
