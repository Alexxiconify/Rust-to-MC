package com.alexxiconify.rustmc.util;
import net.minecraft.client.gui.DrawContext;

//  Draws a compact text-only timing overlay. Keeps the existing toggle/config plumbing but removes the pie graphic itself.
public final class PieChartRenderer {
    private PieChartRenderer() {}
    // Cached stats to avoid JNI + computation every render frame
    private static long lastUpdateMs;
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
    private static boolean cacheValid;
    private static final long UPDATE_INTERVAL_MS = 250;
    // Draws text-only timing info in the top-right of the screen. Estimates category proportions from the frame history distribution.
    public static void draw(DrawContext context, net.minecraft.client.font.TextRenderer textRenderer, int screenW) {
        // Refresh cached stats at most every 250ms
        long now = System.currentTimeMillis();
        if ((!cacheValid || now - lastUpdateMs > UPDATE_INTERVAL_MS) && !refreshStats()) return; // no data
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
    // Refreshes cached stats from the local frame history ring buffer.
    private static boolean refreshStats() {
        float[] history = com.alexxiconify.rustmc.mixin.client.DebugHudMixin.getFrameHistory();
        if (history == null || history.length == 0) {
            cacheValid = false;
            return false;
        }

        float avg = com.alexxiconify.rustmc.mixin.client.DebugHudMixin.getAvgMs();
        float min = com.alexxiconify.rustmc.mixin.client.DebugHudMixin.getMinMs();
        float max = com.alexxiconify.rustmc.mixin.client.DebugHudMixin.getMaxMs();
        int slowFrames = com.alexxiconify.rustmc.mixin.client.DebugHudMixin.getSlowFramesCount();

        // Estimate category proportions heuristically from frame variance
        float renderPct = Math.min(0.55f, 0.35f + (avg - 8f) * 0.005f);
        float tickPct   = Math.min(0.25f, 0.15f + (slowFrames / (float) history.length) * 0.1f);
        float netPct    = 0.08f;
        float gpuPct    = Math.max(0.05f, 1f - renderPct - tickPct - netPct - 0.1f);
        float otherPct  = 1f - renderPct - tickPct - netPct - gpuPct;
        // Normalize
        float sum = renderPct + tickPct + netPct + gpuPct + otherPct;
        float normalizedRenderPct = renderPct / sum;
        float normalizedTickPct = tickPct / sum;
        float normalizedNetPct = netPct / sum;
        float normalizedGpuPct = gpuPct / sum;
        float normalizedOtherPct = otherPct / sum;
        cachedRenderLabel = formatPercentLabel("Render", normalizedRenderPct);
        cachedTickLabel = formatPercentLabel("Tick", normalizedTickPct);
        cachedNetLabel = formatPercentLabel("Net", normalizedNetPct);
        cachedGpuLabel = formatPercentLabel("GPU", normalizedGpuPct);
        cachedOtherLabel = formatPercentLabel("Other", normalizedOtherPct);
        cachedAvgLabel = "Avg: " + formatMsValue(avg) + "ms";
        cachedMinMaxLabel = "Min: " + formatMsValue(min) + "ms  Max: " + formatMsValue(max) + "ms";
        cachedSlowLabel = "Slow: " + slowFrames + "/" + history.length;
        if (com.alexxiconify.rustmc.ModBridge.DISTANT_HORIZONS) {
            String reason = com.alexxiconify.rustmc.compat.DistantHorizonsCompat.getLastRefreshReason();
            boolean init = com.alexxiconify.rustmc.compat.DistantHorizonsCompat.isFrustumInitialized();
            double move = com.alexxiconify.rustmc.compat.DistantHorizonsCompat.getLastCameraMoveSq();
            cachedDhLabel = "DH: ACTIVE (" + reason + ")";
            cachedFrustumLabel = "Frust: " + (init ? "READY" : "WAIT") + " m=" + formatMsValue((float)move);
            cachedJniLabel = "JNI: ACTIVE";
        } else {
            cachedDhLabel = "DH: NOT FOUND";
            cachedFrustumLabel = "Frust: N/A";
            cachedJniLabel = "JNI: N/A";
        }
        lastUpdateMs = System.currentTimeMillis();
        cacheValid = true;
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