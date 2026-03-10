package com.alexxiconify.rustmc.mixin.screen;

import com.alexxiconify.rustmc.RustMC;
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

        long used = Runtime.getRuntime().totalMemory() - Runtime.getRuntime().freeMemory();
        long max  = Runtime.getRuntime().maxMemory();
        float ratio = (float) used / max;

        int barW = Math.min(400, w - 20);
        int barH = 5;
        int bx   = (w - barW) / 2;
        int by   = h - 22;

        // Track + fill
        context.fill(bx, by, bx + barW, by + barH, 0xFF1A1A1A);
        context.fill(bx, by, bx + (int)(barW * ratio), by + barH, ramColor(ratio));

        // Labels — tight below/above bar, no empty gap
        String ramText = String.format("RAM %dMB / %dMB (%.0f%%)", used >> 20, max >> 20, ratio * 100f);
        context.drawCenteredTextWithShadow(this.client.textRenderer, ramText, w / 2, by + barH + 2,
            RustMC.CONFIG.getLoadingBarTextColor());

        int modCount = net.fabricmc.loader.api.FabricLoader.getInstance().getAllMods().size();
        context.drawCenteredTextWithShadow(this.client.textRenderer,
            "Rust-MC  \u2022  " + modCount + " mods", w / 2, by - 9,
            RustMC.CONFIG.getLoadingBarSubtextColor());
    }

    private int ramColor(float r) {
     if (r < 0.6f) return RustMC.CONFIG.getLoadingBarLowColor();
        if (r < 0.8f) return RustMC.CONFIG.getLoadingBarMidColor();
        return RustMC.CONFIG.getLoadingBarHighColor();
    }
}