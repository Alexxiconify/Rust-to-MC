package com.alexxiconify.rustmc.util;

import com.alexxiconify.rustmc.NativeBridge;
import com.alexxiconify.rustmc.ModBridge;
import com.alexxiconify.rustmc.RustMC;
import com.alexxiconify.rustmc.compat.DistantHorizonsCompat;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;

/**
 * Unified HUD manager: timing diagnostics, native metrics, memory bar, and render state.
 * Consolidates DiagnosticHudRenderer + NativeStatsRenderer + RamBarRenderer + RenderState.
 */
public final class HudManager {
    private HudManager() {}

    // --- Render State (shared render-pass coordination) ---
    // Isolated render state to prevent mixin-class issues with non-private static fields
    @SuppressWarnings({"java:S1104", "java:S1444"})
    public static final class RenderState {
        private RenderState() {}
        public static volatile boolean heavyEntityModsActive = false;
        public static volatile boolean immediatelyFastActive = false;
        public static volatile int renderBudgetTier = 0;
    }

    // Timing cache
    private static long lastTimingUpdate;
    private static String cachedRenderLabel = "Render 0%";
    private static String cachedTickLabel = "Tick 0%";
    private static String cachedNetLabel = "Net 0%";
    private static String cachedGpuLabel = "GPU 0%";
    private static String cachedOtherLabel = "Other 0%";
    private static String cachedAvgLabel = "Avg: 0.0ms";
    private static String cachedMinMaxLabel = "Min: 0.0ms  Max: 0.0ms";
    private static String cachedSlowLabel = "Slow: 0/0";
    private static String cachedDhLabel = "DH: OFF";
    private static String cachedFrustumLabel = "Frust: INIT";
    private static String cachedJniLabel = "JNI: 0.0ms";
    private static boolean timingCacheValid;

    // Native metrics cache
    private static long[] nativeMetrics = new long[5];
    private static long lastNativeUpdate;
    private static final String[] NATIVE_LABELS = {
        "JNI Calls: ", "Light Updates: ", "Frustum Tests: ", "Chunk Packets: ", "Chunk Bytes: "
    };

    // RAM cache
    private static final Runtime RUNTIME = Runtime.getRuntime();
    private static long cachedUsedMb = -1L;
    private static long cachedMaxMb = -1L;
    private static int cachedRamPct = -1;
    private static String cachedRamText = "RAM 0MB / 0MB (0%)";

    private static final long TIMING_INTERVAL = 250;
    private static final long NATIVE_INTERVAL = 100;

    // --- Main entry: render all enabled overlays
    public static void render(DrawContext context, TextRenderer tr,
                              boolean showTiming, boolean showNative, boolean showRam) {
        if (showTiming) drawTimingOverlay(context, tr);
        if (showNative) drawNativeMetrics(context, tr);
        if (showRam)    drawRamBar(context, tr);
    }

    // --- Compat wrapper for legacy RamBarRenderer.drawRamBar() calls
    public static void drawRamBarCompat(DrawContext context, TextRenderer tr, int screenW, int screenH, int bgColor) {
        long used = RUNTIME.totalMemory() - RUNTIME.freeMemory();
        long max = RUNTIME.maxMemory();
        float ratio = (float) used / max;
        int barW = Math.min(400, screenW - 20);
        int barH = 5;
        int bx = (screenW - barW) / 2;
        int yOffset = (ModBridge.APPLESKIN && RustMC.CONFIG.isEnableAppleSkinCompat()) ? 14 : 0;
        int by = screenH - 22 - yOffset;

        context.fill(bx, by, bx + barW, by + barH, bgColor);
        context.fill(bx, by, bx + (int)(barW * ratio), by + barH, ramColor(ratio));
    }

    // --- Timing diagnostics
    private static void drawTimingOverlay(DrawContext context, TextRenderer textRenderer) {
        long now = System.currentTimeMillis();
        if ((!timingCacheValid || now - lastTimingUpdate > TIMING_INTERVAL) && !refreshTimingStats()) {
            return;
        }

        int screenW = context.getScaledWindowWidth();
        int maxWidth = Math.max(textRenderer.getWidth("Timing Info"),
                    Math.max(textRenderer.getWidth(cachedAvgLabel),
                    Math.max(textRenderer.getWidth(cachedMinMaxLabel),
                    Math.max(textRenderer.getWidth(cachedSlowLabel), 150))));

        int x = screenW - maxWidth - 10;
        int y = 6;
        int height = 10 * 12 + 6;

        context.fill(x - 4, y - 3, x + maxWidth + 4, y + height, 0x70000000);
        drawText(context, textRenderer, "Timing Info", x, y, 0xFF33CCFF);
        drawText(context, textRenderer, cachedAvgLabel, x, y + 10, 0xFFCCCCCC);
        drawText(context, textRenderer, cachedMinMaxLabel, x, y + 20, 0xFFCCCCCC);
        drawText(context, textRenderer, cachedSlowLabel, x, y + 30, 0xFFCCCCCC);
        drawText(context, textRenderer, cachedRenderLabel, x, y + 40, 0xFFAAAAAA);
        drawText(context, textRenderer, cachedTickLabel, x, y + 50, 0xFFAAAAAA);
        drawText(context, textRenderer, cachedNetLabel, x, y + 60, 0xFFAAAAAA);
        drawText(context, textRenderer, cachedGpuLabel, x, y + 70, 0xFFAAAAAA);
        drawText(context, textRenderer, cachedOtherLabel, x, y + 80, 0xFFAAAAAA);
        drawText(context, textRenderer, cachedDhLabel, x, y + 90, 0xFF55FF55);
        drawText(context, textRenderer, cachedFrustumLabel, x, y + 100, 0xFFFFFF55);
        drawText(context, textRenderer, cachedJniLabel, x, y + 110, 0xFFFFAA55);
    }

    // --- Native metrics (JNI calls, light updates, frustum tests, etc.)
    private static void drawNativeMetrics(DrawContext context, TextRenderer textRenderer) {
        long now = System.currentTimeMillis();
        if (now - lastNativeUpdate > NATIVE_INTERVAL) {
            nativeMetrics = NativeBridge.getMetrics(true);
            lastNativeUpdate = now;
        }

        int x = 5;
        int y = context.getScaledWindowHeight() - 60;
        for (int i = 0; i < NATIVE_LABELS.length; i++) {
            long val = (i < nativeMetrics.length) ? nativeMetrics[i] : 0;
            String text = NATIVE_LABELS[i] + (val * 10) + "/s";
            if (i == 4) text = NATIVE_LABELS[i] + String.format("%.1f KB/s", (val * 10) / 1024.0);
            drawText(context, textRenderer, text, x, y, 0xFF00FF00);
            y += 10;
        }
    }

    // --- RAM bar (memory usage visualization)
    private static void drawRamBar(DrawContext context, TextRenderer textRenderer) {
        long used = RUNTIME.totalMemory() - RUNTIME.freeMemory();
        long max = RUNTIME.maxMemory();
        float ratio = (float) used / max;
        long usedMb = used >> 20;
        long maxMb = max >> 20;
        int pct = Math.round(ratio * 100.0f);

        // Cache to avoid string recreation
        if (usedMb != cachedUsedMb || maxMb != cachedMaxMb || pct != cachedRamPct) {
            cachedUsedMb = usedMb;
            cachedMaxMb = maxMb;
            cachedRamPct = pct;
            cachedRamText = "RAM " + usedMb + "MB / " + maxMb + "MB (" + pct + "%)";
        }

        int screenW = context.getScaledWindowWidth();
        int screenH = context.getScaledWindowHeight();
        int barW = Math.min(400, screenW - 20);
        int barH = 5;
        int bx = (screenW - barW) / 2;
        int yOffset = (ModBridge.APPLESKIN && RustMC.CONFIG.isEnableAppleSkinCompat()) ? 14 : 0;
        int by = screenH - 22 - yOffset;

        int bgColor = RustMC.CONFIG.getLoadingBarBgColor();
        int barColor = ramColor(ratio);
        int textColor = RustMC.CONFIG.getLoadingBarTextColor();

        context.fill(bx, by, bx + barW, by + barH, bgColor);
        context.fill(bx, by, bx + (int)(barW * ratio), by + barH, barColor);
        context.drawCenteredTextWithShadow(textRenderer, cachedRamText, screenW / 2, by + barH + 2, textColor);
    }

    // --- Helpers
    private static int ramColor(float ratio) {
        if (ratio < 0.6f) return RustMC.CONFIG.getLoadingBarLowColor();
        if (ratio < 0.8f) return RustMC.CONFIG.getLoadingBarMidColor();
        return RustMC.CONFIG.getLoadingBarHighColor();
    }

    private static void drawText(DrawContext context, TextRenderer tr, String text, int x, int y, int color) {
        context.drawTextWithShadow(tr, text, x, y, color);
    }

    private static boolean refreshTimingStats() {
        float[] history = FrameTracker.rustmcGetFrameHistory();
        if (history == null || history.length == 0) {
            timingCacheValid = false;
            return false;
        }

        float avg = FrameTracker.getAvgMs();
        float min = FrameTracker.getMinMs();
        float max = FrameTracker.getMaxMs();
        int slowFrames = FrameTracker.getSlowFramesCount();

        // Estimate time proportions
        float renderPct = Math.min(0.55f, 0.35f + (avg - 8f) * 0.005f);
        float tickPct = Math.min(0.25f, 0.15f + (slowFrames / (float) history.length) * 0.1f);
        float netPct = 0.08f;
        float gpuPct = Math.max(0.05f, 1f - renderPct - tickPct - netPct - 0.1f);
        float otherPct = 1f - renderPct - tickPct - netPct - gpuPct;

        float sum = renderPct + tickPct + netPct + gpuPct + otherPct;
        cachedRenderLabel = formatPercent("Render", renderPct / sum);
        cachedTickLabel = formatPercent("Tick", tickPct / sum);
        cachedNetLabel = formatPercent("Net", netPct / sum);
        cachedGpuLabel = formatPercent("GPU", gpuPct / sum);
        cachedOtherLabel = formatPercent("Other", otherPct / sum);

        cachedAvgLabel = "Avg: " + formatMs(avg) + "ms";
        cachedMinMaxLabel = "Min: " + formatMs(min) + "ms  Max: " + formatMs(max) + "ms";
        cachedSlowLabel = "Slow: " + slowFrames + "/" + history.length;

        if (ModBridge.DISTANT_HORIZONS) {
            String reason = DistantHorizonsCompat.getLastRefreshReason();
            boolean init = DistantHorizonsCompat.isFrustumInitialized();
            double move = DistantHorizonsCompat.getLastCameraMoveSq();
            cachedDhLabel = "DH: ACTIVE (" + reason + ")";
            cachedFrustumLabel = "Frust: " + (init ? "READY" : "WAIT") + " m=" + formatMs((float)move);
            cachedJniLabel = "JNI: ACTIVE";
        } else {
            cachedDhLabel = "DH: NOT FOUND";
            cachedFrustumLabel = "Frust: N/A";
            cachedJniLabel = "JNI: N/A";
        }
        lastTimingUpdate = System.currentTimeMillis();
        timingCacheValid = true;
        return true;
    }

    private static String formatPercent(String label, float fraction) {
        return label + " " + Math.round(fraction * 100.0f) + "%";
    }

    private static String formatMs(float value) {
        int tenths = Math.round(value * 10.0f);
        return (tenths / 10) + "." + Math.abs(tenths % 10);
    }
}