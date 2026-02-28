package com.alexxiconify.rustmc.mixin.screen;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.SplashOverlay;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import com.alexxiconify.rustmc.RustMC;

@Mixin(SplashOverlay.class)
public abstract class SplashOverlayMixin {

    @Shadow
    @Final
    private MinecraftClient client;

    @Shadow
    private long reloadCompleteTime; // Yarn mapping for fadeOutStart / clear time

    @Inject(at = @At("TAIL"), method = "render")
    public void render(DrawContext context, int mouseX, int mouseY, float delta, CallbackInfo ci) {
        if (RustMC.CONFIG.isUseFastLoadingScreen()) {
            if (this.reloadCompleteTime != -1L) {
                this.client.setOverlay(null);
            } else {
                if (this.client.textRenderer == null) return;
                int modsCount = net.fabricmc.loader.api.FabricLoader.getInstance().getAllMods().size();
                String text = "Rust-MC: Fast Loading - " + modsCount + " Mods Loaded";
                int textWidth = this.client.textRenderer.getWidth(text);
                int screenWidth = context.getScaledWindowWidth();
                int screenHeight = context.getScaledWindowHeight();
                context.drawTextWithShadow(this.client.textRenderer, text, screenWidth - textWidth - 5, screenHeight - 15, 0xFFFFFF);
            }
        }
    }

    @Inject(at = @At("RETURN"), method = "isReadyToFadeOut", cancellable = true)
    private void isReadyToFadeOut(CallbackInfoReturnable<Boolean> cir) {
        if (RustMC.CONFIG.isUseFastLoadingScreen()) {
            cir.setReturnValue(true);
        }
    }
}
