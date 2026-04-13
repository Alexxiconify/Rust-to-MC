package com.alexxiconify.rustmc.mixin;
import com.alexxiconify.rustmc.NativeBridge;
import net.minecraft.client.MinecraftClient;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
@Mixin(MinecraftClient.class)
public class MinecraftClientMixin {
    @org.spongepowered.asm.mixin.Shadow public net.minecraft.client.world.ClientWorld world;
    @Unique private long lastFrameTime = System.nanoTime();
    @Inject(method = "render(Z)V", at = @At("HEAD"))
    private void onRenderHead(boolean tick, CallbackInfo ci) {
        long now = System.nanoTime();
        long delta = now - lastFrameTime;
        lastFrameTime = now;
        // Always record frame times when native is ready and in-world.
        // Ring buffer append is ~5ns native — cheaper than branch miss from config checks.
        if (delta > 0 && NativeBridge.isReady() && this.world != null) {
            NativeBridge.invokeAddFrameTime(delta);
        }
    }
}