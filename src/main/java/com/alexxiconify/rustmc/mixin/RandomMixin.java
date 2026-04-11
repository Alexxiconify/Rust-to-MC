package com.alexxiconify.rustmc.mixin;

import com.alexxiconify.rustmc.NativeBridge;
import com.alexxiconify.rustmc.RustMC;
import net.minecraft.util.math.random.CheckedRandom;
import net.minecraft.util.math.random.LocalRandom;
import net.minecraft.util.math.random.Xoroshiro128PlusPlusRandom;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

// Redirects high-frequency random number generation to native Xoshiro256++ stubs. Reduces Java math overhead.
@Mixin({Xoroshiro128PlusPlusRandom.class, LocalRandom.class, CheckedRandom.class})
public class RandomMixin {
    
    private RandomMixin() {}

    @Inject(method = "nextInt(I)I", at = @At("HEAD"), cancellable = true)
    private void rustmc$onNextInt(int bound, CallbackInfoReturnable<Integer> cir) {
        if (RustMC.CONFIG.isEnableNativeRandom()) {
            cir.setReturnValue(NativeBridge.randomNextInt(bound));
        }
    }

    @Inject(method = "nextFloat()F", at = @At("HEAD"), cancellable = true)
    private void rustmc$onNextFloat(CallbackInfoReturnable<Float> cir) {
        if (RustMC.CONFIG.isEnableNativeRandom()) {
            cir.setReturnValue(NativeBridge.randomNextFloat());
        }
    }

    @Inject(method = "setSeed(J)V", at = @At("HEAD"))
    private void rustmc$onSetSeed(long seed, CallbackInfo ci) {
        if (RustMC.CONFIG.isEnableNativeRandom()) {
            NativeBridge.randomSetSeed(seed);
        }
    }
}