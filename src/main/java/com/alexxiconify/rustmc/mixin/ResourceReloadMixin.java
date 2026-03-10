package com.alexxiconify.rustmc.mixin;

import com.alexxiconify.rustmc.RustMC;
import net.minecraft.resource.ReloadableResourceManagerImpl;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArg;

import java.util.concurrent.Executor;
import java.util.concurrent.ForkJoinPool;

/**
 * Increases the parallelism of resource reloading by replacing the prepare executor
 * with a higher-parallelism ForkJoinPool. Vanilla uses a small thread pool which
 * under-utilizes modern multicore CPUs during resource pack loading.
 * <p>
 * This can reduce initial load time by 1-3 seconds on 8+ core machines.
 */
@Mixin(ReloadableResourceManagerImpl.class)
public class ResourceReloadMixin {

    @ModifyArg(
        method = "reload",
        at = @At(value = "INVOKE", target = "Lnet/minecraft/resource/SimpleResourceReload;start(Lnet/minecraft/resource/ResourceManager;Ljava/util/List;Ljava/util/concurrent/Executor;Ljava/util/concurrent/Executor;Ljava/util/concurrent/CompletableFuture;Z)Lnet/minecraft/resource/ResourceReload;"),
        index = 2,
        require = 0
    )
    private Executor boostPrepareExecutor(Executor original) {
        int cores = Runtime.getRuntime().availableProcessors();
        if (cores <= 4) return original; // Don't boost on low-core machines

        int workers = Math.max(4, cores - 2);
        RustMC.LOGGER.debug("[Rust-MC] Boosting resource reload prepare executor to {} threads", workers);
        return new ForkJoinPool(workers);
    }
}