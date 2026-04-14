package com.alexxiconify.rustmc.mixin.performance;
import com.alexxiconify.rustmc.ModBridge;
import net.minecraft.client.render.chunk.ChunkBuilder;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArg;

// Expands ChunkBuilder worker count while yielding to Sodium when it owns chunk threading.
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