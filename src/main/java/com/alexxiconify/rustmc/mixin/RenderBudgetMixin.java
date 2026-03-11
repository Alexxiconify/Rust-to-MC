package com.alexxiconify.rustmc.mixin;

import com.alexxiconify.rustmc.util.RenderState;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.WorldRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Frame-level render optimizations applied at the WorldRenderer render entry point.
 * Sets render-budget flags that other mixins (particle, entity compat) read to
 * decide how aggressively to cull or throttle.
 * <p>
 * When FPS drops below threshold, signals to tighten culling across the board.
 * When FPS is healthy (120+), relaxes budgets for better visual quality.
 */
@Mixin(WorldRenderer.class)
class RenderBudgetMixin {

    @Unique private static long lastBudgetCheckNs = 0;
    @Unique private static final long BUDGET_CHECK_INTERVAL_NS = 250_000_000L; // 4 Hz

    @Inject(method = "render", at = @At("HEAD"), require = 0)
    private void adjustRenderBudget(CallbackInfo ci) {
        long now = System.nanoTime();
        if (now - lastBudgetCheckNs < BUDGET_CHECK_INTERVAL_NS) return;
        lastBudgetCheckNs = now;

        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc.world == null) return;

        // Use getCurrentFps() to decide render budget tightness
        int fps = mc.getCurrentFps();

        // When FPS is low (<60), signal tight budget mode for particle and entity culling
        // When FPS is healthy (>90), relax for better visual quality
        RenderState.renderBudgetTight = fps < 60;
        RenderState.renderBudgetRelaxed = fps > 90;
    }
}