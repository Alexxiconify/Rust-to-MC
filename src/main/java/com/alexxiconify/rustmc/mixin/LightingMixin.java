package com.alexxiconify.rustmc.mixin;

import net.minecraft.world.chunk.light.LightingProvider;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(LightingProvider.class)
public class LightingMixin {

    @Inject(method = "doLightUpdates", at = @At("HEAD"))
    private void onDoLightUpdates(CallbackInfo ci) {
        // Reserved for future JNI-based light propagation optimizations.
    }
}
