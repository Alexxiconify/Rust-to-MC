package com.alexxiconify.rustmc.mixin;
import net.minecraft.Bootstrap;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import java.util.concurrent.atomic.AtomicBoolean;
 //  Pre-warms Bootstrap.initialize() on a platform daemon thread so the 4-5 s
 //  Datafixer registry build overlaps with mixin loading rather than
 //  blocking the main thread at game start.
 //  Bootstrap.initialize() is idempotent (guards with a static boolean),
 //  so if our platform thread finishes first the main-thread call is instant.
@Mixin(Bootstrap.class)
public class BootstrapMixin {
    private BootstrapMixin() {}

    @Unique
    private static final AtomicBoolean prewarmStarted = new AtomicBoolean( false);
    // Fire pre-warm at first call site; CAS prevents duplicate prewarm thread and Bootstrap itself is idempotent.
    @Inject(method = "initialize", at = @At("HEAD"))
    private static void prewarm(CallbackInfo ci) {
        if (prewarmStarted.compareAndSet(false, true)) {
            Thread.ofPlatform().name("rustmc-bootstrap-prewarm").daemon(true).start(Bootstrap::initialize);
        }
    }
}