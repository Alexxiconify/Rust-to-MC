package com.alexxiconify.rustmc.mixin;

import net.minecraft.util.math.noise.SimplexNoiseSampler;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import com.alexxiconify.rustmc.NativeBridge;

@Mixin(SimplexNoiseSampler.class)
public class SimplexNoiseSamplerMixin {
    @Inject(method = "sample(DD)D", at = @At("HEAD"), cancellable = true)
    private void onSample(double x, double y, CallbackInfoReturnable<Double> cir) {
        cir.setReturnValue(NativeBridge.noise2d(x, y));
    }
}
