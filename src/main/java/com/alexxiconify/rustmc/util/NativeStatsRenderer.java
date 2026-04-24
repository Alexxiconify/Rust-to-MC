package com.alexxiconify.rustmc.util;
import com.alexxiconify.rustmc.NativeBridge;
import com.alexxiconify.rustmc.RustMC;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;

// Renders native metrics on screen.
public final class NativeStatsRenderer {
    private NativeStatsRenderer() {}
    private static long[] metrics = new long[3];
    private static long lastUpdate = 0;
    private static final String[] LABELS = {"JNI Calls/sec: ", "Light Updates/sec: ", "Frustum Tests/sec: "};
    public static void render(DrawContext context) {
        if (!RustMC.CONFIG.isEnableNativeMetricsHud()) return;
        long now = System.currentTimeMillis();
        if (now - lastUpdate > 500) {
            metrics = NativeBridge.getMetrics(true);
            lastUpdate = now;
        }
        MinecraftClient client = MinecraftClient.getInstance();
        TextRenderer tr = client.textRenderer;
        int x = 5;
        int y = context.getScaledWindowHeight() - 50;
        int lineCount = Math.min(LABELS.length, metrics.length);
        for (int i = 0; i < lineCount; i++) {
            String text = LABELS[i] + (metrics[i] * 2); // 500ms window -> * 2
            context.drawTextWithShadow(tr, text, x, y, 0x00FF00);
            y += 10;
        }
    }
}