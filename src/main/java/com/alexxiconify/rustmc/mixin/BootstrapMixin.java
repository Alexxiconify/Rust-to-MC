package com.alexxiconify.rustmc.mixin;

import net.minecraft.Bootstrap;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Pre-warms Bootstrap.initialize() on a virtual thread so the 4-5 s
 * Datafixer registry build overlaps with mixin loading rather than
 * blocking the main thread at game start.
 *
 * Bootstrap.initialize() is idempotent (guards with a static boolean),
 * so if our virtual thread finishes first the main-thread call is instant.
 */
@Mixin(Bootstrap.class)
public class BootstrapMixin {

    private BootstrapMixin() {}

    private static final AtomicBoolean prewarmStarted = new AtomicBoolean(false);

    /** Fire the pre-warm at the very first call site we can reach. */
    @Inject(method = "initialize", at = @At("HEAD"))
    private static void prewarm(CallbackInfo ci) {
        if (prewarmStarted.compareAndSet(false, true)) {
            Thread.ofVirtual().name("rustmc-bootstrap-prewarm").start(Bootstrap::initialize);
        }
    }
}
