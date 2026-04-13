package com.alexxiconify.rustmc.util;
import com.alexxiconify.rustmc.ModBridge;
import com.alexxiconify.rustmc.RustMC;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;
public final class RamBarRenderer {
    private RamBarRenderer() {}
    //Picks a color based on the memory usage ratio (0–1). // /
    public static int ramColor(float ratio) {
        if (ratio < 0.6f) return RustMC.CONFIG.getLoadingBarLowColor();
        if (ratio < 0.8f) return RustMC.CONFIG.getLoadingBarMidColor();
        return RustMC.CONFIG.getLoadingBarHighColor();
    }
    public static void drawRamBar(DrawContext context, TextRenderer textRenderer,
                                  int screenW, int screenH, int bgColor) {
        long used = Runtime.getRuntime().totalMemory() - Runtime.getRuntime().freeMemory();
        long max  = Runtime.getRuntime().maxMemory();
        float ratio = (float) used / max;
        int barW = Math.min(400, screenW - 20);
        int barH = 5;
        int bx   = (screenW - barW) / 2;
        // When AppleSkin is installed and compat is enabled, offset up to avoid its HUD overlay
        int yOffset = (ModBridge.APPLESKIN && RustMC.CONFIG.isEnableAppleSkinCompat()) ? 14 : 0;
         int by   = screenH - 22 - yOffset;
         context.fill(bx, by, bx + barW, by + barH, bgColor);
         context.fill(bx, by, bx + (int)(barW * ratio), by + barH, ramColor(ratio));
         String ramText = String.format("RAM %dMB / %dMB (%.0f%%)", used >> 20, max >> 20, ratio * 100f);
        context.drawCenteredTextWithShadow(textRenderer, ramText, screenW / 2, by + barH + 2,
                RustMC.CONFIG.getLoadingBarTextColor());
    }
}