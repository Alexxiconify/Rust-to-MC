package com.alexxiconify.rustmc.mixin;

import org.spongepowered.asm.mixin.Mixin;

// Target intermediary name directly — no refmap generated for installed jars
@Mixin(targets = "net.minecraft.class_129", remap = false)
public class CrashReportSectionMixin {
    private CrashReportSectionMixin() {}

    @org.spongepowered.asm.mixin.Overwrite(remap = false)
    public boolean method_584(StackTraceElement stackTraceElement, StackTraceElement stackTraceElement2) {
        if (stackTraceElement == null || stackTraceElement2 == null) {
            return false;
        }
        String fileName1 = stackTraceElement.getFileName();
        String fileName2 = stackTraceElement2.getFileName();
        
        if (fileName1 == null) {
            fileName1 = "<unknown>";
        }
        if (fileName2 == null) {
            fileName2 = "<unknown>";
        }

        if (stackTraceElement.getLineNumber() == stackTraceElement2.getLineNumber() && fileName1.equals(fileName2)) {
            return stackTraceElement.getClassName().equals(stackTraceElement2.getClassName()) && stackTraceElement.getMethodName().equals(stackTraceElement2.getMethodName());
        }
        
        return false;
    }
}