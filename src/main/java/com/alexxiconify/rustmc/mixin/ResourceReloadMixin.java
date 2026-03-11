package com.alexxiconify.rustmc.mixin;

import com.alexxiconify.rustmc.RustMC;
import net.minecraft.resource.ReloadableResourceManagerImpl;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArg;

import java.util.concurrent.Executor;
import java.util.concurrent.ForkJoinPool;

/**
 * Increases the parallelism of resource reloading by replacing the prepare executor
 * with a higher-parallelism ForkJoinPool. Vanilla uses a small thread pool which
 * under-utilizes modern multicore CPUs during resource pack loading.
 * <p>
 * The pool is eagerly created as a static singleton to avoid creation latency at
 * first reload. Uses asyncMode=true (FIFO) for better throughput on I/O-heavy tasks.
 */
@Mixin(ReloadableResourceManagerImpl.class)
public class ResourceReloadMixin {

    @Unique
    private static final ForkJoinPool RELOAD_POOL;

    static {
        int cores = Runtime.getRuntime().availableProcessors();
        // Use all cores minus 1 (leave one for the main/render thread)
        // Minimum 4 threads to handle parallel resource loading effectively
        int workers = Math.max(4, cores - 1);
        RustMC.LOGGER.debug("[Rust-MC] Creating resource reload pool with {} threads (async mode)", workers);
        RELOAD_POOL = new ForkJoinPool(
            workers,
            ForkJoinPool.defaultForkJoinWorkerThreadFactory,
            null,
            true // asyncMode=true: FIFO scheduling, better for I/O-bound tasks
        );
    }

    @ModifyArg(
        method = "reload",
        at = @At(value = "INVOKE", target = "Lnet/minecraft/resource/SimpleResourceReload;start(Lnet/minecraft/resource/ResourceManager;Ljava/util/List;Ljava/util/concurrent/Executor;Ljava/util/concurrent/Executor;Ljava/util/concurrent/CompletableFuture;Z)Lnet/minecraft/resource/ResourceReload;"),
        index = 2,
        require = 0
    )
    private Executor boostPrepareExecutor(Executor original) {
        return RELOAD_POOL;
    }
}