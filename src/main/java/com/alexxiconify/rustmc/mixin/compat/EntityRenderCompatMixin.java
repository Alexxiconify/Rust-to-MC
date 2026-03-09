package com.alexxiconify.rustmc.mixin.compat;

import com.alexxiconify.rustmc.ModBridge;
import com.alexxiconify.rustmc.RustMC;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.entity.EntityRenderDispatcher;
import net.minecraft.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Entity rendering optimization and compatibility with:
 * - EMF (Entity Model Features) — custom entity models with OptiFine-format CEM
 * - ETF (Entity Texture Features) — random/biome entity textures
 * - EntityCulling — occlusion-based entity culling
 *
 * When EMF/ETF are present, entity rendering is heavier due to custom models/textures.
 * We add a distance-based LOD skip for entities beyond a threshold when these mods
 * aren't handling it already via EntityCulling.
 *
 * Maintains vanilla parity: entities still exist and function, only rendering is skipped.
 */
@Mixin(EntityRenderDispatcher.class)
public class EntityRenderCompatMixin {

    @Inject(method = "render", at = @At("HEAD"), cancellable = true, require = 0)
    private <E extends Entity> void optimizeEntityRender(
            E entity, double x, double y, double z, float tickDelta,
            net.minecraft.client.util.math.MatrixStack matrices,
            net.minecraft.client.render.VertexConsumerProvider vertexConsumers,
            int light, CallbackInfo ci) {

        // If EntityCulling is installed, it handles visibility — skip our check
        if (ModBridge.ENTITYCULLING && RustMC.CONFIG.isEnableEntityCullingCompat()) return;
        if (!RustMC.CONFIG.isEnableEMFCompat()) return;

        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc.player == null) return;

        // Compute LOD skip distance — wider when EMF/ETF add rendering overhead
        double baseDist = mc.options.getClampedViewDistance() * 10.0;
        if (ModBridge.ENTITY_MODEL_FEATURES || ModBridge.ENTITY_TEXTURE_FEATURES) {
            baseDist *= 0.75; // Reduce render distance for expensive custom entity rendering
        }

        double distSq = mc.player.squaredDistanceTo(entity);
        if (distSq > baseDist * baseDist) {
            ci.cancel();
        }
    }
}

