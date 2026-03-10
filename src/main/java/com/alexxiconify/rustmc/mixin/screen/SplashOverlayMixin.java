package com.alexxiconify.rustmc.mixin.screen;

import com.alexxiconify.rustmc.RustMC;
import com.alexxiconify.rustmc.util.RamBarRenderer;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.SplashOverlay;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(SplashOverlay.class)
public abstract class SplashOverlayMixin {

    @Shadow @Final private MinecraftClient client;

    // Paint dark bg BEFORE vanilla renders its white background (eliminates white flash).
    // Not cancellable — we let vanilla's logo/progress bar still render on top.
    @Inject(at = @At("HEAD"), method = "render")
    public void renderHead(DrawContext context, int mouseX, int mouseY, float delta, CallbackInfo ci) {
        if ( !RustMC.CONFIG.isUseFastLoadingScreen() ) return;
        int w = context.getScaledWindowWidth();
        int h = context.getScaledWindowHeight();
        // Fill full screen with dark color first — vanilla then draws its logo on top
        context.fill(0, 0, w, h, 0xFF0D0D0D);
    }

    @Inject(at = @At("TAIL"), method = "render")
    public void renderTail(DrawContext context, int mouseX, int mouseY, float delta, CallbackInfo ci) {
        if ( !RustMC.CONFIG.isUseFastLoadingScreen() ) return;
        if (this.client.textRenderer == null) return;

        int w = context.getScaledWindowWidth();
        int h = context.getScaledWindowHeight();

        RamBarRenderer.drawRamBar(context, this.client.textRenderer, w, h, 0xFF1A1A1A);

        int modCount = net.fabricmc.loader.api.FabricLoader.getInstance().getAllMods().size();
        int by = h - 22;
        context.drawCenteredTextWithShadow(this.client.textRenderer,
            "Rust-MC  \u2022  " + modCount + " mods", w / 2, by - 9,
            RustMC.CONFIG.getLoadingBarSubtextColor());
    }
}