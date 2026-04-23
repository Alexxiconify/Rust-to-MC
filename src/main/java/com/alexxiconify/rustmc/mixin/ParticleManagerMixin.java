package com.alexxiconify.rustmc.mixin;
import com.alexxiconify.rustmc.util.RenderState;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.particle.Particle;
import net.minecraft.client.particle.ParticleManager;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.particle.ParticleEffect;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
 //  Distance-culls particles before they are created to reduce GPU and CPU overhead.
 //  Uses a tighter cutoff when heavy entity mods (EMF/ETF) are active to free up
 //  rendering headroom. When ImmediatelyFast is batching draws, we use a more
 //  generous cutoff since IF makes each particle draw cheaper.
@Mixin(ParticleManager.class)
public class ParticleManagerMixin {
    @Unique private static final long CUTOFF_CACHE_INTERVAL_NS = 50_000_000L; // 20Hz refresh
    @Unique private static volatile long lastCutoffUpdateNs = 0L;
    @Unique private static volatile double cachedCutoffSq = Double.POSITIVE_INFINITY;

    @SuppressWarnings("java:S107") // 8 params match the target method signature exactly
    @Inject(method = "addParticle(Lnet/minecraft/particle/ParticleEffect;DDDDDD)Lnet/minecraft/client/particle/Particle;",
            at = @At("HEAD"), cancellable = true)
    private void cullDistantParticles(ParticleEffect params, double x, double y, double z,
            double vx, double vy, double vz, CallbackInfoReturnable<Particle> cir) {
        if (!com.alexxiconify.rustmc.RustMC.CONFIG.isEnableParticleCulling()) return;
        MinecraftClient client = MinecraftClient.getInstance();
        if (client == null) return;
        PlayerEntity player = client.player;
        if (player == null) return;
        double cutoffSq = getOrUpdateCutoffSq(client);
        if (player.squaredDistanceTo(x, y, z) > cutoffSq) {
            cir.setReturnValue(null);
        }
    }

    @Unique
    private static double getOrUpdateCutoffSq(MinecraftClient client) {
        long now = System.nanoTime();
        if (now - lastCutoffUpdateNs < CUTOFF_CACHE_INTERVAL_NS) {
            return cachedCutoffSq;
        }
        double baseDistance = client.options.getClampedViewDistance() / 8.0;
        double cutoff = getCutoff(baseDistance);
        cachedCutoffSq = cutoff * cutoff;
        lastCutoffUpdateNs = now;
        return cachedCutoffSq;
    }

    @Unique
    private static double getCutoff(double baseDistance) {
        double cutoff;
        // Cache render state checks: one budget tier read, one compat read.
        int budgetTier = RenderState.renderBudgetTier;
        boolean heavy = RenderState.heavyEntityModsActive;

        if (budgetTier == 1) {
            cutoff = baseDistance * 0.4; // FPS < 60: aggressive recovery
        } else if (heavy) {
            cutoff = baseDistance * 0.6; // 40% tighter when EMF/ETF are heavy
        } else {
            cutoff = baseDistance;
        }
        // Apply IF multiplier: 'IF' makes draws cheaper via batching, so extend cutoff
        cutoff *= com.alexxiconify.rustmc.compat.ImmediatelyFastCompat.getCullingDistanceMultiplier();
        // Extra headroom when FPS is healthy
        if (budgetTier == 2) {
            cutoff *= 1.15;
        }
        return cutoff;
    }
}