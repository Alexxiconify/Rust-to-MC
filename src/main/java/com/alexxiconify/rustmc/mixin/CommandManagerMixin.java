package com.alexxiconify.rustmc.mixin;
import net.minecraft.server.command.CommandManager;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import com.alexxiconify.rustmc.NativeBridge;
import com.alexxiconify.rustmc.RustMC;
import com.mojang.brigadier.ParseResults;
import net.minecraft.server.command.ServerCommandSource;
import java.nio.charset.StandardCharsets;
@Mixin(CommandManager.class)
public class CommandManagerMixin {
    // In 1.21.11, execute() returns void (not int). Descriptor updated accordingly.
    @Inject(method = "execute(Lcom/mojang/brigadier/ParseResults;Ljava/lang/String;)V",
            at = @At("HEAD"), cancellable = true)
    private void onExecute(ParseResults<ServerCommandSource> parseResults, String command, CallbackInfo ci) {
        if (!NativeBridge.isReady()) return;
        if (!RustMC.CONFIG.isUseNativeCommands()) return;
        byte[] bytes = command.getBytes(StandardCharsets.UTF_8);
        int result = NativeBridge.executeCommand(bytes);
        if (result > 0) ci.cancel();
    }
}