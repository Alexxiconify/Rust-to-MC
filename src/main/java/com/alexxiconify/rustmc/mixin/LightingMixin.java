package com.alexxiconify.rustmc.mixin;

import com.alexxiconify.rustmc.ModBridge;
import com.alexxiconify.rustmc.NativeBridge;
import com.alexxiconify.rustmc.RustMC;
import net.minecraft.world.LightType;
import net.minecraft.world.chunk.light.ChunkLightProvider;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

// Routes pending light tasks through Rust's parallel propagation thread pool when: - useNativeLighting is enabled in config - No other mod owns lighting (ScalableLux / Starlight / C2ME / FerriteCore) - NativeBridge is ready
@Mixin(ChunkLightProvider.class)
public abstract class LightingMixin {

    @Shadow @Final protected LightType type;

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

    @Inject(method = "hasUpdates()Z", at = @At("HEAD"))
    private void onHasUpdates(CallbackInfoReturnable<Boolean> cir) {
        if (!isRustLightingActive()) return;
        ensureRustThread();
    }

    @Inject(method = "checkBlock(Lnet/minecraft/util/math/BlockPos;)V", at = @At("HEAD"), require = 0)
    private void rustmcOnEnqueue(net.minecraft.util.math.BlockPos pos, CallbackInfo ci) {
        if (!isRustLightingActive()) return;
        // Capture both BLOCK and SKY light to supplement the lighting engine more effectively
        int val = this.type == LightType.SKY ? 16 : 15;
        synchronized (QUEUE_LOCK) {
            if (tail - head < 8192) {
                int qIdx = tail & 8191;
                PENDING_POS[qIdx] = pos.asLong();
                PENDING_VAL[qIdx] = val;
                tail++;
            }
        }
    }
}