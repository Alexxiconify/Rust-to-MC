package com.alexxiconify.rustmc.mixin;
import org.spongepowered.asm.mixin.Mixin;
// Target intermediary name directly — no refmap generated for installed jars
@Mixin(targets = "net.minecraft.class_129", remap = false)
public class CrashReportSectionMixin {
    private CrashReportSectionMixin() {}
    @org.spongepowered.asm.mixin.Overwrite(remap = false)
    public boolean method_584(StackTraceElement ste1, StackTraceElement ste2) {
        if (ste1 == null || ste2 == null) return false;
        // Short-circuit on line number mismatch (cheapest check first)
        if (ste1.getLineNumber() != ste2.getLineNumber()) return false;
        // Compare file names with null-safe defaults
        String file1 = ste1.getFileName();
        String file2 = ste2.getFileName();
        if (!((file1 == null ? "<unknown>" : file1).equals(file2 == null ? "<unknown>" : file2))) return false;
        // Only compare class/method names if line numbers and files match
        return ste1.getClassName().equals(ste2.getClassName()) && ste1.getMethodName().equals(ste2.getMethodName());
    }
}