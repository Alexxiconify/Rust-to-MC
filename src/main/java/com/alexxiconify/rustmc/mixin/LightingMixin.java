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
 *  - Native lighting is enabled in config
 *  - No other mod owns lighting (ScalableLux / Starlight / StarrySky / C2ME)
 *  - NativeBridge is ready
 *
 * When ScalableLux IS present, we yield completely — it's already faster than Rust here.
 * The batch queue is drained on the Rust side in parallel; results are written back
 * to the light level cache via the bulk JNI call (rustPropagateLightBulk).
 */
@Mixin(LightingProvider.class)
public class LightingMixin {

    // Pending encoded (x,y,z,type) tuples waiting to be sent to Rust in bulk.
    private static final BlockingQueue<int[]> PENDING = new ArrayBlockingQueue<>(4096);
    private static volatile boolean rustLightThreadRunning = false;

    // Encode a position + type into a single int[] tuple for the JNI call.
    private static int[] encode(int x, int y, int z, int type) {
        return new int[]{x, y, z, type};
    }

    /** Best-effort drain — submits all queued positions to rustPropagateLightBulk. */
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

    /** Starts a single background virtual thread that periodically flushes the queue. */
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
        // Guard: yield to ScalableLux / Starlight / C2ME
        if (!RustMC.CONFIG.isUseNativeLighting() || ModBridge.isLightingOwned() || !NativeBridge.isReady()) return;
        ensureRustThread();
    }
}
