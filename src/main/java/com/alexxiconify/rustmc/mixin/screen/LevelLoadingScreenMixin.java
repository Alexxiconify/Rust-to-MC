package com.alexxiconify.rustmc.mixin.screen;

import com.alexxiconify.rustmc.RustMC;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.world.LevelLoadingScreen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(LevelLoadingScreen.class)
public abstract class LevelLoadingScreenMixin {

    @Inject(at = @At("TAIL"), method = "render")
    public void render(DrawContext context, int mouseX, int mouseY, float delta, CallbackInfo ci) {
        if (!RustMC.CONFIG.isUseFastLoadingScreen()) return;

        MinecraftClient client = MinecraftClient.getInstance();
        if (client.textRenderer == null) return;

        int w = context.getScaledWindowWidth();
        int h = context.getScaledWindowHeight();

        long used = Runtime.getRuntime().totalMemory() - Runtime.getRuntime().freeMemory();
        long max  = Runtime.getRuntime().maxMemory();
        float ratio = (float) used / max;

        int barW = Math.min(400, w - 20);
        int barH = 5;
        int bx   = (w - barW) / 2;
        int by   = h - 22;

        context.fill(bx, by, bx + barW, by + barH, RustMC.CONFIG.getLoadingBarBgColor());
        context.fill(bx, by, bx + (int)(barW * ratio), by + barH, ramColor(ratio));

        String ramText = String.format("RAM %dMB / %dMB (%.0f%%)", used >> 20, max >> 20, ratio * 100f);
        context.drawCenteredTextWithShadow(client.textRenderer, ramText, w / 2, by + barH + 2, RustMC.CONFIG.getLoadingBarTextColor());
    }

    private int ramColor(float r) {
        if (r < 0.6f) return RustMC.CONFIG.getLoadingBarLowColor();
        if (r < 0.8f) return RustMC.CONFIG.getLoadingBarMidColor();
        return RustMC.CONFIG.getLoadingBarHighColor();
    }
}
