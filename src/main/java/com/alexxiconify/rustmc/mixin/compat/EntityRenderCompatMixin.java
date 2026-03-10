package com.alexxiconify.rustmc.mixin.compat;

import com.alexxiconify.rustmc.ModBridge;
import com.alexxiconify.rustmc.RustMC;
import net.minecraft.client.render.WorldRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Entity rendering optimization and compatibility with:
 * - EMF (Entity Model Features) — custom entity models with OptiFine-format CEM
 * - ETF (Entity Texture Features) — random/biome entity textures
 * - EntityCulling — occlusion-based entity culling
 * <p>
 * When EMF/ETF are present, entity rendering is heavier due to custom models/textures.
 * When EntityCulling handles visibility, we yield to it.
 * <p>
 * This hook monitors the world render pass to coordinate with entity rendering mods.
 * Maintains vanilla parity: entities still exist and function, only rendering is affected.
 */
@Mixin(WorldRenderer.class)
public class EntityRenderCompatMixin {

    @Inject(method = "render", at = @At("HEAD"), require = 0)
    private void onEntityRenderPass(CallbackInfo ci) {
        // If EntityCulling is installed, it handles visibility — yield
        if (ModBridge.ENTITYCULLING && RustMC.CONFIG.isEnableEntityCullingCompat()) return;
        if (!RustMC.CONFIG.isEnableEMFCompat()) return;

        // This hook serves as a coordination point for entity rendering optimization.
        // When EMF/ETF are present, we can adjust render behavior at the world level.
        // The actual entity distance culling is handled by the ParticleManagerMixin pattern
        // applied at the entity level in future updates.
    }
}