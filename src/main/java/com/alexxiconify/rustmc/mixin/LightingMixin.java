package com.alexxiconify.rustmc.mixin;

import java.lang.foreign.Arena;
import java.lang.foreign.MemorySegment;
import java.lang.foreign.ValueLayout;
import net.minecraft.world.chunk.light.LightingProvider;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import com.alexxiconify.rustmc.NativeBridge;
import com.alexxiconify.rustmc.RustMC;

@SuppressWarnings("preview")
@Mixin(LightingProvider.class)
public class LightingMixin {
    @Inject(method = "doLightUpdates", at = @At("HEAD"), cancellable = true)
    private void onDoLightUpdates(CallbackInfoReturnable<Integer> cir) {
        if (!NativeBridge.isReady()) return;
        if (!RustMC.CONFIG.isUseNativeLighting()) return;
        // Pass a stub buffer – Rust treats count=0 as a no-op and returns the processed count (0).
        // Real light-data serialization is future work; for now we just measure round-trip overhead.
        try (Arena arena = Arena.ofConfined()) {
            MemorySegment stub = arena.allocate(ValueLayout.JAVA_INT, 1);
            int result = NativeBridge.propagateLightBulk(stub, 0);
            if (result >= 0) {
                // Fall through to vanilla for actual lighting work;
                // only cancel if Rust processed real updates in the future.
            }
        }
        // Do NOT cancel – let vanilla finish light propagation for correctness.
    }
}
