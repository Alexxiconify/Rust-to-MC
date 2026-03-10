package com.alexxiconify.rustmc.mixin.screen;

import com.alexxiconify.rustmc.RustMC;
import com.alexxiconify.rustmc.util.RamBarRenderer;
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

        RamBarRenderer.drawRamBar(context, client.textRenderer, w, h, RustMC.CONFIG.getLoadingBarBgColor());
    }
}