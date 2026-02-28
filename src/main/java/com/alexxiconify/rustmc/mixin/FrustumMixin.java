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

    @Inject(method = "isVisible(Lnet/minecraft/util/math/Box;)Z", at = @At("HEAD"), cancellable = true)
    private void onIsVisible(Box box, CallbackInfoReturnable<Boolean> cir) {
        if (!RustMC.CONFIG.isUseNativeCulling() || ModBridge.isFrustumOwned() || !NativeBridge.isReady()) return;
        try {
            int result = NativeBridge.invokeFrustumIntersect(
                box.minX, box.minY, box.minZ,
                box.maxX, box.maxY, box.maxZ
            );
            if (result >= 0) cir.setReturnValue(result == 1);
        } catch (Exception ignored) { // native call unavailable; fall back to vanilla
        }
    }
}
