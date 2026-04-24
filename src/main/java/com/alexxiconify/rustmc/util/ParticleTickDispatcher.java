package com.alexxiconify.rustmc.util;

import com.alexxiconify.rustmc.NativeBridge;
import com.alexxiconify.rustmc.RustMC;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.IntStream;

// Adaptive particle tick dispatcher: prefer native path, switch to Java multicore fallback after repeated slow native batches.
public final class ParticleTickDispatcher {
    private static final AtomicBoolean preferJavaFallback = new AtomicBoolean(false);
    private static final AtomicBoolean loggedFallback = new AtomicBoolean(false);
    private static final AtomicInteger slowNativeStreak = new AtomicInteger(0);
    private static final int PARALLEL_THRESHOLD = 1024;
    private static final long NATIVE_SLOW_NS = 2_500_000L;
    private static final int SLOW_STREAK_LIMIT = 3;

    private ParticleTickDispatcher() {}

    public static void tick(double[] positions, double[] velocities, double gravity) {
        int count = getCount(positions, velocities);
        if (count <= 0) return;
        if (shouldUseJavaFallback()) {
            tickJavaParallel(positions, velocities, count, gravity);
            return;
        }
        long startNs = count >= PARALLEL_THRESHOLD ? System.nanoTime() : 0L;
        if (invokeNative(positions, velocities, gravity)) {
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

    private static boolean shouldUseJavaFallback() {
        return !NativeBridge.isReady() || preferJavaFallback.get();
    }

    private static boolean invokeNative(double[] positions, double[] velocities, double gravity) {
        try {
            NativeBridge.tickParticlesNative(positions, velocities, gravity);
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
            IntStream.range(0, count).parallel().forEach(i -> {
                int base = i * 3;
                velocities[base + 1] -= gravity;
                positions[base] += velocities[base];
                positions[base + 1] += velocities[base + 1];
                positions[base + 2] += velocities[base + 2];
            });
            return;
        }
        for (int i = 0; i < count; i++) {
            int base = i * 3;
            velocities[base + 1] -= gravity;
            positions[base] += velocities[base];
            positions[base + 1] += velocities[base + 1];
            positions[base + 2] += velocities[base + 2];
        }
    }
}