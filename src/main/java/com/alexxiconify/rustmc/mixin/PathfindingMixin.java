package com.alexxiconify.rustmc.mixin;


import net.minecraft.entity.ai.pathing.PathNodeNavigator;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import com.alexxiconify.rustmc.NativeBridge;
import com.alexxiconify.rustmc.RustMC;


@Mixin(PathNodeNavigator.class)
public class PathfindingMixin {
    @Inject(
        method = "findPathToAny(Lnet/minecraft/world/chunk/ChunkCache;Lnet/minecraft/entity/mob/MobEntity;Ljava/util/Set;FIF)Lnet/minecraft/entity/ai/pathing/Path;",
        at = @At("HEAD"),
        cancellable = true
    )
    private void onFindPath(
            net.minecraft.world.chunk.ChunkCache world,
            net.minecraft.entity.mob.MobEntity mob,
            java.util.Set<net.minecraft.util.math.BlockPos> positions,
            float distance, int range, float extraRange,
            CallbackInfoReturnable<net.minecraft.entity.ai.pathing.Path> cir) {

        if (!NativeBridge.isReady()) return;
        if (!RustMC.CONFIG.isUseNativePathfinding()) return;

        net.minecraft.util.math.BlockPos start = mob.getBlockPos();
        for (net.minecraft.util.math.BlockPos end : positions) {
            int result = NativeBridge.findPathRaw(
                    start.getX(), start.getY(), start.getZ(),
                    end.getX(), end.getY(), end.getZ());
            // result is Manhattan distance; 0 means mob is already at target.
            if (result == 0) {
                // Mob is already at the target – cancel pathfinding with null (no path needed).
                cir.setReturnValue(null);
                return;
            }
            // For any other result, fall through to vanilla A* which handles actual navigation.
            // TODO: Build and return a real Path object from Rust data in a future update.
        }
    }
}
