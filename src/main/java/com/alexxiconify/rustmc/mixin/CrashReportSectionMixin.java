package com.alexxiconify.rustmc.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

// Target intermediary name directly — no refmap generated for installed jars
@Mixin(targets = "net.minecraft.class_129", remap = false)
public class CrashReportSectionMixin {
    private CrashReportSectionMixin() {}

    // Redirect getFileName() calls inside method_584 to never return null.
    // @Inject + cancellable fails here because notenoughcrashes calls the patched
    // method without creating the CallbackInfoReturnable, passing null instead.
    @Redirect(
        method = "method_584(Ljava/lang/StackTraceElement;Ljava/lang/StackTraceElement;)Z",
        at = @At(value = "INVOKE", target = "Ljava/lang/StackTraceElement;getFileName()Ljava/lang/String;"),
        remap = false, allow = 2
    )
    private static String safeGetFileName(StackTraceElement element) {
        String name = element.getFileName();
        return name != null ? name : "<unknown>";
    }
}
