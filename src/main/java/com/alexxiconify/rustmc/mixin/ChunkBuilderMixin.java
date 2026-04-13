package com.alexxiconify.rustmc.mixin;
import com.alexxiconify.rustmc.ModBridge;
import net.minecraft.client.render.chunk.ChunkBuilder;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArg;
//
 //  Expands the ChunkBuilder worker thread count to use more cores.
 //  Vanilla default is typically max(1, cpus/2 - 1).
 //  We target max(2, cpus - 2) to keep 2 cores free for the main/render threads,
 //  extracting more parallelism on high-core-count machines.
 //  <p>
 //  Yields to Sodium's chunk pipeline when Sodium is installed (it manages its own).
 //  <p>
 //  Note: In MC 1.21.11 the internal executor API may have changed. The {@code require = 0}
 //  annotation ensures this mixin is silently skipped at runtime when the target is absent.
@SuppressWarnings("all") // Target may not exist in all MC versions; require=0 handles it
@Mixin(ChunkBuilder.class)
public class ChunkBuilderMixin {
    @ModifyArg(
        method = "<init>",
        at = @At(value = "INVOKE", target = "Ljava/util/concurrent/Executors;newFixedThreadPool(ILjava/util/concurrent/ThreadFactory;)Ljava/util/concurrent/ExecutorService;"),
        index = 0,
        require = 0 // don't crash if Sodium replaces this
    )
    private int expandWorkers(int original) {
        if (ModBridge.SODIUM || !com.alexxiconify.rustmc.RustMC.CONFIG.isEnableChunkBuilderExpand()) return original;
        int cores = Runtime.getRuntime().availableProcessors();
        return Math.max(2, cores - 2);
    }
}