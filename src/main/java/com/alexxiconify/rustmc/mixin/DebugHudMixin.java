package com.alexxiconify.rustmc.mixin;

import com.alexxiconify.rustmc.ModBridge;
import com.alexxiconify.rustmc.NativeBridge;
import com.alexxiconify.rustmc.RustMC;
import com.alexxiconify.rustmc.util.PieChartRenderer;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.hud.DebugHud;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Draws a compact frame-time sparkline graph in the F3 overlay when enabled.
 * The frame history is maintained in the Rust native core as a ring buffer
 * (240 samples). Each bar represents one frame's ms time.
 * <p>
 * Uses require=0 to gracefully skip if DebugHud.render changes signature
 * across MC versions (the method was refactored in 1.21.11).
 */
@SuppressWarnings("ALL")
@Mixin(DebugHud.class)
public class DebugHudMixin {

    // ── Cached sparkline data (avoids JNI every render frame) ──
    @Unique private static float[] cachedHistory;
    @Unique private static float cachedAvg;
    @Unique private static float cachedMin;
    @Unique private static float cachedMax;
    @Unique private static long lastHistoryUpdateMs;
    @Unique private static final long HISTORY_UPDATE_INTERVAL_MS = 100; // 10 Hz refresh

    @Inject(method = "render", at = @At("TAIL"), require = 0)
    private void onRenderTail(DrawContext context, CallbackInfo ci) {
        if (!NativeBridge.isReady()) return;

        // Only render overlays when actually in a world — suppress on title screen,
        // multiplayer list, singleplayer list, and other menu screens.
        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc.world == null || mc.player == null) return;

        // Early exit if neither overlay is enabled — avoids any JNI/rendering overhead
        if (!RustMC.CONFIG.isEnableDebugHudGraph() && !RustMC.CONFIG.isEnablePieChart()) return;

        // ── Sparkline graph (F8 toggle) ──
        if (RustMC.CONFIG.isEnableDebugHudGraph() && RustMC.CONFIG.isUseNativeF3() && !ModBridge.isHudOwned()) {
            drawSparkline(context);
        }

        // ── Pie chart (F7 toggle) ──
        if (RustMC.CONFIG.isEnablePieChart() && mc.textRenderer != null) {
            int screenW = context.getScaledWindowWidth();
            PieChartRenderer.draw(context, mc.textRenderer, screenW);
        }
    }

    @Inject(method = "getLeftText", at = @At("RETURN"), cancellable = true, require = 0)
    private void onGetLeftText(CallbackInfoReturnable<java.util.List<String>> cir) {
        java.util.List<String> list = cir.getReturnValue();
        if (list == null || list.isEmpty()) return;

        // BetterF3 or other HUD mods might provide an immutable list. 
        // We create a fresh copy to ensure we can append our info without side effects.
        java.util.List<String> newList = new java.util.ArrayList<>(list);
        
        try {
            newList.add("");
            newList.add("§6[Rust-MC] General Info§r");
            newList.add("Native Status: " + (NativeBridge.isReady() ? "§aReady§r" : "§cOffline§r"));
            newList.add("Native Frustum Calls/frame: " + NativeBridge.frustumChecksThisFrame.getAndSet(0));
            newList.add(String.format("Tweaks: Culling=%s | Light=%s | F3Sync=%s",
                    RustMC.CONFIG.isUseNativeCulling() ? "§aOn§r" : "§cOff§r",
                    RustMC.CONFIG.isUseNativeLighting() ? "§aOn§r" : "§cOff§r",
                    RustMC.CONFIG.isUseNativeF3() ? "§aOn§r" : "§cOff§r"
            ));
            newList.add(String.format("Compat: DH=%s SLX=%s",
                    ModBridge.DISTANT_HORIZONS ? "§aOn§r" : "§cOff§r",
                    ModBridge.SCALABLELUX ? "§aOn§r" : "§cOff§r"
            ));
            
            cir.setReturnValue(newList);
        } catch (Exception ignored) {
            // Silently fail if something goes wrong during list construction
        }
    }

    @Unique
    private static void refreshHistoryCache() {
        long now = System.currentTimeMillis();
        if (cachedHistory != null && now - lastHistoryUpdateMs < HISTORY_UPDATE_INTERVAL_MS) return;

        float[] history = NativeBridge.invokeGetFrameHistory();
        if (history == null || history.length == 0) {
            cachedHistory = null;
            return;
        }

        cachedHistory = history;
        int len = Math.min(history.length, 240);
        float total = 0;
        float min = Float.MAX_VALUE;
        float max = 0;
        for (int i = 0; i < len; i++) {
            float ms = history[i];
            total += ms;
            if (ms < min) min = ms;
            if (ms > max) max = ms;
        }
        cachedAvg = total / len;
        cachedMin = min;
        cachedMax = max;
        lastHistoryUpdateMs = now;
    }

    @Unique
    private static void drawSparkline(DrawContext context) {
        refreshHistoryCache();
        if (cachedHistory == null) return;

        MinecraftClient mc = MinecraftClient.getInstance();
        int graphW = Math.min(cachedHistory.length, 240);
        int graphH = 40;
        int x0 = 2;
        int y0 = 2;

        // Background
        context.fill(x0 - 1, y0 - 1, x0 + graphW + 1, y0 + graphH + 1, 0x80000000);

        // 16.67ms target line (60 FPS)
        int targetY = y0 + graphH - (int)(16.67f / 50.0f * graphH);
        context.fill(x0, targetY, x0 + graphW, targetY + 1, 0x4000FF00);

        // 33.33ms target line (30 FPS)
        int target30Y = y0 + graphH - (int)(33.33f / 50.0f * graphH);
        context.fill(x0, target30Y, x0 + graphW, target30Y + 1, 0x40FFFF00);

        // Draw bars
        for (int i = 0; i < graphW; i++) {
            float ms = cachedHistory[i];
            int barH = Math.clamp((int)(ms / 50.0f * graphH), 1, graphH);
            int barY = y0 + graphH - barH;
            int color;
            if (ms < 16.67f) {
                color = 0xFF00CC44; // Green — above 60 FPS
            } else if (ms < 33.33f) {
                color = 0xFFCCAA00; // Yellow — 30-60 FPS
            } else {
                color = 0xFFCC2222; // Red — below 30 FPS
            }
            context.fill(x0 + i, barY, x0 + i + 1, y0 + graphH, color);
        }

        // ── Legend (right side of graph) ──
        if (mc.textRenderer != null) {
            // Target line labels
            context.drawTextWithShadow(mc.textRenderer, "60", x0 + graphW + 2, targetY - 4, 0xFF00CC44);
            context.drawTextWithShadow(mc.textRenderer, "30", x0 + graphW + 2, target30Y - 4, 0xFFCCAA00);

            // Stats below graph — use cached values
            int statsY = y0 + graphH + 3;
            context.drawTextWithShadow(mc.textRenderer,
                    "%.1fms avg | %.1f min | %.1f max".formatted(cachedAvg, cachedMin, cachedMax),
                    x0, statsY, 0xFFAAAAAA);

            // Color legend
            int legendY = statsY + 11;
            context.fill(x0, legendY + 1, x0 + 6, legendY + 7, 0xFF00CC44);
            context.drawTextWithShadow(mc.textRenderer, ">60", x0 + 8, legendY, 0xFF00CC44);
            context.fill(x0 + 38, legendY + 1, x0 + 44, legendY + 7, 0xFFCCAA00);
            context.drawTextWithShadow(mc.textRenderer, "30-60", x0 + 46, legendY, 0xFFCCAA00);
            context.fill(x0 + 88, legendY + 1, x0 + 94, legendY + 7, 0xFFCC2222);
            context.drawTextWithShadow(mc.textRenderer, "<30", x0 + 96, legendY, 0xFFCC2222);
        }
    }
}