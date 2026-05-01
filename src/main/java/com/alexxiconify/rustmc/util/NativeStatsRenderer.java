package com.alexxiconify.rustmc.util;
import com.alexxiconify.rustmc.NativeBridge;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;

// Renders native metrics on screen.
public final class NativeStatsRenderer {
    private NativeStatsRenderer() {}
    private static long[] metrics = new long[5];
    private static long lastUpdate = 0;
    private static final String[] LABELS = {
        "JNI Calls: ",
        "Light Updates: ",
        "Frustum Tests: ",
        "Chunk Packets: ",
        "Chunk Bytes: "
    };
    public static void render(DrawContext context) {
        long now = System.currentTimeMillis();
        if (now - lastUpdate > 100) {
            metrics = NativeBridge.getMetrics(true);
            lastUpdate = now;
        }
        MinecraftClient client = MinecraftClient.getInstance();
        TextRenderer tr = client.textRenderer;
        int x = 5;
        int y = context.getScaledWindowHeight() - 60;
        for (int i = 0; i < LABELS.length; i++) {
            long val = (i < metrics.length) ? metrics[i] : 0;
            String text = LABELS[i] + (val * 10) + "/s";
            if (i == 4) { // Bytes to KB
                text = LABELS[i] + String.format("%.1f KB/s", (val * 10) / 1024.0);
            }
            context.drawTextWithShadow(tr, text, x, y, 0xFF00FF00);
            y += 10;
        }
    }
}