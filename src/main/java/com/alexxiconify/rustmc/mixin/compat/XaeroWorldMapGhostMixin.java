package com.alexxiconify.rustmc.mixin.compat;

import com.alexxiconify.rustmc.RustMC;
import com.alexxiconify.rustmc.compat.XaeroGhostMapCompat;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.util.Identifier;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Injects the ghost map overlay into Xaero's World Map GUI rendering.
 * Separate from the minimap mixin since GuiMap is a different class hierarchy.
 */
@Pseudo
@Mixin(targets = "xaero.map.gui.GuiMap")
public class XaeroWorldMapGhostMixin {

    @Inject(method = "render", at = @At("TAIL"), remap = false, require = 0)
    private void drawGhostMapOverlayWorldMap(
            DrawContext context,
            int mouseX,
            int mouseY,
            float delta,
            CallbackInfo ci
    ) {
        if (!RustMC.CONFIG.isGhostMapEnabled()) return;

        Identifier tex = XaeroGhostMapCompat.getGhostTexture();
        if (tex == null) return;

        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc.currentScreen == null) return;

        int screenW = mc.currentScreen.width;
        int screenH = mc.currentScreen.height;

        int mapSize = Math.min(screenW, screenH) - 20;
        if (mapSize <= 0) return;

        int mapX = (screenW - mapSize) / 2;
        int mapY = (screenH - mapSize) / 2;

        context.setShaderColor(1.0f, 1.0f, 1.0f, 0.35f);
        context.drawTexture(RenderLayer::getGuiTextured, tex, mapX, mapY, 0, 0, mapSize, mapSize, mapSize, mapSize);
        context.setShaderColor(1.0f, 1.0f, 1.0f, 1.0f);
    }
}

