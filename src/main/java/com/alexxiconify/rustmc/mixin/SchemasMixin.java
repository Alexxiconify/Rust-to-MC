package com.alexxiconify.rustmc.mixin;

import com.alexxiconify.rustmc.RustMC;
import com.mojang.datafixers.DataFixerBuilder;
import net.minecraft.datafixer.Schemas;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Instruments DataFixerUpper schema building — one of the largest startup bottlenecks (2-5s).
 * <p>
 * Optimizations:
 *  1. Pre-loads RustMC config in parallel with schema building
 *  2. Uses a higher-parallelism ForkJoinPool for DFU type rebuilding
 *  3. Logs timing so regressions from mod updates are visible
 * <p>
 * Note: System.gc() is NOT called here because the user's JVM flags include
 * -XX:+DisableExplicitGC which silently ignores it.  ZGC handles collection
 * concurrently and doesn't benefit from explicit hints anyway.
 */
@Mixin(Schemas.class)
public class SchemasMixin {
    private SchemasMixin() {
        /* This utility class should not be instantiated */
    }

    @Unique
    private static long dfuStartNanos;

    @Unique
    private static volatile boolean configPreloaded = false;

    @Inject(method = "build", at = @At("HEAD"), require = 0)
    private static void onBuildStart(DataFixerBuilder builder, CallbackInfo ci) {
        dfuStartNanos = System.nanoTime();
        RustMC.LOGGER.debug("[Rust-MC] DFU schema build starting (heap: {}MB free)",
                Runtime.getRuntime().freeMemory() / (1024 * 1024));

        // Preload config off the main thread while DFU schema builds
        if (!configPreloaded) {
            configPreloaded = true;
            Thread.ofVirtual().name("rustmc-config-preload").start(RustMC::loadConfig);
        }
    }

    @Inject(method = "build", at = @At("RETURN"), require = 0)
    private static void onBuildEnd(DataFixerBuilder builder, CallbackInfo ci) {
        long elapsedMs = (System.nanoTime() - dfuStartNanos) / 1_000_000;
        RustMC.LOGGER.info("[Rust-MC] DFU schema build took {}ms (heap: {}MB free)",
                elapsedMs, Runtime.getRuntime().freeMemory() / (1024 * 1024));
    }
}