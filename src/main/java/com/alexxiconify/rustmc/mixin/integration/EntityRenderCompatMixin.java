package com.alexxiconify.rustmc.mixin.integration;
import com.alexxiconify.rustmc.ModBridge;
import com.alexxiconify.rustmc.RustMC;
import com.alexxiconify.rustmc.util.HudManager;
import net.minecraft.client.render.WorldRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
//
 //  Entity rendering optimization and compatibility with:
 //  - EMF (Entity Model Features) — custom entity models with OptiFine-format CEM
 //  - ETF (Entity Texture Features) — random/biome entity textures
 //  - EntityCulling — occlusion-based entity culling
 //  - ImmediatelyFast — batched entity rendering optimizations
 //  <p>
//  Sets shared flags in {@link HudManager.RenderState} at the start of each render pass
 //  that our particle culling and other hooks read to decide culling thresholds.
 //noinspection MixinClassReference
@Mixin(WorldRenderer.class)
public class EntityRenderCompatMixin {
    private EntityRenderCompatMixin() {}
    @SuppressWarnings("java:S2696") // Mixin @Inject has to be; writing static RenderState fields is intentional
    @Inject(method = "render", at = @At("HEAD"), require = 0)
    private void onEntityRenderPass(CallbackInfo ci) {
        if (ModBridge.ENTITYCULLING && RustMC.CONFIG.isEnableEntityCullingCompat()) {
            HudManager.RenderState.heavyEntityModsActive = false;
            return;
        }
        boolean emfActive = ModBridge.ENTITY_MODEL_FEATURES && RustMC.CONFIG.isEnableEMFCompat();
        boolean etfActive = ModBridge.ENTITY_TEXTURE_FEATURES && RustMC.CONFIG.isEnableETFCompat();
        HudManager.RenderState.heavyEntityModsActive = emfActive || etfActive;
        HudManager.RenderState.immediatelyFastActive = ModBridge.IMMEDIATELYFAST && RustMC.CONFIG.isEnableImmediatelyFastCompat();
    }
}