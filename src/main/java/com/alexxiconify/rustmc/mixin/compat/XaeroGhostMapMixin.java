package com.alexxiconify.rustmc.mixin.compat;

import com.alexxiconify.rustmc.RustMC;
import com.alexxiconify.rustmc.compat.XaeroGhostMapCompat;

import com.alexxiconify.rustmc.config.RustMCConfig;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.util.Identifier;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Pseudo
@Mixin(targets = {
    "xaero.common.minimap.MinimapInterface",
    "xaero.map.gui.GuiMap"
})
public class XaeroGhostMapMixin {

    @SuppressWarnings("java:S107")
    @Inject(method = "drawMinimap", at = @At("HEAD"), remap = false, require = 0)
    private void drawGhostMapUnderlayMinimap(
            DrawContext context,
            MinecraftClient mc,
            int width,
            int height,
            int scaledWidth,
            int scaledHeight,
            int scale,
            CallbackInfo ci
    ) {
        drawGhostMapCommon(context, width, height);
    }

    @Inject(method = "render", at = @At("HEAD"), remap = false, require = 0)
    private void drawGhostMapUnderlayWorldMap(
            DrawContext context,
            int mouseX,
            int mouseY,
            float delta,
            CallbackInfo ci
    ) {
        // World Map usually fills screen, use screen bounds or map bound attributes
        MinecraftClient mc = MinecraftClient.getInstance();
        net.minecraft.client.gui.screen.Screen screen = mc.currentScreen;
        if (screen != null) {
            drawGhostMapCommon(context, screen.width, screen.height);
        }
    }

    private void drawGhostMapCommon(DrawContext context, int width, int height) {

        if (RustMC.CONFIG.getGhostMapMode() == RustMCConfig.GhostMapMode.NONE) {
            return;
        }

        Identifier tex = XaeroGhostMapCompat.getGhostTexture();
        if (tex == null) {
            return;
        }

        // Temporary debug
        RustMC.LOGGER.info("[Rust-MC] Xaero Map Draw Triggered!");

        // Draw context bounds on screen.
        context.drawTexturedQuad(
                tex,
                0, 0,
                0, 0,
                width, height,
                width, height
        );
    }
}