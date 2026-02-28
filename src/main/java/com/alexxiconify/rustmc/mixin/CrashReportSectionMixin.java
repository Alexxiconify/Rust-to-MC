package com.alexxiconify.rustmc.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(targets = "net.minecraft.class_129", remap = false)
public class CrashReportSectionMixin {
    private CrashReportSectionMixin() {
        /* This utility class should not be instantiated */
    }


    @Inject(method = "method_584(Ljava/lang/StackTraceElement;Ljava/lang/StackTraceElement;)Z",
            at = @At("HEAD"), cancellable = true, remap = false)
    private static void fixNullFileName(StackTraceElement a, StackTraceElement b,
                                        CallbackInfoReturnable<Boolean> cir) {
        if (a == null || b == null || a.getFileName() == null || b.getFileName() == null) {
            cir.setReturnValue(false);
        }
    }
}
