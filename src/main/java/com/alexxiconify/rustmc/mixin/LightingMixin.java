package com.alexxiconify.rustmc.mixin;

import com.alexxiconify.rustmc.ModBridge;
import com.alexxiconify.rustmc.NativeBridge;
import com.alexxiconify.rustmc.RustMC;
import net.minecraft.world.chunk.light.LightingProvider;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.TimeUnit;

/**
 * Routes pending light tasks through Rust's parallel propagation thread pool when:
 *  - useNativeLighting is enabled in config
 *  - No other mod owns lighting (ScalableLux / Starlight / C2ME / FerriteCore)
 *  - NativeBridge is ready
 */
@Mixin(LightingProvider.class)
public class LightingMixin {

    @Unique
    private static final BlockingQueue<int[]> PENDING = new ArrayBlockingQueue<>(4096);
    @Unique
    private static volatile boolean rustLightThreadRunning = false;

    // Single reusable buffer — only accessed from the single virtual thread.
    // Holds up to 4096 entries × 4 ints each = 16384 ints.
    @Unique
    private static final int[] flatBuffer = new int[4096 * 4];

    @Unique
    private static void flushToRust() {
        if (PENDING.isEmpty()) return;

        int idx = 0;
        int[] entry;
        while ((entry = PENDING.poll()) != null && idx + 4 <= flatBuffer.length) {
            System.arraycopy(entry, 0, flatBuffer, idx, 4);
            idx += 4;
        }
        if (idx > 0) {
            NativeBridge.propagateLightBulk(flatBuffer, idx / 4);
        }
    }

    @Unique
    private static synchronized void ensureRustThread() {
        if (rustLightThreadRunning) return;
        rustLightThreadRunning = true;
        Thread.ofVirtual().name("rustmc-light-propagation").start(() -> {
            try {
                while (!Thread.currentThread().isInterrupted()) {
                    // Block on the queue — properly parks without busy-waiting
                    int[] item = PENDING.poll(50, TimeUnit.MILLISECONDS);
                    if (item != null && !PENDING.offer(item)) {
                            // Queue full — process just this item directly
                            System.arraycopy(item, 0, flatBuffer, 0, 4);
                            NativeBridge.propagateLightBulk(flatBuffer, 1);
                        }

                    if (NativeBridge.isReady() && !ModBridge.isLightingOwned()
                            && RustMC.CONFIG.isUseNativeLighting()) {
                        flushToRust();
                    }
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            } finally {
                rustLightThreadRunning = false;
            }
        });
    }

    @Inject(method = "hasUpdates()Z", at = @At("HEAD"))
    private void onHasUpdates(CallbackInfoReturnable<Boolean> cir) {
        if (!RustMC.CONFIG.isUseNativeLighting() || ModBridge.isLightingOwned() || !NativeBridge.isReady()) return;
        ensureRustThread();
    }
}