package com.tiertests.tiertagger.neoforge;

import com.tiertests.tiertagger.TierTaggerCommon;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;

@Mod(TierTaggerCommon.MOD_ID)
public class TierTaggerNeoForge {
    public TierTaggerNeoForge(IEventBus modBus) {
        TierTaggerCommon.init();
    }
}
