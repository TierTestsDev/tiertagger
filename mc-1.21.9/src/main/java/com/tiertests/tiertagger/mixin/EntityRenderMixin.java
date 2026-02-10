package com.tiertests.tiertagger.mixin;

import com.tiertests.tiertagger.config.DisplayType;
import com.tiertests.tiertagger.config.ModConfig;
import com.tiertests.tiertagger.manager.TierManager;
import com.tiertests.tiertagger.util.TierTagUtil;
import net.minecraft.ChatFormatting;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.state.EntityRenderState;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(EntityRenderer.class)
public abstract class EntityRenderMixin<T extends Entity, S extends EntityRenderState> {
    @Inject(method = "getNameTag", at = @At("RETURN"), cancellable = true)
    private void onGetNameTag(T entity, CallbackInfoReturnable<Component> cir) {
        if (entity == null) return;
        if (!(entity instanceof Player player)) return;
        if (!TierManager.hasPlayerData(player.getUUID())) return;

        MutableComponent tag = TierTagUtil.buildTagComponent(player.getUUID());
        if (tag == null) return;

        boolean prefix = ModConfig.getDisplayType() == DisplayType.PREFIX;
        Component separator = Component.literal(" | ").withStyle(ChatFormatting.GRAY);

        if (prefix) {
            cir.setReturnValue(tag.append(separator).append(entity.getDisplayName()));
        } else {
            cir.setReturnValue(entity.getDisplayName().copy().append(separator).append(tag));
        }
    }
}
