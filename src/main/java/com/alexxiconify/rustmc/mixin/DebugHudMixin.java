package com.alexxiconify.rustmc.mixin;

import com.alexxiconify.rustmc.ModBridge;
import com.alexxiconify.rustmc.RustMC;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.hud.DebugHud;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(DebugHud.class)
public class DebugHudMixin {

    @Inject(method = "render(Lnet/minecraft/client/gui/DrawContext;)V", at = @At("HEAD"))
    private void onRender(DrawContext context, CallbackInfo ci) {
        if (RustMC.CONFIG.isUseNativeF3() && !ModBridge.isHudOwned()) {
            // Future hardware-accelerated graph rendering
        }
    }

    private long lastCacheUpdate = 0;

    @Inject(method = "drawLeftText(Lnet/minecraft/client/gui/DrawContext;)V", at = @At("HEAD"), cancellable = true)
    private void onDrawLeftText(DrawContext context, CallbackInfo ci) {
        if (!RustMC.CONFIG.isUseNativeF3()) return;
        
        long now = System.currentTimeMillis();
        if (now - lastCacheUpdate < 20) { // 50 Hz update rate for F3 text
             // Future: Implement string building throttling
        }
    }
}
