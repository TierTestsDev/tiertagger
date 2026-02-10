package com.tiertests.tiertagger.api;

import com.tiertests.tiertagger.api.impl.McTiersSource;
import com.tiertests.tiertagger.api.impl.PvpTiersSource;
import com.tiertests.tiertagger.api.impl.SubTiersSource;
import com.tiertests.tiertagger.api.impl.TierTestsSource;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Supplier;

public class TierSourceFactory {
    private static final Map<TierSources, TierSource> INSTANCES = new HashMap<>();

    @RequiredArgsConstructor
    public enum TierSources {
        TIER_TESTS("Tier Tests", "\uE903", TierTestsSource::new),
        MC_TIERS("MCTiers", "\uE901", McTiersSource::new),
        SUB_TIERS("SubTiers", "\uE902", SubTiersSource::new),
        PVP_TIERS("PVPTiers", "\uE904", PvpTiersSource::new);

        @Getter
        private final String displayName;
        @Getter
        private final String logoIcon;
        private final Supplier<TierSource> source;

        public TierSource getSource() {
            return source.get();
        }
    }

    public static void init() {
        for (TierSources source : TierSources.values()) {
            INSTANCES.put(source, source.getSource());
        }
    }

    public static TierSource getTierSource(TierSources tierSources) {
        return INSTANCES.get(tierSources);
    }

}
