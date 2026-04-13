package com.alexxiconify.rustmc.mixin;

import com.alexxiconify.rustmc.NativeBridge;
import net.minecraft.client.render.MapRenderer;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

// Supplements map rendering performance by offloading color processing to Rust. Complements ImmediatelyFast's atlas-based batching.
@Mixin(MapRenderer.class)
public class MapRendererMixin {

    private MapRendererMixin() {}

    // Redirects internal map texture updates to our native processor. Handles per-pixel calculations parallel on the Rust side. Drastically reduces time spent on the main render thread.
    @Mixin(targets = "net.minecraft.client.render.MapRenderer$MapTexture")
    public abstract static class MapTextureMixin {
        // Backing texture for this map instance. Direct access allows raw pixel buffer manipulation.
        @Shadow @Final private net.minecraft.client.texture.NativeImageBackedTexture texture;

        // Called when map texture updates from its MapState. Intercepts state after Vanilla fill, before GPU upload.
        @Inject(method = {"updateTexture", "update"}, at = @At("TAIL"), require = 0)
        private void onUpdate(CallbackInfo ci) {
            if (!NativeBridge.isReady()) return;
            
            // Map textures in MC are strictly 128x128.
            net.minecraft.client.texture.NativeImage image = this.texture.getImage();
            if (image != null) {
                long ptr = ((com.alexxiconify.rustmc.mixin.accessor.NativeImageAccessor) (Object) image).getPointer();
                if (ptr != 0) {
                    NativeBridge.processMapTexturePtr(ptr, 128, 128);
                }
            }
        }
    }

}