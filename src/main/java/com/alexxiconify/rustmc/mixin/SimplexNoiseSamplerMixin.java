package com.alexxiconify.rustmc.mixin;
import net.minecraft.util.math.noise.SimplexNoiseSampler;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import com.alexxiconify.rustmc.NativeBridge;
import com.alexxiconify.rustmc.RustMC;
//
 //  Replaces {@link SimplexNoiseSampler#sample(double, double)} with the Rust
 //  Simplex implementation.  The Rust generator is seeded with the world seed via
 //  {@code ServerWorldEvents.LOAD} in {@link RustMC#onInitialize()} before any
 //  chunk is generated, so the noise matches the MC world seed.
@Mixin(SimplexNoiseSampler.class)
public class SimplexNoiseSamplerMixin {
    @Inject(method = "sample(DD)D", at = @At("HEAD"), cancellable = true)
    private void onSample(double x, double y, CallbackInfoReturnable<Double> cir) {
        if (NativeBridge.isReady() && RustMC.CONFIG.isUseNativeNoise()) {
            cir.setReturnValue(NativeBridge.noise2d(x, y));
        }
    }
}