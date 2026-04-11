package com.alexxiconify.rustmc.mixin;

import com.alexxiconify.rustmc.ModBridge;
import com.alexxiconify.rustmc.NativeBridge;
import com.alexxiconify.rustmc.RustMC;
import net.minecraft.world.chunk.light.ChunkLightProvider;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

// Routes pending light tasks through Rust's parallel propagation thread pool when native lighting is enabled and no other mod owns lighting.
@Mixin(ChunkLightProvider.class)
public abstract class LightingMixin {

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
    private static final int[] flatBuffer = new int[8192 * 4];

    // Drains the queue and dispatches to Rust.
    @Unique
    private static void drainAndDispatch() {
        int idx = 0;
        synchronized (QUEUE_LOCK) {
            while (head < tail && idx + 4 <= flatBuffer.length) {
                int qIdx = head & 8191;
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
            NativeBridge.propagateLightBulk(flatBuffer, idx / 4);
        }
    }

    @Unique
    private static boolean isRustLightingActive() {
        return NativeBridge.isReady() && !ModBridge.isLightingConflict() && RustMC.CONFIG.isUseNativeLighting();
    }

    @Unique
    @SuppressWarnings("all")
    private static synchronized void ensureRustThread() {
        if (rustLightThreadRunning) return;
        rustLightThreadRunning = true;
        Thread.ofVirtual().name("rustmc-light-propagation").start(() -> {
            try {
                while (!Thread.currentThread().isInterrupted()) {
                    Thread.sleep(10);
                    if (isRustLightingActive()) {
                        drainAndDispatch();
                    }
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            } finally {
                rustLightThreadRunning = false;
            }
        });
    }

    // hasUpdates still exists in 1.21.11 — safe hook point for starting the background thread
    @Inject(method = "hasUpdates()Z", at = @At("HEAD"), require = 0)
    private void onHasUpdates(CallbackInfoReturnable<Boolean> cir) {
        if (!isRustLightingActive()) return;
        ensureRustThread();
    }

    // 1.21.11 renamed checkBlock(BlockPos) to checkForLightUpdate(long packedBlockPos)
    @Inject(method = "checkForLightUpdate(J)V", at = @At("HEAD"), require = 0)
    private void rustmcOnEnqueue(long packedPos, CallbackInfo ci) {
        if (!isRustLightingActive()) return;
        // LightType field was removed in 1.21.11; use default value 15
        int val = 15;
        synchronized (QUEUE_LOCK) {
            if (tail - head < 8192) {
                int qIdx = tail & 8191;
                PENDING_POS[qIdx] = packedPos;
                PENDING_VAL[qIdx] = val;
                tail++;
            }
        }
    }
}