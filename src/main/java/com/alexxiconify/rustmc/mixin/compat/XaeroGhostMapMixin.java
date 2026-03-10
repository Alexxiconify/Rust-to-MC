package com.alexxiconify.rustmc.mixin.compat;

import com.alexxiconify.rustmc.RustMC;
import com.alexxiconify.rustmc.compat.XaeroGhostMapCompat;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gl.RenderPipelines;
import net.minecraft.util.Identifier;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Injects the ghost map overlay into Xaero's Minimap rendering.
 * Split from the world map mixin to avoid targeting two unrelated classes.
 * <p>
 * Transparency is baked into the texture pixels by XaeroGhostMapCompat
 * (alpha channel in the ARGB data), so no RenderSystem blend calls are needed.
 */
@Pseudo
@Mixin(targets = "xaero.common.minimap.MinimapInterface")
public class XaeroGhostMapMixin {

    @SuppressWarnings("java:S107")
    @Inject(method = "drawMinimap", at = @At("TAIL"), remap = false, require = 0)
    private void drawGhostMapOverlayMinimap(
            DrawContext context,
            MinecraftClient mc,
            int width,
            int height,
            int scaledWidth,
            int scaledHeight,
            int scale,
            CallbackInfo ci
    ) {
        if (!RustMC.CONFIG.isGhostMapEnabled()) return;

        Identifier tex = XaeroGhostMapCompat.getGhostTexture();
        if (tex == null) return;

        int mapSize = Math.min(scaledWidth, scaledHeight);
        int mapX = scaledWidth - mapSize;
        int mapY = 0;

        // Alpha is baked into the texture pixels — no blend state needed
        context.drawTexture(RenderPipelines.GUI_TEXTURED, tex, mapX, mapY, 0.0f, 0.0f, mapSize, mapSize, mapSize, mapSize);
    }
}