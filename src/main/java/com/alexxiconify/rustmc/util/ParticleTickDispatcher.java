package com.alexxiconify.rustmc.util;

import com.alexxiconify.rustmc.NativeBridge;
import com.alexxiconify.rustmc.RustMC;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

// Adaptive particle tick dispatcher: prefer native path, switch to Java multicore fallback after repeated slow native batches.
public final class ParticleTickDispatcher {
    private static final AtomicBoolean preferJavaFallback = new AtomicBoolean(false);
    private static final AtomicBoolean loggedFallback = new AtomicBoolean(false);
    private static final AtomicInteger slowNativeStreak = new AtomicInteger(0);
    private static final int PARALLEL_THRESHOLD = 1024;
    private static final long NATIVE_SLOW_NS = 2_500_000L;
    private static final int SLOW_STREAK_LIMIT = 3;
    // Reusable CompletableFuture array (per-thread static to avoid per-tick allocation)
    @SuppressWarnings({"java:S5164", "unchecked"})
    private static final ThreadLocal<CompletableFuture<?>[]> FUTURES_POOL =
        (ThreadLocal<CompletableFuture<?>[]>) ThreadLocal.withInitial( () -> new CompletableFuture[Runtime.getRuntime().availableProcessors()]);

    private ParticleTickDispatcher() {}

    public static void tick(double[] positions, double[] velocities, double gravity) {
        int count = getCount(positions, velocities);
        if (count <= 0) return;

        // Early exit for native fallback state check (single volatile read)
        if (!NativeBridge.isReady() || preferJavaFallback.get()) {
            tickJavaParallel(positions, velocities, count, gravity);
            return;
        }

        double camX = 0;
        double camY = 0;
        double camZ = 0;
        double maxDistSq = 1e18;
        if (RustMC.CONFIG.isEnableParticleCulling()) {
            var mc = net.minecraft.client.MinecraftClient.getInstance();
            if (mc.gameRenderer != null && mc.gameRenderer.getCamera() != null) {
                var camPos = mc.gameRenderer.getCamera().getCameraPos();
                camX = camPos.x;
                camY = camPos.y;
                camZ = camPos.z;
                maxDistSq = (double) RustMC.CONFIG.getParticleCullingDistance() * RustMC.CONFIG.getParticleCullingDistance();
            }
        }

        long startNs = count >= PARALLEL_THRESHOLD ? System.nanoTime() : 0L;
        if (invokeNative(positions, velocities, gravity, camX, camY, camZ, maxDistSq)) {
            trackNativeTiming(startNs);
            return;
        }
        preferJavaFallback.set(true);
        tickJavaParallel(positions, velocities, count, gravity);
    }

    private static int getCount(double[] positions, double[] velocities) {
        if (positions == null || velocities == null || positions.length == 0 || velocities.length == 0) {
            return 0;
        }
        return Math.min(positions.length, velocities.length) / 3;
    }


    private static boolean invokeNative(double[] positions, double[] velocities, double gravity, double camX, double camY, double camZ, double maxDistSq) {
        try {
            NativeBridge.tickParticlesNative(positions, velocities, gravity, camX, camY, camZ, maxDistSq);
            return true;
        } catch (UnsatisfiedLinkError ignored) {
            return false;
        }
    }

    private static void trackNativeTiming(long startNs) {
        if (startNs == 0L) {
            return;
        }
        long elapsed = System.nanoTime() - startNs;
        if (elapsed <= NATIVE_SLOW_NS) {
            slowNativeStreak.set(0);
            return;
        }
        int streak = slowNativeStreak.incrementAndGet();
        if (streak >= SLOW_STREAK_LIMIT
            && preferJavaFallback.compareAndSet(false, true)
            && loggedFallback.compareAndSet(false, true)) {
            RustMC.LOGGER.info("[Rust-MC] Particle tick switched to Java parallel fallback after repeated native slow calls.");
        }
    }

    private static void tickJavaParallel(double[] positions, double[] velocities, int count, double gravity) {
        if (count >= PARALLEL_THRESHOLD && Runtime.getRuntime().availableProcessors() > 2) {
            int cores = Runtime.getRuntime().availableProcessors();
            int chunkSize = (count + cores - 1) / cores;

            java.util.concurrent.CompletableFuture<?>[] futures = FUTURES_POOL.get();
            for (int c = 0; c < cores; c++) {
                final int start = c * chunkSize;
                final int end = Math.min(start + chunkSize, count);
                futures[c] = java.util.concurrent.CompletableFuture.runAsync(() -> {
                    for (int i = start; i < end; i++) {
                        tickSingle(positions, velocities, i, gravity);
                    }
                });
            }
            java.util.concurrent.CompletableFuture.allOf(java.util.Arrays.copyOfRange(futures, 0, cores)).join();
            return;
        }

        for (int i = 0; i < count; i++) {
            tickSingle(positions, velocities, i, gravity);
        }
    }

    private static void tickSingle(double[] positions, double[] velocities, int i, double gravity) {
        int base = i * 3;
        velocities[base + 1] -= gravity;
        positions[base] += velocities[base];
        positions[base + 1] += velocities[base + 1];
        positions[base + 2] += velocities[base + 2];
    }
}