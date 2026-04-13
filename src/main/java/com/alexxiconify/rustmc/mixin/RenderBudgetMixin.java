package com.alexxiconify.rustmc.mixin;
import com.alexxiconify.rustmc.NativeBridge;
import com.alexxiconify.rustmc.util.RenderState;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.WorldRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
//
 //  Frame-level render optimizations applied at the WorldRenderer render entry point.
 //  Sets render-budget flags that other mixins (particle, entity compat) read to
 //  decide how aggressively to cull or throttle.
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