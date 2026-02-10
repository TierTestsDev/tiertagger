package com.tiertests.tiertagger.mixin;

import com.tiertests.tiertagger.config.DisplayType;
import com.tiertests.tiertagger.config.ModConfig;
import com.tiertests.tiertagger.manager.TierManager;
import com.tiertests.tiertagger.util.TierTagUtil;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.components.PlayerTabOverlay;
import net.minecraft.client.multiplayer.PlayerInfo;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.UUID;

@Mixin(PlayerTabOverlay.class)
public class PlayerTabOverlayMixin {

    @Inject(method = "getNameForDisplay", at = @At("RETURN"), cancellable = true)
    private void tiertagger$prependTier(PlayerInfo entry, CallbackInfoReturnable<Component> cir) {
        if (!ModConfig.isShowInTab()) return;

        UUID uuid = entry.getProfile().id();
        if (!TierManager.hasPlayerData(uuid)) return;

        MutableComponent tag = TierTagUtil.buildTabComponent(uuid);
        if (tag == null) return;

        Component original = cir.getReturnValue();
        boolean prefix = ModConfig.getDisplayType() == DisplayType.PREFIX;
        Component separator = Component.literal(" | ").withStyle(ChatFormatting.GRAY);

        if (prefix) {
            cir.setReturnValue(tag.append(separator).append(original));
        } else {
            cir.setReturnValue(original.copy().append(separator).append(tag));
        }
    }
}
