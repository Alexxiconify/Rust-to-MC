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


    /** Drains the queue starting from the given item and dispatches to Rust. */
    @Unique
    private static void drainAndDispatch(int[] firstItem) {
        System.arraycopy(firstItem, 0, flatBuffer, 0, 4);
        int idx = 4;
        int[] next;
        while ((next = PENDING.poll()) != null && idx + 4 <= flatBuffer.length) {
            System.arraycopy(next, 0, flatBuffer, idx, 4);
            idx += 4;
        }
        NativeBridge.propagateLightBulk(flatBuffer, idx / 4);
    }

    @Unique
    private static boolean isRustLightingActive() {
        return NativeBridge.isReady() && !ModBridge.isLightingOwned()
                && RustMC.CONFIG.isUseNativeLighting();
    }

    @Unique
    private static synchronized void ensureRustThread() {
        if (rustLightThreadRunning) return;
        rustLightThreadRunning = true;
        Thread.ofVirtual().name("rustmc-light-propagation").start(() -> {
            try {
                while (!Thread.currentThread().isInterrupted()) {
                    int[] item = PENDING.poll(50, TimeUnit.MILLISECONDS);
                    if (item != null && isRustLightingActive()) {
                        drainAndDispatch(item);
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