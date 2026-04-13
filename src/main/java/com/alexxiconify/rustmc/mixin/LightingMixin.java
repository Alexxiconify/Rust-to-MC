package com.alexxiconify.rustmc.mixin;
import com.alexxiconify.rustmc.NativeBridge;
import com.alexxiconify.rustmc.RustMC;
import net.minecraft.world.chunk.light.LightingProvider;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
// Routes pending light tasks through Rust's propagation pool when native lighting is active.
@SuppressWarnings ( "ALL" )
@Mixin(LightingProvider.class)
public class LightingMixin {
    @Unique
    private static final long[] PENDING_POS = new long[8192];
    @Unique
    private static final int[] PENDING_VAL = new int[8192];
    @Unique
    private static int head = 0;
    @Unique
    private static int tail = 0;
    @Unique
    private static final Object QUEUE_LOCK = new Object();
    @Unique
    private static volatile boolean rustLightThreadRunning = false;
    @Unique
    private static final java.util.concurrent.atomic.AtomicBoolean rustLightThreadStarted = new java.util.concurrent.atomic.AtomicBoolean(false);
    @Unique
    private static final int[] flatBuffer = new int[8192 / 4];
    // Drains the queue and dispatches packed xyz/value entries to Rust.
    @Unique
    private static boolean drainAndDispatch() {
        int idx = 0;
        synchronized (QUEUE_LOCK) {
            while (head < tail && idx + 4 <= flatBuffer.length) {
                int qIdx = head % 8192;
                long pos = PENDING_POS[qIdx];
                int val = PENDING_VAL[qIdx];
                flatBuffer[idx++] = (int) (pos >> 38);       // X
                flatBuffer[idx++] = (int) (pos << 52 >> 52); // Y
                flatBuffer[idx++] = (int) (pos << 26 >> 38); // Z
                flatBuffer[idx++] = val;                     // Value
                head++;
            }
        }
        if (idx > 0) {
            NativeBridge.propagateLightBulk(flatBuffer, idx);
            return true;
        }
        return false;
    }
    @Unique
    private static boolean isRustLightingActive() {
        // Client-only: process received lighting data from server
        return NativeBridge.isReady() && RustMC.CONFIG.isUseNativeLighting();
    }
    @Unique
    private static void ensureRustThread() {
        if (!rustLightThreadStarted.compareAndSet(false, true)) return;
        rustLightThreadRunning = true;
        Thread.ofPlatform().name("rustmc-light-propagation").daemon(true).start(() -> {
            try {
                while (rustLightThreadRunning) {
                    try {
                        if (isRustLightingActive()) {
                            if (!drainAndDispatch()) {
                                sleepQuietly(1L);
                            }
                        } else {
                            sleepQuietly(8L);
                        }
                    } catch (Exception e) {
                        RustMC.LOGGER.warn("[Rust-MC] Lighting worker stopped after error: {}", e.getMessage());
                        rustLightThreadRunning = false;
                    }
                }
            } finally {
                rustLightThreadRunning = false;
                rustLightThreadStarted.set(false);
            }
        });
    }

    @Unique
    private static void sleepQuietly(long millis) {
        try {
            Thread.sleep(millis);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            rustLightThreadRunning = false;
        }
    }
    @Inject(method = "hasUpdates()Z", at = @At("HEAD"))
    private void onHasUpdates(CallbackInfoReturnable<Boolean> cir) {
        if (!isRustLightingActive()) return;
        ensureRustThread();
    }
}