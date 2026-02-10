package com.tiertests.tiertagger.mixin;

import com.tiertests.tiertagger.TierTaggerCommon;
import net.minecraft.client.multiplayer.MultiPlayerGameMode;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(MultiPlayerGameMode.class)
public abstract class AttackMixin {
    @Inject(method = "attack", at = @At("HEAD"))
    private void onAttack(Player attacker, Entity target, CallbackInfo ci) {
        if (target instanceof Player hitPlayer) {
            TierTaggerCommon.setLastHitPlayer(hitPlayer);
        }
    }
}
