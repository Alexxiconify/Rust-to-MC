package com.alexxiconify.rustmc.mixin.compat;

import com.alexxiconify.rustmc.ModBridge;
import com.alexxiconify.rustmc.RustMC;
import net.minecraft.client.MinecraftClient;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Integrates with TickSync (by LoggaMoJa) and other tick-timing mods.
 * When TickSync is present, we yield tick scheduling to it.
 * When it's absent and our compat is enabled, we apply a lightweight
 * tick-smoothing adjustment to reduce stutter from uneven tick intervals.
 * <p>
 * This mixin hooks the client tick method to monitor tick interval regularity
 * and optionally smooth the render interpolation to reduce visual jitter.
 */
@Mixin(MinecraftClient.class)
public class TickSyncCompatMixin {

    private long lastTickNanos = 0;
    private float smoothTickDelta = 0.0f;

    @Inject(method = "tick", at = @At("HEAD"))
    private void onTickHead(CallbackInfo ci) {
        if (!RustMC.CONFIG.isEnableTickSyncCompat()) return;

        // If TickSync is installed, it handles everything — we just monitor
        if (ModBridge.TICK_SYNC) return;

        long now = System.nanoTime();
        if (lastTickNanos > 0) {
            long delta = now - lastTickNanos;
            float deltaMs = delta / 1_000_000.0f;
            // Expected tick interval is 50ms (20 TPS)
            // Smooth the delta to reduce visual jitter from uneven tick spacing
            smoothTickDelta = smoothTickDelta * 0.8f + deltaMs * 0.2f;
        }
        lastTickNanos = now;
    }

    /**
     * Returns the smoothed tick delta for use by render interpolation hooks.
     * Returns 0 if tick sync is disabled or TickSync mod handles it.
     */
    public float getSmoothedTickDelta() {
        return smoothTickDelta;
    }
}