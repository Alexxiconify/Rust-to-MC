package com.alexxiconify.rustmc.mixin.compat;

import com.alexxiconify.rustmc.RustMC;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Pseudo
@Mixin(targets = "xaero.common.minimap.MinimapInterface")
public class XaeroMinimapLimiterMixin {

    private long lastDrawTime = 0;

    @SuppressWarnings("all")
    @Inject(method = "drawMinimap", at = @At("HEAD"), cancellable = true, remap = false)
    private void limitMinimapFramerate(DrawContext context, MinecraftClient mc, int width, int height, int scaledWidth, int scaledHeight, int scale, CallbackInfo ci) {
        if (!RustMC.CONFIG.isLimitXaeroMinimap() || com.alexxiconify.rustmc.ModBridge.XAEROPLUS) return;

        long currentTime = System.currentTimeMillis();
        // Limit to 30 FPS update rate (approx 33ms)
        if (currentTime - lastDrawTime < 33) {
            ci.cancel();
        } else {
            lastDrawTime = currentTime;
        }
    }
}
