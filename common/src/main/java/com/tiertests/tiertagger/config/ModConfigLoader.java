package com.tiertests.tiertagger.config;

import com.tiertests.tiertagger.TierTaggerCommon;
import com.tiertests.tiertagger.api.TierSourceFactory;
import org.yaml.snakeyaml.Yaml;

import java.io.IOException;
import java.io.InputStream;
import java.io.Writer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;

public class ModConfigLoader {
    private static final Yaml YAML = new Yaml();
    private static final Path CONFIG_PATH =
            Path.of("config", "tiertagger.yml");

    public static void load() {
        try {
            if (!Files.exists(CONFIG_PATH)) {
                saveDefaults();
                return;
            }

            try (InputStream in = Files.newInputStream(CONFIG_PATH)) {
                Map<String, Object> data = YAML.load(in);

                ModConfig.gameMode =
                        (int) data.getOrDefault("game-mode", 0);

                try {
                    ModConfig.tierDisplayMode =
                            DisplayMode.valueOf(
                                    (String) data.getOrDefault(
                                            "tier-display-mode",
                                            DisplayMode.HIGHEST_FALLBACK.name()
                                    )
                            );
                } catch (IllegalArgumentException e) {
                    ModConfig.tierDisplayMode = DisplayMode.HIGHEST_FALLBACK;
                }

                try {
                    ModConfig.displayType =
                            DisplayType.valueOf(
                                    (String) data.getOrDefault(
                                            "display-type",
                                            DisplayType.PREFIX.name()
                                    )
                            );
                } catch (IllegalArgumentException e) {
                    ModConfig.displayType = DisplayType.PREFIX;
                }

                ModConfig.useMCTiersFormat =
                        (boolean) data.getOrDefault("use-mc-tiers-format", false);

                ModConfig.showRegion =
                        (boolean) data.getOrDefault("show-region", true);

                ModConfig.showInTab =
                        (boolean) data.getOrDefault("show-in-tab", true);

                int tierSourceOrdinal = (int) data.getOrDefault("tier-source", 0);
                TierSourceFactory.TierSources[] sources = TierSourceFactory.TierSources.values();
                ModConfig.tierSource = tierSourceOrdinal >= 0 && tierSourceOrdinal < sources.length
                        ? sources[tierSourceOrdinal]
                        : TierSourceFactory.TierSources.TIER_TESTS;

                try {
                    ModConfig.iconType =
                            IconType.valueOf(
                                    (String) data.getOrDefault(
                                            "icon-type",
                                            IconType.MCTIERS.name()
                                    )
                            );
                } catch (IllegalArgumentException e) {
                    ModConfig.iconType = IconType.MCTIERS;
                }
            }

        } catch (Exception e) {
            TierTaggerCommon.LOGGER.error("Error loading config", e);
        }
    }

    public static void save() {
        try {
            Files.createDirectories(CONFIG_PATH.getParent());

            try (Writer writer = Files.newBufferedWriter(CONFIG_PATH)) {
                Map<String, Object> configMap = new java.util.LinkedHashMap<>();
                configMap.put("game-mode", ModConfig.getGameMode());
                configMap.put("tier-display-mode", ModConfig.getTierDisplayMode().name());
                configMap.put("display-type", ModConfig.getDisplayType().name());
                configMap.put("use-mc-tiers-format", ModConfig.isUseMCTiersFormat());
                configMap.put("show-region", ModConfig.isShowRegion());
                configMap.put("show-in-tab", ModConfig.isShowInTab());
                configMap.put("tier-source", ModConfig.getTierSource().ordinal());
                configMap.put("icon-type", ModConfig.getIconType().name());
                YAML.dump(configMap, writer);
            }

        } catch (IOException e) {
            TierTaggerCommon.LOGGER.error("Error saving config", e);
        }
    }

    private static void saveDefaults() {
        save();
    }

}
