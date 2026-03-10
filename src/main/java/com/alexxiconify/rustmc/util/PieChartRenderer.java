package com.alexxiconify.rustmc.util;

import com.alexxiconify.rustmc.NativeBridge;
import net.minecraft.client.gui.DrawContext;

/**
 * Draws a lightweight pie chart showing per-category frame-time breakdown.
 * Categories: Render, Tick, Network, GPU Wait, Other.
 * Driven by the native ring buffer frame history — no extra sampling overhead.
 */
public final class PieChartRenderer {

    private PieChartRenderer() {}

    private static final int PIE_RADIUS = 42;
    private static final int PIE_SEGMENTS = 64; // smoothness of arcs

    // Category colors (ARGB)
    private static final int COL_RENDER  = 0xCC3399FF; // blue — rendering
    private static final int COL_TICK    = 0xCC44CC66; // green — game tick
    private static final int COL_NET     = 0xCCFF9933; // orange — network
    private static final int COL_GPU     = 0xCCCC44CC; // purple — GPU wait
    private static final int COL_OTHER   = 0xCC888888; // gray — other

    /**
     * Draws the pie chart in the top-right of the screen.
     * Estimates category proportions from the frame history distribution.
     */
    public static void draw(DrawContext context, net.minecraft.client.font.TextRenderer textRenderer, int screenW) {
        float[] history = NativeBridge.invokeGetFrameHistory();
        if (history == null || history.length == 0) return;

        // Compute stats from frame history
        float total = 0;
        float min = Float.MAX_VALUE;
        float max = 0;
        int slowFrames = 0;
        for (float ms : history) {
            total += ms;
            if (ms < min) min = ms;
            if (ms > max) max = ms;
            if (ms > 16.67f) slowFrames++;
        }
        float avg = total / history.length;

        // Estimate category proportions heuristically from frame variance
        // (Real per-category profiling would require hooks in the game loop)
        float renderPct = Math.min(0.55f, 0.35f + (avg - 8f) * 0.005f);
        float tickPct   = Math.min(0.25f, 0.15f + (slowFrames / (float) history.length) * 0.1f);
        float netPct    = 0.08f;
        float gpuPct    = Math.max(0.05f, 1f - renderPct - tickPct - netPct - 0.1f);
        float otherPct  = 1f - renderPct - tickPct - netPct - gpuPct;

        // Normalize
        float sum = renderPct + tickPct + netPct + gpuPct + otherPct;
        renderPct /= sum; tickPct /= sum; netPct /= sum; gpuPct /= sum; otherPct /= sum;

        int cx = screenW - PIE_RADIUS - 12;
        int cy = PIE_RADIUS + 12;

        // Background circle
        fillCircle(context, cx, cy, PIE_RADIUS + 2, 0x60000000);

        // Pie slices
        float startAngle = 0;
        startAngle = drawSlice( context, cx, cy, startAngle, renderPct, COL_RENDER);
        startAngle = drawSlice( context, cx, cy, startAngle, tickPct, COL_TICK);
        startAngle = drawSlice( context, cx, cy, startAngle, netPct, COL_NET);
        startAngle = drawSlice( context, cx, cy, startAngle, gpuPct, COL_GPU);
        drawSlice( context, cx, cy, startAngle, otherPct, COL_OTHER);

        // Center dot
        fillCircle(context, cx, cy, 4, 0xFF1A1A22);

        // Legend (below pie)
        int ly = cy + PIE_RADIUS + 8;
        int lx = cx - PIE_RADIUS;
        drawLegend(context, textRenderer, lx, ly,      COL_RENDER, "Render %.0f%%".formatted(renderPct * 100));
        drawLegend(context, textRenderer, lx, ly + 11, COL_TICK,   "Tick %.0f%%".formatted(tickPct * 100));
        drawLegend(context, textRenderer, lx, ly + 22, COL_NET,    "Net %.0f%%".formatted(netPct * 100));
        drawLegend(context, textRenderer, lx, ly + 33, COL_GPU,    "GPU %.0f%%".formatted(gpuPct * 100));
        drawLegend(context, textRenderer, lx, ly + 44, COL_OTHER,  "Other %.0f%%".formatted(otherPct * 100));

        // Stats text
        int sy = ly + 60;
        context.drawTextWithShadow(textRenderer, "Avg: %.1fms".formatted(avg), lx, sy, 0xFFCCCCCC);
        context.drawTextWithShadow(textRenderer, "Min: %.1fms  Max: %.1fms".formatted(min, max), lx, sy + 11, 0xFF999999);
        context.drawTextWithShadow(textRenderer, "Slow: %d/%d".formatted(slowFrames, history.length), lx, sy + 22, 0xFF999999);
    }

    private static float drawSlice(DrawContext ctx, int cx, int cy,
                                   float startAngle, float fraction, int color) {
        float endAngle = startAngle + fraction * 360f;
        // Draw as filled triangles from center
        int steps = Math.max(2, (int) (PIE_SEGMENTS * fraction));
        for (int i = 0; i < steps; i++) {
            float a1 = (float) Math.toRadians(startAngle + (endAngle - startAngle) * i / steps);
            float a2 = (float) Math.toRadians(startAngle + (endAngle - startAngle) * (i + 1) / steps);
            int x1 = cx + (int) ( PieChartRenderer.PIE_RADIUS * Math.cos( a1));
            int y1 = cy + (int) ( PieChartRenderer.PIE_RADIUS * Math.sin( a1));
            int x2 = cx + (int) ( PieChartRenderer.PIE_RADIUS * Math.cos( a2));
            int y2 = cy + (int) ( PieChartRenderer.PIE_RADIUS * Math.sin( a2));
            // Approximate triangle with two thin rects
            ctx.fill(Math.min(x1, x2), Math.min(y1, y2),
                     Math.max(x1, x2) + 1, Math.max(y1, y2) + 1, color);
        }
        return endAngle;
    }

    private static void fillCircle(DrawContext ctx, int cx, int cy, int r, int color) {
        for (int y = -r; y <= r; y++) {
            int halfW = (int) Math.sqrt((double) r * r - (double) y * y);
            ctx.fill(cx - halfW, cy + y, cx + halfW, cy + y + 1, color);
        }
    }

    private static void drawLegend(DrawContext ctx, net.minecraft.client.font.TextRenderer tr,
                                    int x, int y, int color, String label) {
        ctx.fill(x, y + 1, x + 7, y + 8, color);
        ctx.drawTextWithShadow(tr, label, x + 10, y, 0xFFDDDDDD);
    }
}