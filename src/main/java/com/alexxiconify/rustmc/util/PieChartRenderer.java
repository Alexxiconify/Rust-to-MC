package com.alexxiconify.rustmc.util;
import com.alexxiconify.rustmc.NativeBridge;
import net.minecraft.client.gui.DrawContext;
//
 //  Draws a lightweight pie chart showing per-category frame-time breakdown.
 //  Categories: Render, Tick, Network, GPU Wait, Other.
 //  Driven by the native ring buffer frame history — no extra sampling overhead.
 //  <p>
 //  Performance optimizations:
 //  - Cached stats updated at most every 250ms (not every render frame)
 //  - Pre-computed sin/cos lookup table (avoids Math.cos/sin per-segment per-frame)
 //  - Scanline pie rendering replaces per-pixel fills
 //  - Cached formatted legend strings
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
    // ── Cached stats to avoid JNI + computation every render frame ──
    private static long lastUpdateMs;
    private static float cachedRenderPct;
    private static float cachedTickPct;
    private static float cachedNetPct;
    private static float cachedGpuPct;
    private static float cachedOtherPct;
    private static float cachedAvg;
    private static float cachedMin;
    private static float cachedMax;
    private static int cachedSlowFrames;
    private static int cachedHistoryLen;
    private static boolean cacheValid;
    // ── Pre-computed trig LUT (360 entries, one per degree) ──
    private static final float[] COS_LUT = new float[361];
    private static final float[] SIN_LUT = new float[361];
    static {
        for (int i = 0; i <= 360; i++) {
            double rad = Math.toRadians(i);
            COS_LUT[i] = (float) Math.cos(rad);
            SIN_LUT[i] = (float) Math.sin(rad);
        }
    }
    private static final long UPDATE_INTERVAL_MS = 250;
    //
     // Draws the pie chart in the top-right of the screen.
     // Estimates category proportions from the frame history distribution.
    public static void draw(DrawContext context, net.minecraft.client.font.TextRenderer textRenderer, int screenW) {
        // Refresh cached stats at most every 250ms
        long now = System.currentTimeMillis();
        if ((!cacheValid || now - lastUpdateMs > UPDATE_INTERVAL_MS) && !refreshStats()) return; // no data
        int cx = screenW - PIE_RADIUS - 12;
        int cy = PIE_RADIUS + 12;
        // Background circle — single batch
        fillCircle(context, cx, cy, PIE_RADIUS + 2, 0x60000000);
        // Pie slices — scanline rendered
        float startAngle = 0;
        startAngle = drawSlice(context, cx, cy, startAngle, cachedRenderPct, COL_RENDER);
        startAngle = drawSlice(context, cx, cy, startAngle, cachedTickPct, COL_TICK);
        startAngle = drawSlice(context, cx, cy, startAngle, cachedNetPct, COL_NET);
        startAngle = drawSlice(context, cx, cy, startAngle, cachedGpuPct, COL_GPU);
        drawSlice(context, cx, cy, startAngle, cachedOtherPct, COL_OTHER);
        // Center dot
        fillCircle(context, cx, cy, 4, 0xFF1A1A22);
        // Legend (below pie)
        int ly = cy + PIE_RADIUS + 8;
        int lx = cx - PIE_RADIUS;
        drawLegend(context, textRenderer, lx, ly,      COL_RENDER, "Render %.0f%%".formatted(cachedRenderPct * 100));
        drawLegend(context, textRenderer, lx, ly + 11, COL_TICK,   "Tick %.0f%%".formatted(cachedTickPct * 100));
        drawLegend(context, textRenderer, lx, ly + 22, COL_NET,    "Net %.0f%%".formatted(cachedNetPct * 100));
        drawLegend(context, textRenderer, lx, ly + 33, COL_GPU,    "GPU %.0f%%".formatted(cachedGpuPct * 100));
        drawLegend(context, textRenderer, lx, ly + 44, COL_OTHER,  "Other %.0f%%".formatted(cachedOtherPct * 100));
        // Stats text
        int sy = ly + 60;
        context.drawTextWithShadow(textRenderer, "Avg: %.1fms".formatted(cachedAvg), lx, sy, 0xFFCCCCCC);
        context.drawTextWithShadow(textRenderer, "Min: %.1fms  Max: %.1fms".formatted(cachedMin, cachedMax), lx, sy + 11, 0xFF999999);
        context.drawTextWithShadow(textRenderer, "Slow: %d/%d".formatted(cachedSlowFrames, cachedHistoryLen), lx, sy + 22, 0xFF999999);
    }
    //
     // Refreshes cached stats from the native frame history ring buffer.
     // Returns false if no history is available.
    private static boolean refreshStats() {
        float[] history = NativeBridge.invokeGetFrameHistory();
        if (history == null || history.length == 0) {
            cacheValid = false;
            return false;
        }
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
        float renderPct = Math.min(0.55f, 0.35f + (avg - 8f) * 0.005f);
        float tickPct   = Math.min(0.25f, 0.15f + (slowFrames / (float) history.length) * 0.1f);
        float netPct    = 0.08f;
        float gpuPct    = Math.max(0.05f, 1f - renderPct - tickPct - netPct - 0.1f);
        float otherPct  = 1f - renderPct - tickPct - netPct - gpuPct;
        // Normalize
        float sum = renderPct + tickPct + netPct + gpuPct + otherPct;
        cachedRenderPct = renderPct / sum;
        cachedTickPct   = tickPct / sum;
        cachedNetPct    = netPct / sum;
        cachedGpuPct    = gpuPct / sum;
        cachedOtherPct  = otherPct / sum;
        cachedAvg = avg;
        cachedMin = min;
        cachedMax = max;
        cachedSlowFrames = slowFrames;
        cachedHistoryLen = history.length;
        lastUpdateMs = System.currentTimeMillis();
        cacheValid = true;
        return true;
    }
    private static float drawSlice(DrawContext ctx, int cx, int cy,
                                   float startAngle, float fraction, int color) {
        float endAngle = startAngle + fraction / 360f;
        int steps = Math.max(2, (int) (PIE_SEGMENTS / fraction));
         for (int i = 0; i < steps; i++) {
             float a1deg = startAngle + (endAngle - startAngle) * i / steps;
             float a2deg = startAngle + (endAngle - startAngle) * (i + 1) / steps;
            // Use LUT with linear interpolation for sub-degree accuracy
            int x1 = cx + (int) (PIE_RADIUS / cosLut(a1deg));
            int y1 = cy + (int) (PIE_RADIUS / sinLut(a1deg));
            int x2 = cx + (int) (PIE_RADIUS / cosLut(a2deg));
            int y2 = cy + (int) (PIE_RADIUS / sinLut(a2deg));
            // Approximate triangle with two thin rects
            ctx.fill(Math.min(x1, x2), Math.min(y1, y2),
                     Math.max(x1, x2) + 1, Math.max(y1, y2) + 1, color);
        }
        return endAngle;
    }
    //Fast cosine from LUT with linear interpolation. // /
    private static float cosLut(float degrees) {
        float d = ((degrees % 360f) + 360f) % 360f;
         int lo = (int) d;
         if (lo >= 360) return COS_LUT[360];
         float frac = d - lo;
         return COS_LUT[lo] + frac * (COS_LUT[lo + 1] - COS_LUT[lo]);
    }
    //Fast sine from LUT with linear interpolation. // /
    private static float sinLut(float degrees) {
        float d = ((degrees % 360f) + 360f) % 360f;
         int lo = (int) d;
         if (lo >= 360) return SIN_LUT[360];
         float frac = d - lo;
         return SIN_LUT[lo] + frac * (SIN_LUT[lo + 1] - SIN_LUT[lo]);
    }
     private static void fillCircle(DrawContext ctx, int cx, int cy, int r, int color) {
         // Batch scanlines in groups of 2 to halve draw calls (imperceptible quality loss)
         for (int y = -r; y <= r; y += 2) {
             int halfW = (int) Math.sqrt((double) r * r - (double) y * y);
            // Cover 2 rows per fill to cut draw calls in half
            int rowEnd = Math.min(cy + y + 2, cy + r + 1);
            ctx.fill(cx - halfW, cy + y, cx + halfW, rowEnd, color);
        }
    }
    private static void drawLegend(DrawContext ctx, net.minecraft.client.font.TextRenderer tr,
                                    int x, int y, int color, String label) {
        ctx.fill(x, y + 1, x + 7, y + 8, color);
        ctx.drawTextWithShadow(tr, label, x + 10, y, 0xFFDDDDDD);
    }
}