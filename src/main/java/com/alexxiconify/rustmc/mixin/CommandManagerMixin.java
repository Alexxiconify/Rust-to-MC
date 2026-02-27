package com.alexxiconify.rustmc.mixin;

import net.minecraft.server.command.CommandManager;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import com.alexxiconify.rustmc.NativeBridge;
import com.alexxiconify.rustmc.RustMC;
import com.mojang.brigadier.ParseResults;
import net.minecraft.server.command.ServerCommandSource;
import java.lang.foreign.MemorySegment;
import java.lang.foreign.Arena;
import java.nio.charset.StandardCharsets;

@Mixin(CommandManager.class)
public class CommandManagerMixin {
    @SuppressWarnings("preview")
    @Inject(method = "execute(Lcom/mojang/brigadier/ParseResults;Ljava/lang/String;)I", at = @At("HEAD"), cancellable = true)
    private void onExecute(ParseResults<ServerCommandSource> parseResults, String command, CallbackInfoReturnable<Integer> cir) {
        if (!NativeBridge.isReady()) return;
        if (!RustMC.CONFIG.isUseNativeCommands()) return;
        try (Arena arena = Arena.ofConfined()) {
            byte[] bytes = command.getBytes(StandardCharsets.UTF_8);
            MemorySegment segment = arena.allocate(bytes.length);
            segment.copyFrom(MemorySegment.ofArray(bytes));
            int result = NativeBridge.executeCommand(segment, bytes.length);
            // Only cancel vanilla processing if Rust explicitly handled the command (result > 0).
            // result == -1 means "not handled"; result == 0 means "Rust handled it, success=0"
            if (result > 0) {
                cir.setReturnValue(result);
            }
        }
    }
}
