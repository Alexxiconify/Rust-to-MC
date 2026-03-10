package com.alexxiconify.rustmc.mixin.compat;

import com.alexxiconify.rustmc.RustMC;
import com.alexxiconify.rustmc.compat.XaeroGhostMapCompat;
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
 * Tiles the ghost texture across the minimap viewport to fill all grid cells,
 * matching Xaero's own tiling behavior.
 */
@SuppressWarnings("all") // @Pseudo target — Xaero classes unavailable at compile time
@Pseudo
@Mixin(targets = "xaero.common.minimap.MinimapInterface")
public class XaeroGhostMapMixin {

    @SuppressWarnings("java:S107")
    @Inject(method = "drawMinimap", at = @At("TAIL"), remap = false, require = 0)
    private void drawGhostMapOverlayMinimap(
            DrawContext context,
            net.minecraft.client.MinecraftClient mc,
            int width,
            int height,
            int scaledWidth,
            int scaledHeight,
            int scale,
            CallbackInfo ci
    ) {
        if ( RustMC.CONFIG.isGhostMapEnabled ( ) ) return;

        Identifier tex = XaeroGhostMapCompat.getGhostTexture();
        if (tex == null) return;

        int mapSize = Math.min(scaledWidth, scaledHeight);
        int mapX = scaledWidth - mapSize;
        int mapY = 0;
        int texSz = 128; // ghost texture is 128×128

        // Tile the ghost texture across the minimap viewport to fill all grid cells
        for (int ty = mapY; ty < mapY + mapSize; ty += texSz) {
            for (int tx = mapX; tx < mapX + mapSize; tx += texSz) {
                int drawW = Math.min(texSz, mapX + mapSize - tx);
                int drawH = Math.min(texSz, mapY + mapSize - ty);
                context.drawTexture(RenderPipelines.GUI_TEXTURED, tex,
                        tx, ty, 0.0f, 0.0f, drawW, drawH, texSz, texSz);
            }
        }
    }
}