package com.alexxiconify.rustmc.mixin;

import com.alexxiconify.rustmc.NativeBridge;
import com.alexxiconify.rustmc.RustMC;
import net.minecraft.client.particle.Particle;
import net.minecraft.client.render.Camera;
import net.minecraft.client.render.VertexConsumer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Optimizes particle rendering by adding native frustum culling.
 * Overhauls the buildGeometry pathway to skip out-of-view particles.
 */
@Mixin(Particle.class)
public abstract class ParticleMixin {
    @Shadow protected double x;
    @Shadow protected double y;
    @Shadow protected double z;

    @Inject(method = "buildGeometry(Lnet/minecraft/client/render/VertexConsumer;Lnet/minecraft/client/render/Camera;F)V", at = @At("HEAD"), cancellable = true)
    private void rustmcOnBuildGeometry(VertexConsumer vertexConsumer, Camera camera, float tickDelta, CallbackInfo ci) {
        if (!RustMC.CONFIG.isEnableParticleCulling() || !NativeBridge.isReady()) {
            return;
        }

        String className = this.getClass().getName();
        
        // Enhance: Particle Rain support
        // We apply aggressive culling to rain/snow particles since they are high-count.
        boolean isHeavyParticle = className.contains("rain") || className.contains("snow") || className.contains("ParticleRain");
        double radius = isHeavyParticle ? 0.2 : 0.5;

        if (NativeBridge.isOutsideFrustum(this.x, this.y, this.z, radius)) {
            // Log throwaway state for debug
            RustMC.LOGGER.trace("[Rust-MC] Culled particle of type: {}", className);
            ci.cancel();
        }
    }
}
