package com.alexxiconify.rustmc.mixin;
import com.alexxiconify.rustmc.NativeBridge;
import net.minecraft.client.MinecraftClient;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import java.util.concurrent.atomic.AtomicLong;

@Mixin(MinecraftClient.class)
public class MinecraftClientMixin {
    @org.spongepowered.asm.mixin.Shadow public net.minecraft.client.world.ClientWorld world;
    @Unique private final AtomicLong lastFrameTime = new AtomicLong(System.nanoTime());

    @Inject(method = "render(Z)V", at = @At("HEAD"))
    private void onRenderHead(boolean tick, CallbackInfo ci) {
        NativeBridge.rollFrustumFrameCounters();
        long now = System.nanoTime();
        long prevTime = lastFrameTime.getAndSet(now);
        long delta = now - prevTime;
        // Lock-free frame time tracking: no contention, atomic update with getAndSet
        if (delta > 0 && this.world != null && NativeBridge.isReady()) {
            NativeBridge.invokeAddFrameTime(delta);
        }
    }
}