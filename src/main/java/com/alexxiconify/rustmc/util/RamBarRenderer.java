package com.alexxiconify.rustmc.util;
import com.alexxiconify.rustmc.ModBridge;
import com.alexxiconify.rustmc.RustMC;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;
public final class RamBarRenderer {
    private RamBarRenderer() {}
    private static final Runtime RUNTIME = Runtime.getRuntime();
    private static long cachedUsedMb = -1L;
    private static long cachedMaxMb = -1L;
    private static int cachedPct = -1;
    private static String cachedRamText = "RAM 0MB / 0MB (0%)";
    //Picks a color based on the memory usage ratio (0–1). // /
    public static int ramColor(float ratio) {
        if (ratio < 0.6f) return RustMC.CONFIG.getLoadingBarLowColor();
        if (ratio < 0.8f) return RustMC.CONFIG.getLoadingBarMidColor();
        return RustMC.CONFIG.getLoadingBarHighColor();
    }
    private static String ramText(long usedMb, long maxMb, int pct) {
        if (usedMb == cachedUsedMb && maxMb == cachedMaxMb && pct == cachedPct) {
            return cachedRamText;
        }
        cachedUsedMb = usedMb;
        cachedMaxMb = maxMb;
        cachedPct = pct;
        cachedRamText = "RAM " + usedMb + "MB / " + maxMb + "MB (" + pct + "%)";
        return cachedRamText;
    }
    public static void drawRamBar(DrawContext context, TextRenderer textRenderer,
                                  int screenW, int screenH, int bgColor) {
        long used = RUNTIME.totalMemory() - RUNTIME.freeMemory();
        long max  = RUNTIME.maxMemory();
        float ratio = (float) used / max;
        long usedMb = used >> 20;
        long maxMb = max >> 20;
        int pct = Math.round(ratio * 100.0f);
        int barW = Math.min(400, screenW - 20);
        int barH = 5;
        int bx   = (screenW - barW) / 2;
        // When AppleSkin is installed and compat is enabled, offset up to avoid its HUD overlay
        int yOffset = (ModBridge.APPLESKIN && RustMC.CONFIG.isEnableAppleSkinCompat()) ? 14 : 0;
         int by   = screenH - 22 - yOffset;
         context.fill(bx, by, bx + barW, by + barH, bgColor);
         context.fill(bx, by, bx + (int)(barW * ratio), by + barH, ramColor(ratio));
        context.drawCenteredTextWithShadow(textRenderer, ramText(usedMb, maxMb, pct), screenW / 2, by + barH + 2,
                RustMC.CONFIG.getLoadingBarTextColor());
    }
}