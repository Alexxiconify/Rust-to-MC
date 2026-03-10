package com.alexxiconify.rustmc.mixin;

import net.minecraft.entity.ai.pathing.PathNodeNavigator;
import net.minecraft.entity.mob.MobEntity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.chunk.ChunkCache;
import net.minecraft.entity.ai.pathing.Path;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import com.alexxiconify.rustmc.NativeBridge;
import com.alexxiconify.rustmc.RustMC;

import java.util.Set;

/**
 * Hooks into mob pathfinding to get a distance estimate from Rust A*.
 * When the mob is already at the target (distance 0), vanilla pathfinding is
 * canceled with null (no path needed).  For all other distances the vanilla
 * A* runs normally; in a future update Rust will return a serialized Path.
 */
@Mixin(PathNodeNavigator.class)
public class PathfindingMixin {

    @Inject(
        method = "findPathToAny(Lnet/minecraft/world/chunk/ChunkCache;Lnet/minecraft/entity/mob/MobEntity;Ljava/util/Set;FIF)Lnet/minecraft/entity/ai/pathing/Path;",
        at = @At("HEAD"),
        cancellable = true
    )
    private void onFindPath(
            ChunkCache world, MobEntity mob,
            Set<BlockPos> positions, float distance, int range, float extraRange,
            CallbackInfoReturnable<Path> cir) {

        if (!NativeBridge.isReady() || !RustMC.CONFIG.isUseNativePathfinding()) return;

        BlockPos start = mob.getBlockPos();
        for (BlockPos end : positions) {
            int result = NativeBridge.findPathRaw(
                    start.getX(), start.getY(), start.getZ(),
                    end.getX(),   end.getY(),   end.getZ());

            if (result == 0) {
                // Already at target – cancel with null so vanilla skips unnecessary A*
                cir.setReturnValue(null);
                return;
            }
            // result > 0: Rust returned path cost/distance; let vanilla A* navigate
            // result < 0: error – let vanilla A* handle it
        }
    }
}