package com.alexxiconify.rustmc.mixin;

import com.alexxiconify.rustmc.NativeBridge;
import com.alexxiconify.rustmc.RustMC;
import net.minecraft.client.MinecraftClient;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(MinecraftClient.class)
public class MinecraftClientMixin {
    @Unique private long lastFrameTime = System.nanoTime();

    @Inject(method = "render(Z)V", at = @At("HEAD"))
    private void onRenderHead(boolean tick, CallbackInfo ci) {
        long now = System.nanoTime();
        long delta = now - lastFrameTime;
        lastFrameTime = now;
        // Only record frame times when overlays are active — avoids JNI overhead every frame
        if (delta > 0 && (RustMC.CONFIG.isEnableDebugHudGraph() || RustMC.CONFIG.isEnablePieChart())) {
            NativeBridge.invokeAddFrameTime(delta);
        }
    }
}