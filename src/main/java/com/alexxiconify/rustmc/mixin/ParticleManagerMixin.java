package com.alexxiconify.rustmc.mixin;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.particle.Particle;
import net.minecraft.client.particle.ParticleManager;
import net.minecraft.particle.ParticleEffect;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(ParticleManager.class)
public class ParticleManagerMixin {

    // Cull particle creation beyond half the render distance.
    // Sodium doesn't touch particles, so this fills a real gap.
    @SuppressWarnings({"java:S107", "ConstantConditions"})
    @Inject(method = "addParticle(Lnet/minecraft/particle/ParticleEffect;DDDDDD)Lnet/minecraft/client/particle/Particle;",
            at = @At("HEAD"), cancellable = true)
    private void cullDistantParticles(ParticleEffect params, double x, double y, double z,
            double vx, double vy, double vz, CallbackInfoReturnable<Particle> cir) {
        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc.player == null) return;
        double cutoff = mc.options.getClampedViewDistance() * 8.0; // half render dist
        if (mc.player != null && mc.player.squaredDistanceTo(x, y, z) > cutoff * cutoff) {
            cir.setReturnValue(null);
        }
    }
}
