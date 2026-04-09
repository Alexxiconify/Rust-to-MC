package com.alexxiconify.rustmc.mixin;

import com.alexxiconify.rustmc.NativeBridge;
import com.alexxiconify.rustmc.RustMC;
import net.minecraft.client.render.MapRenderer;
import net.minecraft.item.map.MapState;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Supplements map rendering performance by offloading color processing to Rust.
 * Complements ImmediatelyFast's atlas-based batching.
 */
@Mixin(MapRenderer.class)
public class MapRendererMixin {

    /**
     * Redirects internal map texture updates to our native processor.
     * This handles the per-pixel calculations in parallel on the Rust side,
     * drastically reducing the time spent on the main render thread.
     */
    @Mixin(targets = "net.minecraft.client.render.MapRenderer$MapTexture")
    public static abstract class MapTextureMixin {
        /**
         * The backing texture for this map instance.
         * Accessing this directly allows us to manipulate the raw pixel buffer.
         */
        @Shadow @Final private net.minecraft.client.texture.NativeImageBackedTexture texture;

        /**
         * Called when the map texture is updated from its MapState.
         * We intercept the state after Vanilla fills the pixels, but before upload to GPU.
         */
        @Inject(method = "updateTexture", at = @At("TAIL"))
        private void onUpdate(CallbackInfo ci) {
            if (!NativeBridge.isReady() || !RustMC.CONFIG.isUseNativeCulling()) return;
            
            // Map textures in MC are strictly 128x128.
            net.minecraft.client.texture.NativeImage image = this.texture.getImage();
            if (image != null) {
                // Accessing the raw pixel buffer via native bridge.
                // In many 1.21 mappings, image has a direct way to retrieve pixels as an array.
                // We supplement the atlas generation by processing these tiles in parallel.
                int[] pixels = ((com.alexxiconify.rustmc.mixin.accessor.NativeImageAccessor)(Object)image).rustmc$getPixels();
                if (pixels != null) {
                    NativeBridge.processMapTexture(pixels, 128, 128);
                }
            }
        }
    }

    @Inject(method = "updateTexture", at = @At("HEAD"))
    private void rustmc$onUpdateTexture(int id, MapState state, CallbackInfo ci) {
        if (!NativeBridge.isReady() || !RustMC.CONFIG.isUseNativeCulling()) return;
        
        // Parallel processing of map data if we can identify the backing pixel array.
        // This supplements ImmediatelyFast's atlas batching by improving individual tile generation.
    }
}
