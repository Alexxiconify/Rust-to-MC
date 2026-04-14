package com.alexxiconify.rustmc.mixin.client;
import com.alexxiconify.rustmc.ModBridge;
import com.alexxiconify.rustmc.NativeBridge;
import com.alexxiconify.rustmc.RustMC;
import com.alexxiconify.rustmc.util.PieChartRenderer;
import com.alexxiconify.rustmc.util.RenderState;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.hud.DebugHud;
import net.minecraft.client.render.WorldRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
// Draws a compact frame-time sparkline graph in the F3 overlay when enabled.
@Mixin(DebugHud.class)
public class DebugHudMixin {
    // ── Cached sparkline data ──
    @Unique private static NativeBridge.FrameHistorySnapshot cachedSnapshot;
    @Unique private static String cachedStatsText = "0.0ms avg | 0.0 min | 0.0 max";
    @Unique private static long lastHistoryUpdateMs;
    @Unique private static final long HISTORY_UPDATE_INTERVAL_MS = 100; // 10 Hz refresh
    @Inject(method = "render", at = @At("TAIL"), require = 0)
    private void onRenderTail(DrawContext context, CallbackInfo ci) {
        if (!NativeBridge.isReady()) return;
        // Only render overlays when actually in a world.
        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc.world == null || mc.player == null) return;
        if (!RustMC.CONFIG.isEnableDebugHudGraph() && !RustMC.CONFIG.isEnablePieChart()) return;
        if (RustMC.CONFIG.isEnableDebugHudGraph() && RustMC.CONFIG.isUseNativeF3() && !ModBridge.isHudOwned()) {
            drawSparkline(context, mc);
        }
        if (RustMC.CONFIG.isEnablePieChart() && mc.textRenderer != null) {
            int screenW = context.getScaledWindowWidth();
            PieChartRenderer.draw(context, mc.textRenderer, screenW);
        }
    }

    @Unique
    private static void refreshHistoryCache() {
        long now = System.currentTimeMillis();
        if (cachedSnapshot != null && now - lastHistoryUpdateMs < HISTORY_UPDATE_INTERVAL_MS) return;
        NativeBridge.FrameHistorySnapshot snapshot = NativeBridge.getFrameHistorySnapshot();
        float[] history = snapshot.history();
        if (history == null || history.length == 0) {
            cachedSnapshot = null;
            cachedStatsText = "0.0ms avg | 0.0 min | 0.0 max";
            return;
        }
        cachedSnapshot = snapshot;
        cachedStatsText = "%.1fms avg | %.1f min | %.1f max".formatted(snapshot.avgMs(), snapshot.minMs(), snapshot.maxMs());
        lastHistoryUpdateMs = now;
    }
    @Unique
    private static void drawSparkline(DrawContext context, MinecraftClient mc) {
        refreshHistoryCache();
        if (cachedSnapshot == null) return;
        float[] history = cachedSnapshot.history();
        int graphW = Math.min(history.length, 240);
        int graphH = 40;
        int x0 = 2;
        int y0 = 2;
        // Background
        context.fill(x0 - 1, y0 - 1, x0 + graphW + 1, y0 + graphH + 1, 0x80000000);
        // 16.67ms target line (60 FPS)
        int targetY = y0 + graphH - (int) ((16.67f / 50.0f) * graphH);
        context.fill(x0, targetY, x0 + graphW, targetY + 1, 0x4000FF00);
        // 33.33ms target line (30 FPS)
        int target30Y = y0 + graphH - (int) ((33.33f / 50.0f) * graphH);
        context.fill(x0, target30Y, x0 + graphW, target30Y + 1, 0x40FFFF00);
        // Draw bars
        for (int i = 0; i < graphW; i++) {
            float ms = history[i];
            int barH = Math.clamp((int) ((ms / 50.0f) * graphH), 1, graphH);
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
            context.drawTextWithShadow(mc.textRenderer, cachedStatsText, x0, statsY, 0xFFAAAAAA);
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

@Mixin(WorldRenderer.class)
class RenderBudgetMixin {
    @Unique private static long lastBudgetCheckNs = 0;
    @Unique private static final long BUDGET_CHECK_INTERVAL_NS = 250_000_000L; // 4 Hz

    @SuppressWarnings("java:S2696") // @Inject methods can't be static in Mixin
    @Inject(method = "render", at = @At("HEAD"), require = 0)
    private void adjustRenderBudget(CallbackInfo ci) {
        long now = System.nanoTime();
        if (now - lastBudgetCheckNs < BUDGET_CHECK_INTERVAL_NS) return;
        lastBudgetCheckNs = now;
        updateBudget();
    }

    @Unique
    private static void updateBudget() {
        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc.world == null) {
            RenderState.renderBudgetTier = 0;
            return;
        }
        float rustAvg = NativeBridge.invokeGetAvgFps();
        int fps = rustAvg > 0 ? (int) rustAvg : mc.getCurrentFps();
        int newTier;
        if (fps < 60) {
            newTier = 1;
        } else if (fps > 90) {
            newTier = 2;
        } else {
            newTier = 0;
        }
        if (RenderState.renderBudgetTier != newTier) {
            RenderState.renderBudgetTier = newTier;
        }
    }
}