package com.alexxiconify.rustmc.mixin;

import net.minecraft.network.handler.DecoderHandler;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import com.alexxiconify.rustmc.NativeBridge;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import java.util.List;

@Mixin(DecoderHandler.class)
public class DecoderHandlerMixin {
    @Inject(method = "decode", at = @At("HEAD"), cancellable = true)
    private void onDecode(ChannelHandlerContext ctx, ByteBuf buf, List<Object> objects, CallbackInfo ci) {
        if (!NativeBridge.isReady()) return;

        int readable = buf.readableBytes();
        if (readable <= 0) return;

        byte[] inputBytes = new byte[readable];
        buf.getBytes(buf.readerIndex(), inputBytes);

        // Offload the raw packet to Rust
        int result = NativeBridge.invokeProcessPacket(inputBytes, readable);
        if (result > 0) {
            // Rust handled the packet completely natively (e.g. heartbeat responses, keepalives)
            buf.skipBytes(readable); 
            ci.cancel();
        }
    }
}
