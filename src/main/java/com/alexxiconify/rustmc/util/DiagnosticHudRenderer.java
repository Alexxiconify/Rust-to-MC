package com.alexxiconify.rustmc.util;

import com.alexxiconify.rustmc.NativeBridge;
import com.alexxiconify.rustmc.ModBridge;
import com.alexxiconify.rustmc.compat.DistantHorizonsCompat;
import com.alexxiconify.rustmc.mixin.client.DebugHudMixin;

import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;

/**
 * Unified diagnostic HUD renderer that handles both timing information
 * and native JNI metrics.
 */
public final class DiagnosticHudRenderer {
    private DiagnosticHudRenderer() {}

    // Timing Cache
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

    // Native Metrics Cache
    private static long[] nativeMetrics = new long[5];
    private static long lastNativeUpdate;
    private static final String[] NATIVE_LABELS = {
        "JNI Calls: ",
        "Light Updates: ",
        "Frustum Tests: ",
        "Chunk Packets: ",
        "Chunk Bytes: "
    };

    private static final long TIMING_INTERVAL = 250;
    private static final long NATIVE_INTERVAL = 100;

    public static void render(DrawContext context, TextRenderer textRenderer, boolean showTiming, boolean showNative) {
        if (showTiming) {
            drawTimingOverlay(context, textRenderer);
        }
        if (showNative) {
            drawNativeMetrics(context, textRenderer);
        }
    }

    private static void drawTimingOverlay(DrawContext context, TextRenderer textRenderer) {
        long now = System.currentTimeMillis();
        if ((!timingCacheValid || now - lastTimingUpdate > TIMING_INTERVAL) && !refreshTimingStats()) {
            return;
        }

        int screenW = context.getScaledWindowWidth();
        int maxWidth = textRenderer.getWidth("Timing Info");
        maxWidth = Math.max(maxWidth, textRenderer.getWidth(cachedAvgLabel));
        maxWidth = Math.max(maxWidth, textRenderer.getWidth(cachedMinMaxLabel));
        maxWidth = Math.max(maxWidth, textRenderer.getWidth(cachedSlowLabel));
        maxWidth = Math.max(maxWidth, textRenderer.getWidth(cachedRenderLabel));
        maxWidth = Math.max(maxWidth, textRenderer.getWidth(cachedTickLabel));
        maxWidth = Math.max(maxWidth, textRenderer.getWidth(cachedNetLabel));
        maxWidth = Math.max(maxWidth, textRenderer.getWidth(cachedGpuLabel));
        maxWidth = Math.max(maxWidth, textRenderer.getWidth(cachedOtherLabel));
        maxWidth = Math.max(maxWidth, textRenderer.getWidth(cachedDhLabel));
        maxWidth = Math.max(maxWidth, textRenderer.getWidth(cachedFrustumLabel));

        int x = screenW - maxWidth - 10;
        int y = 6;
        int height = 10 * 12 + 6;

        context.fill(x - 4, y - 3, x + maxWidth + 4, y + height, 0x70000000);
        context.drawTextWithShadow(textRenderer, "Timing Info", x, y, 0xFF33CCFF);
        context.drawTextWithShadow(textRenderer, cachedAvgLabel, x, y + 10, 0xFFCCCCCC);
        context.drawTextWithShadow(textRenderer, cachedMinMaxLabel, x, y + 20, 0xFFCCCCCC);
        context.drawTextWithShadow(textRenderer, cachedSlowLabel, x, y + 30, 0xFFCCCCCC);
        context.drawTextWithShadow(textRenderer, cachedRenderLabel, x, y + 40, 0xFFAAAAAA);
        context.drawTextWithShadow(textRenderer, cachedTickLabel, x, y + 50, 0xFFAAAAAA);
        context.drawTextWithShadow(textRenderer, cachedNetLabel, x, y + 60, 0xFFAAAAAA);
        context.drawTextWithShadow(textRenderer, cachedGpuLabel, x, y + 70, 0xFFAAAAAA);
        context.drawTextWithShadow(textRenderer, cachedOtherLabel, x, y + 80, 0xFFAAAAAA);
        context.drawTextWithShadow(textRenderer, cachedDhLabel, x, y + 90, 0xFF55FF55);
        context.drawTextWithShadow(textRenderer, cachedFrustumLabel, x, y + 100, 0xFFFFFF55);
        context.drawTextWithShadow(textRenderer, cachedJniLabel, x, y + 110, 0xFFFFAA55);
    }

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
            if (i == 4) { // Bytes to KB
                text = NATIVE_LABELS[i] + String.format("%.1f KB/s", (val * 10) / 1024.0);
            }
            context.drawTextWithShadow(textRenderer, text, x, y, 0xFF00FF00);
            y += 10;
        }
    }

    private static boolean refreshTimingStats() {
        float[] history = DebugHudMixin.getFrameHistory();
        if (history == null || history.length == 0) {
            timingCacheValid = false;
            return false;
        }

        float avg = DebugHudMixin.getAvgMs();
        float min = DebugHudMixin.getMinMs();
        float max = DebugHudMixin.getMaxMs();
        int slowFrames = DebugHudMixin.getSlowFramesCount();

        // Estimate proportions
        float renderPct = Math.min(0.55f, 0.35f + (avg - 8f) * 0.005f);
        float tickPct   = Math.min(0.25f, 0.15f + (slowFrames / (float) history.length) * 0.1f);
        float netPct    = 0.08f;
        float gpuPct    = Math.max(0.05f, 1f - renderPct - tickPct - netPct - 0.1f);
        float otherPct  = 1f - renderPct - tickPct - netPct - gpuPct;

        float sum = renderPct + tickPct + netPct + gpuPct + otherPct;
        cachedRenderLabel = formatPercentLabel("Render", renderPct / sum);
        cachedTickLabel = formatPercentLabel("Tick", tickPct / sum);
        cachedNetLabel = formatPercentLabel("Net", netPct / sum);
        cachedGpuLabel = formatPercentLabel("GPU", gpuPct / sum);
        cachedOtherLabel = formatPercentLabel("Other", otherPct / sum);

        cachedAvgLabel = "Avg: " + formatMsValue(avg) + "ms";
        cachedMinMaxLabel = "Min: " + formatMsValue(min) + "ms  Max: " + formatMsValue(max) + "ms";
        cachedSlowLabel = "Slow: " + slowFrames + "/" + history.length;

        if (ModBridge.DISTANT_HORIZONS) {
            String reason = DistantHorizonsCompat.getLastRefreshReason();
            boolean init = DistantHorizonsCompat.isFrustumInitialized();
            double move = DistantHorizonsCompat.getLastCameraMoveSq();
            cachedDhLabel = "DH: ACTIVE (" + reason + ")";
            cachedFrustumLabel = "Frust: " + (init ? "READY" : "WAIT") + " m=" + formatMsValue((float)move);
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

    private static String formatPercentLabel(String label, float fraction) {
        return label + " " + Math.round(fraction * 100.0f) + "%";
    }

    private static String formatMsValue(float value) {
        int tenths = Math.round(value * 10.0f);
        int whole = tenths / 10;
        int frac = Math.abs(tenths % 10);
        return whole + "." + frac;
    }
}
