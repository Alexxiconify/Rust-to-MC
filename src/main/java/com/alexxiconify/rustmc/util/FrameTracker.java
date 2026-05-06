package com.alexxiconify.rustmc.util;

import net.minecraft.client.MinecraftClient;

public final class FrameTracker {
    private static final int HISTORY_SIZE = 240;
    private static final float[] frameHistory = new float[HISTORY_SIZE];
    private static int historyHead = 0;
    private static String cachedStatsText = "0.0ms avg | 0.0 min | 0.0 max";
    private static long lastHistoryUpdateMs;
    private static final long HISTORY_UPDATE_INTERVAL_MS = 100; // 10 Hz refresh
    private static float avgMs = 0;
    private static float minMs = 0;
    private static float maxMs = 0;
    private static int slowFramesCount = 0;

    private FrameTracker() {}

    public static float[] rustmcGetFrameHistory() { return frameHistory; }
    public static float getAvgMs() { return avgMs; }
    public static float getMinMs() { return minMs; }
    public static float getMaxMs() { return maxMs; }
    public static int getSlowFramesCount() { return slowFramesCount; }
    public static int getHistorySize() { return HISTORY_SIZE; }
    public static int getHistoryHead() { return historyHead; }
    public static String getCachedStatsText() { return cachedStatsText; }

    public static void refresh(MinecraftClient mc) {
        long now = System.currentTimeMillis();
        if (now - lastHistoryUpdateMs < HISTORY_UPDATE_INTERVAL_MS) return;
        int fps = mc.getCurrentFps();
        float ms = fps > 0 ? 1000.0f / fps : 0.0f;
        frameHistory[historyHead] = ms;
        historyHead = (historyHead + 1) % HISTORY_SIZE;
        // Compute stats over populated history
        float sum = 0;
        float min = Float.MAX_VALUE;
        float max = 0;
        int count = 0;
        int slow = 0;
        for (float v : frameHistory) {
            if (v <= 0) continue;
            sum += v;
            min = Math.min(min, v);
            max = Math.max(max, v);
            if (v > 16.6f) slow++;
            count++;
        }
        if (count == 0) {
            avgMs = minMs = maxMs = 0;
            slowFramesCount = 0;
            cachedStatsText = "0.0ms avg | 0.0 min | 0.0 max";
        } else {
            avgMs = sum / count;
            minMs = min;
            maxMs = max;
            slowFramesCount = slow;
            cachedStatsText = "%.1fms avg | %.1f min | %.1f max".formatted(avgMs, minMs, maxMs);
        }
        lastHistoryUpdateMs = now;
    }
}







