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

    @Inject(method = "tan", at = @At("HEAD"), cancellable = true)
    private static void onTan(float f, CallbackInfoReturnable<Float> cir) {
        if (NativeBridge.isReady() && RustMC.CONFIG.isUseNativeTan()) {
            cir.setReturnValue(NativeBridge.invokeTan(f));
        }
    }

    @Inject(method = "atan2", at = @At("HEAD"), cancellable = true)
    private static void onAtan2(double y, double x, CallbackInfoReturnable<Double> cir) {
        if (NativeBridge.isReady() && RustMC.CONFIG.isUseNativeAtan2()) {
            cir.setReturnValue(NativeBridge.invokeAtan2(y, x));
        }
    }

    @Inject(method = "floor", at = @At("HEAD"), cancellable = true)
    private static void onFloor(double d, CallbackInfoReturnable<Integer> cir) {
        if (NativeBridge.isReady() && RustMC.CONFIG.isUseNativeFloor()) {
            cir.setReturnValue(NativeBridge.invokeFloor(d));
        }
    }
}
