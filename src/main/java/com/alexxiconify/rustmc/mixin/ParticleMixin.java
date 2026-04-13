package com.alexxiconify.rustmc.mixin;

import com.alexxiconify.rustmc.NativeBridge;
import com.alexxiconify.rustmc.RustMC;
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
    @Inject(method = "tick", at = @At("HEAD"), cancellable = true, require = 0)
    private void rustmcOnTick(CallbackInfo ci) {
        if (!RustMC.CONFIG.isEnableParticleCulling() || !NativeBridge.isReady()) {
            return;
        }
        double radius = 0.5;
        if (NativeBridge.isOutsideFrustum(this.x, this.y, this.z, radius)) {
            ci.cancel();
        }
    }
}