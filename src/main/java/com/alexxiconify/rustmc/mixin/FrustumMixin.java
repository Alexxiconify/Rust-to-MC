package com.alexxiconify.rustmc.mixin;

import com.alexxiconify.rustmc.ModBridge;
import com.alexxiconify.rustmc.NativeBridge;
import com.alexxiconify.rustmc.RustMC;
import net.minecraft.client.render.Frustum;
import net.minecraft.util.math.Box;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(Frustum.class)
public class FrustumMixin {

    // Optimizes the CPU bottleneck of Frustum visibility checks by routing the math to Rust.
    @Inject(method = "isVisible(Lnet/minecraft/util/math/Box;)Z", at = @At("HEAD"), cancellable = true)
    private void onIsVisible(Box box, CallbackInfoReturnable<Boolean> cir) {
        // If Sodium, MoreCulling, or an equivalent mod is present, they drastically rewrite
        // the rendering pipeline. We yield to them to prevent conflicts and crashes.
        if (ModBridge.isLightingOwned() || !NativeBridge.isReady()) {
            return;
        }

        if (RustMC.CONFIG.isUseNativeCulling()) {
            // box.minX, box.minY, box.minZ, box.maxX, box.maxY, box.maxZ
            int result = NativeBridge.invokeFrustumIntersect(
                box.minX, box.minY, box.minZ, 
                box.maxX, box.maxY, box.maxZ
            );
            
            // If the native method returns -1, it means the Rust struct wasn't initialized with
            // the current camera vectors yet, so we fall back to vanilla.
            // If it returns 0 or 1, we use that boolean result.
            if (result >= 0) {
                cir.setReturnValue(result == 1);
            }
        }
    }
}
