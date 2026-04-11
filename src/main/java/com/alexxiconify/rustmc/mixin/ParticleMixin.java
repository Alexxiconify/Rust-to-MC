package com.alexxiconify.rustmc.mixin;

import com.alexxiconify.rustmc.NativeBridge;
import com.alexxiconify.rustmc.RustMC;
import com.alexxiconify.rustmc.util.RenderState;
import net.minecraft.client.particle.Particle;
import net.minecraft.client.render.Camera;
import net.minecraft.client.render.VertexConsumer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

// Optimizes particle rendering by adding native frustum culling. Overhauls the buildGeometry pathway to skip out-of-view particles.
@Mixin(Particle.class)
public abstract class ParticleMixin {
    @Shadow protected double x;
    @Shadow protected double y;
    @Shadow protected double z;

    @Inject(method = {"buildGeometry", "render"}, at = @At("HEAD"), cancellable = true, require = 0)
    private void rustmcOnBuildGeometry(VertexConsumer vertexConsumer, Camera camera, float tickDelta, CallbackInfo ci) {
        if (!RustMC.CONFIG.isEnableParticleCulling() || !NativeBridge.isReady()) {
            return;
        }
        // Avoid expensive reflection and string allocations per-particle. Fast path for all particles with a safe default radius. Adaptive culling threshold based on render budget and mod ecosystem.
        double radius = 0.5;
        if (RenderState.renderBudgetTight) {
            radius = 0.2; // Tighten: only show large particles center-screen
        } else if (RenderState.renderBudgetRelaxed || RenderState.immediatelyFastActive) {
            radius = 0.8; // Relax: IF makes each draw cheaper, so show more.
        }
        if (RenderState.heavyEntityModsActive) {
            radius *= 0.7; // EMF/ETF active: reduce particle density to keep FPS stable.
        }
        if (NativeBridge.isOutsideFrustum(this.x, this.y, this.z, radius)) {
            ci.cancel();
        }
    }
}