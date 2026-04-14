package com.alexxiconify.rustmc.mixin.client;
import com.alexxiconify.rustmc.NativeBridge;
import net.minecraft.client.MinecraftClient;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(MinecraftClient.class)
public class ClientFrameMetricsMixin {
    @org.spongepowered.asm.mixin.Shadow public net.minecraft.client.world.ClientWorld world;
    @Unique private long lastFrameTimeNanos = System.nanoTime();

    @Inject(method = "render(Z)V", at = @At("HEAD"))
    private void onRenderHead(boolean tick, CallbackInfo ci) {
        NativeBridge.rollFrustumFrameCounters();
        long now = System.nanoTime();
        long delta = now - lastFrameTimeNanos;
        lastFrameTimeNanos = now;
        if (delta > 0 && this.world != null && NativeBridge.isReady()) {
            NativeBridge.invokeAddFrameTime(delta);
        }
    }
}