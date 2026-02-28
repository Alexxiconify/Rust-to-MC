package com.alexxiconify.rustmc.mixin;

import com.alexxiconify.rustmc.ModBridge;
import com.alexxiconify.rustmc.NativeBridge;
import com.alexxiconify.rustmc.RustMC;
import net.minecraft.world.chunk.light.LightingProvider;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;

/**
 * Routes pending light tasks through Rust's parallel propagation thread pool when:
 *  - useNativeLighting is enabled in config
 *  - No other mod owns lighting (ScalableLux / Starlight / C2ME / FerriteCore)
 *  - NativeBridge is ready
 *
 * ScalableLux is already faster than Rust here, so we yield completely when it's present.
 * The worker drains the queue every ~4 ms; rustPropagateLightBulk handles parallelism on the Rust side.
 */
@Mixin(LightingProvider.class)
public class LightingMixin {

    // Queue of encoded (x, y, z, type) tuples for the Rust bulk propagation call.
    private static final BlockingQueue<int[]> PENDING = new ArrayBlockingQueue<>(4096);
    private static volatile boolean rustLightThreadRunning = false;

    private static void flushToRust() {
        if (PENDING.isEmpty()) return;
        int size = PENDING.size();
        int[] flat = new int[size * 4];
        int idx = 0;
        int[] entry;
        while ((entry = PENDING.poll()) != null && idx + 4 <= flat.length) {
            flat[idx++] = entry[0];
            flat[idx++] = entry[1];
            flat[idx++] = entry[2];
            flat[idx++] = entry[3];
        }
        NativeBridge.propagateLightBulk(flat, idx / 4);
    }

    private static synchronized void ensureRustThread() {
        if (rustLightThreadRunning) return;
        rustLightThreadRunning = true;
        Thread.ofVirtual().name("rustmc-light-propagation").start(() -> {
            while (true) {
                try {
                    Thread.sleep(4); // ~250 Hz drain rate
                    if (NativeBridge.isReady() && !ModBridge.isLightingOwned()
                            && RustMC.CONFIG.isUseNativeLighting()) {
                        flushToRust();
                    }
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    rustLightThreadRunning = false;
                    return;
                }
            }
        });
    }

    @Inject(method = "hasUpdates()Z", at = @At("HEAD"))
    private void onHasUpdates(CallbackInfoReturnable<Boolean> cir) {
        if (!RustMC.CONFIG.isUseNativeLighting() || ModBridge.isLightingOwned() || !NativeBridge.isReady()) return;
        ensureRustThread();
    }
}
