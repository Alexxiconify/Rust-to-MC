package com.alexxiconify.rustmc.mixin;

import java.lang.foreign.Arena;
import java.lang.foreign.MemorySegment;
import net.minecraft.world.chunk.light.LightingProvider;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import com.alexxiconify.rustmc.NativeBridge;

@SuppressWarnings("preview")
@Mixin(LightingProvider.class)
public class LightingMixin {
    @Inject(method = "doLightUpdates", at = @At("HEAD"), cancellable = true)
    private void onDoLightUpdates(CallbackInfoReturnable<Integer> cir) {
        try (Arena arena = Arena.ofConfined()) {
            int result = NativeBridge.propagateLightBulk(MemorySegment.NULL, 0);
            if (result >= 0) {
                cir.setReturnValue(result);
            }
        }
    }
}
