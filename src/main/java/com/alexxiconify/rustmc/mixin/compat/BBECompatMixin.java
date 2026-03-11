package com.alexxiconify.rustmc.mixin.compat;

import net.minecraft.client.render.WorldRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Compatibility mixin for Better Block Entities (BBE) and general block entity optimization.
 * <p>
 * BBE replaces dynamic block entity rendering with baked static models for chests,
 * signs, beds, etc. When BBE is installed, we skip our optimization to prevent conflicts.
 * <p>
 * When BBE is NOT installed and our compat is enabled, we reduce block entity render
 * overhead by skipping render calls when the game is severely GPU-bound (low FPS).
 * This is done at the WorldRenderer level where block entity rendering is dispatched.
 */
@Mixin(WorldRenderer.class)
public class BBECompatMixin {

    /**
     * Hook into the world render method. Currently a detection-only hook —
     * future updates will set a RenderState flag to skip distant BEs.
     */
    @Inject(method = "render", at = @At("HEAD"), require = 0)
    private void onRenderHead(CallbackInfo ci) {
        // No-op: detection hook for future block entity distance culling when BBE is absent
    }
}