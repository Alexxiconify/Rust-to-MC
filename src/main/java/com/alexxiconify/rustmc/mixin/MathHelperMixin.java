package com.alexxiconify.rustmc.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import com.alexxiconify.rustmc.NativeBridge;
import com.alexxiconify.rustmc.RustMC;

import net.minecraft.util.math.MathHelper;

@Mixin(MathHelper.class)
public class MathHelperMixin {
    private MathHelperMixin() {}

    @Inject(method = "fastInvSqrt", at = @At("HEAD"), cancellable = true)
    private static void onFastInvSqrt(float x, CallbackInfoReturnable<Float> cir) {
        if (NativeBridge.isReady() && RustMC.CONFIG.isUseNativeInvSqrt()) {
            cir.setReturnValue(NativeBridge.fastInvSqrt(x));
        }
    }

    @Inject(method = "sin", at = @At("HEAD"), cancellable = true)
    private static void onSin(float f, CallbackInfoReturnable<Float> cir) {
        if (NativeBridge.isReady() && RustMC.CONFIG.isUseNativeSine()) {
            cir.setReturnValue(NativeBridge.invokeSin(f));
        }
    }

    @Inject(method = "cos", at = @At("HEAD"), cancellable = true)
    private static void onCos(float f, CallbackInfoReturnable<Float> cir) {
        if (NativeBridge.isReady() && RustMC.CONFIG.isUseNativeCos()) {
            cir.setReturnValue(NativeBridge.invokeCos(f));
        }
    }

    @Inject(method = "sqrt", at = @At("HEAD"), cancellable = true)
    private static void onSqrt(float f, CallbackInfoReturnable<Float> cir) {
        if (NativeBridge.isReady() && RustMC.CONFIG.isUseNativeSqrt()) {
            cir.setReturnValue(NativeBridge.invokeSqrt(f));
        }
    }
}
