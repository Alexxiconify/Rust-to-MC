package com.alexxiconify.rustmc.mixin;

import com.alexxiconify.rustmc.NativeBridge;
import com.alexxiconify.rustmc.RustMC;
import com.alexxiconify.rustmc.util.RenderState;
import net.minecraft.client.particle.Particle;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

// Optimizes particle rendering by culling particles outside the view frustum during tick. In 1.21.11, buildGeometry/render were removed from Particle and moved to BillboardParticleRenderer. We use tick() as the hook point instead.
@Mixin(Particle.class)
public abstract class ParticleMixin {
    @Shadow protected double x;
    @Shadow protected double y;
    @Shadow protected double z;
    @Shadow public abstract void markDead();

    // tick() still exists in 1.21.11's Particle class — safe hook for frustum culling
    @Inject(method = "tick", at = @At("HEAD"), cancellable = true, require = 0)
    private void rustmcOnTick(CallbackInfo ci) {
        if (!RustMC.CONFIG.isEnableParticleCulling() || !NativeBridge.isReady()) {
            return;
        }
        // Adaptive culling radius based on render budget and mod ecosystem
        double radius = 0.5;
        if (RenderState.renderBudgetTight) {
            radius = 0.2;
        } else if (RenderState.renderBudgetRelaxed || RenderState.immediatelyFastActive) {
            radius = 0.8;
        }
        if (RenderState.heavyEntityModsActive) {
            radius *= 0.7;
        }
        if (NativeBridge.isOutsideFrustum(this.x, this.y, this.z, radius)) {
            ci.cancel();
        }
    }
}