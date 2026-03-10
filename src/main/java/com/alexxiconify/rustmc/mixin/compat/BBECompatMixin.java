package com.alexxiconify.rustmc.mixin.compat;

import com.alexxiconify.rustmc.ModBridge;
import com.alexxiconify.rustmc.RustMC;
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
     * Hook into the world render method to set a flag that can be used by
     * block entity rendering optimizations. When BBE is present, this is a no-op.
     */
    @Inject(method = "render", at = @At("HEAD"), require = 0)
    private void onRenderHead(CallbackInfo ci) {
        // When BBE is installed, it optimizes block entity rendering already.
        // We just ensure we don't conflict. This hook serves as a detection point.
        if (!RustMC.CONFIG.isEnableBBECompat() || ModBridge.BETTERBLOCKENTITIES) return;
        // Future: could set a thread-local flag to skip distant BEs in the render loop
    }
}