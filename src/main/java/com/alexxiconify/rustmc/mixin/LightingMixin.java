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
 * <p>
 * ScalableLux is already faster than Rust here, so we yield completely when it's present.
 * The worker drains the queue every ~4 ms; rustPropagateLightBulk handles parallelism on the Rust side.
 */
@Mixin(LightingProvider.class)
public class LightingMixin {

    // Queue of encoded (x, y, z, type) tuples for the Rust bulk propagation call.
    @Unique
    private static final BlockingQueue<int[]> PENDING = new ArrayBlockingQueue<>( 4096);
    @Unique
    private static volatile boolean rustLightThreadRunning = false;

    @Unique
    private static final int MAX_BUFFER_SIZE = 32768; // Cap at 32K entries (128KB)

    // flatBuffer is only accessed from the single virtual thread — but use ThreadLocal
    // to make this explicit and avoid any future accidental cross-thread access.
    @Unique
    private static final ThreadLocal<int[]> FLAT_BUFFER = ThreadLocal.withInitial( () -> new int[4096 * 4]);

    @Unique
    private static void flushToRust() {
        if (PENDING.isEmpty()) return;
        int[] flatBuffer = FLAT_BUFFER.get();

        int idx = 0;
        int[] entry;
        while ((entry = PENDING.poll()) != null && idx + 4 <= flatBuffer.length) {
            System.arraycopy(entry, 0, flatBuffer, idx, 4);
            idx += 4;
            if (idx >= MAX_BUFFER_SIZE * 4) break; // cap per flush
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
            while (!Thread.currentThread().isInterrupted()) {
                try {
                    // Block on the queue with a timeout instead of Thread.sleep —
                    // properly parks the thread without busy-waiting.
                    PENDING.poll(4, TimeUnit.MILLISECONDS);
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