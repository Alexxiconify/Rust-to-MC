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
    @Inject(method = "decode", at = @At("HEAD"), cancellable = true, require = 0)
    private void onDecode(ChannelHandlerContext ctx, ByteBuf buf, List<Object> objects, CallbackInfo ci) {
        if (!NativeBridge.isReady() || !com.alexxiconify.rustmc.RustMC.CONFIG.isUseNativeCompression()) return;

        int readable = buf.readableBytes();
        if (readable <= 0) return;

        int result;
        if (buf.hasMemoryAddress()) {
            // Zero-allocation path for direct buffers
            result = NativeBridge.invokeProcessPacketDirect(buf.memoryAddress() + buf.readerIndex(), readable);
        } else if (buf.hasArray()) {
            // Zero-allocation path for heap buffers (via JNI pinning) Note: NativeBridge.invokeProcessPacket uses get_array_elements_critical
            result = NativeBridge.invokeProcessPacket(buf.array(), readable);
        } else {
            // Fallback for complex buffer types
            byte[] inputBytes = new byte[readable];
            buf.getBytes(buf.readerIndex(), inputBytes);
            result = NativeBridge.invokeProcessPacket(inputBytes, readable);
        }

        if (result > 0) {
            buf.skipBytes(readable); 
            ci.cancel();
        }
    }
}