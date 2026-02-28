package com.alexxiconify.rustmc.mixin;

import com.alexxiconify.rustmc.ModBridge;
import net.minecraft.client.render.chunk.ChunkBuilder;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArg;

/**
 * Expands the ChunkBuilder worker thread count to use more cores.
 * Vanilla default is typically max(1, cpus/2 - 1).
 * We target max(2, cpus - 2) to keep 2 cores free for the main/render threads,
 * extracting more parallelism on high-core-count machines.
 *
 * Yields to Sodium's chunk pipeline when Sodium is installed (it manages its own).
 */
@Mixin(ChunkBuilder.class)
public class ChunkBuilderMixin {

    @ModifyArg(
        method = "<init>",
        at = @At(value = "INVOKE", target = "Ljava/util/concurrent/Executors;newFixedThreadPool(ILjava/util/concurrent/ThreadFactory;)Ljava/util/concurrent/ExecutorService;"),
        index = 0,
        require = 0 // don't crash if Sodium replaces this
    )
    private int expandWorkers(int original) {
        if (ModBridge.SODIUM) return original; // Sodium manages its own pool
        int cores = Runtime.getRuntime().availableProcessors();
        return Math.max(2, cores - 2);
    }
}
