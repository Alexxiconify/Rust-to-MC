package com.alexxiconify.rustmc.mixin.compat;

import com.alexxiconify.rustmc.ModBridge;
import com.alexxiconify.rustmc.RustMC;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.client.render.block.entity.BlockEntityRenderDispatcher;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Compatibility mixin for Better Block Entities (BBE).
 * BBE replaces dynamic block entity rendering with baked static models for chests,
 * signs, beds, etc. When BBE is installed, we skip our distance-culling logic for
 * block entities that BBE has already optimized, preventing double-optimization conflicts.
 *
 * When BBE is NOT installed, we apply a distance-based cull to skip rendering
 * block entities beyond a reasonable distance to save GPU draw calls.
 */
@Mixin(BlockEntityRenderDispatcher.class)
public class BBECompatMixin {

    @Inject(method = "render(Lnet/minecraft/block/entity/BlockEntity;FLnet/minecraft/client/util/math/MatrixStack;Lnet/minecraft/client/render/VertexConsumerProvider;)V",
            at = @At("HEAD"), cancellable = true, require = 0)
    private <E extends BlockEntity> void cullDistantBlockEntities(
            E blockEntity, float tickDelta,
            net.minecraft.client.util.math.MatrixStack matrices,
            net.minecraft.client.render.VertexConsumerProvider vertexConsumers,
            CallbackInfo ci) {
        if (!RustMC.CONFIG.isEnableBBECompat()) return;

        // If BBE is installed, it handles block entity rendering optimization — skip ours
        if (ModBridge.BETTERBLOCKENTITIES) return;

        // Apply distance culling for block entities when BBE is absent
        net.minecraft.client.MinecraftClient mc = net.minecraft.client.MinecraftClient.getInstance();
        if (mc.player == null || blockEntity.getPos() == null) return;

        double distSq = mc.player.squaredDistanceTo(
                blockEntity.getPos().getX() + 0.5,
                blockEntity.getPos().getY() + 0.5,
                blockEntity.getPos().getZ() + 0.5);
        double maxDist = mc.options.getClampedViewDistance() * 12.0;
        if (distSq > maxDist * maxDist) {
            ci.cancel();
        }
    }
}

