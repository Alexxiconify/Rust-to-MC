package com.alexxiconify.rustmc.mixin;

import net.minecraft.client.MinecraftClient;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(MinecraftClient.class)
public class TickLimiterMixin {

    private long lastTickTime = 0;
    private static final long SERVER_TICK_INTERVAL_MS = 50; // Vanilla 20 TPS is 50ms per tick

    // Inject before the client tries to tick everything.
    // If we're ticking too fast compared to the server, we throttle to avoid
    // ghost block placements, rubberbanding, and wasted CPU/GUI cycles.
    @Inject(method = "tick", at = @At("HEAD"), cancellable = true)
    @SuppressWarnings("resource")
    private void onTick(CallbackInfo ci) {
        MinecraftClient mc = (MinecraftClient) (Object) this;
        
        // We only limit ticking if we are connected to a world/server. Main menu ticking is fine.
        if (mc.world != null) {
            long currentTime = System.currentTimeMillis();
            
            // If less than 50ms have passed since the last tick, the client is running ahead of the server.
            // We cancel the client tick to sync up and save CPU/GUI rendering overhead.
            if (currentTime - lastTickTime < SERVER_TICK_INTERVAL_MS) {
                // By cancelling the tick early, we prevent redundant UI updates,
                // entity interpolation logic, and redundant packet processing.
                ci.cancel(); 
            } else {
                lastTickTime = currentTime;
            }
        }
    }
}
