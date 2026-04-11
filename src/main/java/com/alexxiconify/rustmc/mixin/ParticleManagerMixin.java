package com.alexxiconify.rustmc.mixin;

import com.alexxiconify.rustmc.util.RenderState;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.particle.Particle;
import net.minecraft.client.particle.ParticleManager;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.particle.ParticleEffect;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

// Distance-culls particles before they are created to reduce GPU and CPU overhead. Uses a tighter cutoff when heavy entity mods (EMF/ETF) are active to free up rendering headroom. When ImmediatelyFast is batching draws, we use a more generous cutoff since IF makes each particle draw cheaper.
@Mixin(ParticleManager.class)
public class ParticleManagerMixin {
    @SuppressWarnings("java:S107") // 8 params match the target method signature exactly
    @Inject(method = "addParticle(Lnet/minecraft/particle/ParticleEffect;DDDDDD)Lnet/minecraft/client/particle/Particle;",
            at = @At("HEAD"), cancellable = true)
    private void cullDistantParticles(ParticleEffect params, double x, double y, double z,
            double vx, double vy, double vz, CallbackInfoReturnable<Particle> cir) {
        if (!com.alexxiconify.rustmc.RustMC.CONFIG.isEnableParticleCulling()) return;
        PlayerEntity player = MinecraftClient.getInstance().player;
        if (player == null) return;
        double baseDistance = MinecraftClient.getInstance().options.getClampedViewDistance() * 8.0;
        // Adaptive culling based on render budget + mod state + IF multiplier
        double cutoff;
        if (RenderState.renderBudgetTight) {
            cutoff = baseDistance * 0.4; // FPS < 60: aggressive recovery
        } else if (RenderState.heavyEntityModsActive) {
            cutoff = baseDistance * 0.6; // 40% tighter when EMF/ETF are heavy
        } else {
            cutoff = baseDistance;
        }
        // Apply IF multiplier: IF makes draws cheaper via batching, so extend cutoff
        cutoff *= com.alexxiconify.rustmc.compat.ImmediatelyFastCompat.getCullingDistanceMultiplier();
        // Extra headroom when FPS is healthy
        if (RenderState.renderBudgetRelaxed) {
            cutoff *= 1.15;
        }
        if (player.squaredDistanceTo(x, y, z) > cutoff * cutoff) {
            cir.setReturnValue(null);
        }
    }
}