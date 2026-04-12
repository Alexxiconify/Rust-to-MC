package com.alexxiconify.rustmc.mixin;

import com.alexxiconify.rustmc.ModBridge;
import com.alexxiconify.rustmc.NativeBridge;
import com.alexxiconify.rustmc.RustMC;
import net.minecraft.world.chunk.light.ChunkBlockLightProvider;
import net.minecraft.world.chunk.light.ChunkSkyLightProvider;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.concurrent.atomic.AtomicInteger;

// Routes pending light tasks through Rust's parallel propagation thread pool when native lighting is enabled and no other mod owns lighting.
@Mixin({ChunkBlockLightProvider.class, ChunkSkyLightProvider.class})
public abstract class LightingMixin {

    @Unique private static final int QUEUE_CAP = 8192;
    @Unique private static final int QUEUE_MASK = QUEUE_CAP - 1;
    @Unique private static final long[] PENDING_POS = new long[QUEUE_CAP];
    @Unique private static final int[] PENDING_VAL = new int[QUEUE_CAP];
    @Unique private static final AtomicInteger HEAD = new AtomicInteger(0);
    @Unique private static final AtomicInteger TAIL = new AtomicInteger(0);
    @Unique private static volatile boolean rustLightThreadRunning = false;
    @Unique private static final int[] flatBuffer = new int[QUEUE_CAP * 4];
    // Drains the ring buffer and dispatches to Rust — called only from the background virtual thread.
    @Unique
    private static void drainAndDispatch() {
        int idx = 0;
        int h = HEAD.get();
        int t = TAIL.get();
        while (h != t && idx + 4 <= flatBuffer.length) {
            int qIdx = h & QUEUE_MASK;
            long pos = PENDING_POS[qIdx];
            int val = PENDING_VAL[qIdx];
            flatBuffer[idx++] = (int) (pos >> 38);        // X
            flatBuffer[idx++] = (int) (pos << 52 >> 52);  // Y
            flatBuffer[idx++] = (int) (pos << 26 >> 38);  // Z
            flatBuffer[idx++] = val;                       // Value
            h++;
        }
        HEAD.set(h);
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
                    if (isRustLightingActive()) drainAndDispatch();
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            } finally {
                rustLightThreadRunning = false;
            }
        });
    }

    // hasUpdates is a safe hook point for starting the background virtual thread.
    @Inject(method = "hasUpdates()Z", at = @At("HEAD"), require = 0)
    private void onHasUpdates(CallbackInfoReturnable<Boolean> cir) {
        if (!isRustLightingActive()) return;
        ensureRustThread();
    }

    // 1.21.11 renamed checkBlock(BlockPos) to checkForLightUpdate(long packedBlockPos).
    @Inject(method = "checkForLightUpdate(J)V", at = @At("HEAD"), require = 0)
    private static void rustmcOnEnqueue(long packedPos, CallbackInfo ci) {
        if (!isRustLightingActive()) return;
        int t = TAIL.get();
        int h = HEAD.get();
        // Drop silently if full rather than blocking.
        if (t - h >= QUEUE_CAP) return;
        int qIdx = t & QUEUE_MASK;
        PENDING_POS[qIdx] = packedPos;
        PENDING_VAL[qIdx] = 15; // LightType removed in 1.21.11; use max value
        TAIL.set(t + 1);
    }
}