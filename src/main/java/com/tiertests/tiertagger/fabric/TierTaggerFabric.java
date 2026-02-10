package com.tiertests.tiertagger.fabric;

import com.tiertests.tiertagger.TierTaggerCommon;
import net.fabricmc.api.ClientModInitializer;

public class TierTaggerFabric implements ClientModInitializer {
    @Override
    public void onInitializeClient() {
        TierTaggerCommon.init();
    }
}
