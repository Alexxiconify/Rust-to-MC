package com.alexxiconify.rustmc.mixin;


import net.minecraft.entity.ai.pathing.PathNodeNavigator;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import com.alexxiconify.rustmc.NativeBridge;


@Mixin(PathNodeNavigator.class)
public class PathfindingMixin {
    @Inject(method = "findPathToAny(Lnet/minecraft/world/chunk/ChunkCache;Lnet/minecraft/entity/mob/MobEntity;Ljava/util/Set;FIF)Lnet/minecraft/entity/ai/pathing/Path;", at = @At("HEAD"), cancellable = true)
    private void onFindPath(net.minecraft.world.chunk.ChunkCache world, net.minecraft.entity.mob.MobEntity mob, java.util.Set<net.minecraft.util.math.BlockPos> positions, float distance, int range, float extraRange, CallbackInfoReturnable<?> cir) {
        if (!NativeBridge.isReady()) return;
        net.minecraft.util.math.BlockPos start = mob.getBlockPos();
        for (net.minecraft.util.math.BlockPos end : positions) {
            int result = NativeBridge.findPathRaw(start.getX(), start.getY(), start.getZ(), end.getX(), end.getY(), end.getZ());
            if (result == 0) { // Found path
                // For now, let vanilla finish if Rust finds it (POC)
            }
        }
    }
}
