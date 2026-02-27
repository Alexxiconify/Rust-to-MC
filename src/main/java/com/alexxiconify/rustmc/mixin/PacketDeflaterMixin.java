package com.alexxiconify.rustmc.mixin;

import java.lang.foreign.ValueLayout;
import net.minecraft.network.handler.PacketDeflater;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import com.alexxiconify.rustmc.NativeBridge;
import com.alexxiconify.rustmc.RustMC;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import java.util.zip.Deflater;
import java.lang.foreign.MemorySegment;
import java.lang.foreign.Arena;

@Mixin(PacketDeflater.class)
public class PacketDeflaterMixin {
    @Shadow @Final private Deflater deflater;
    @Shadow private int compressionThreshold;

    @SuppressWarnings("preview")
    @Inject(method = "encode", at = @At("HEAD"), cancellable = true)
    private void onEncode(ChannelHandlerContext channelHandlerContext, ByteBuf byteBuf, ByteBuf byteBuf2, CallbackInfo ci) {
        if (!NativeBridge.isReady()) return;
        if (!RustMC.CONFIG.isUseNativeCompression()) return;
        int i = byteBuf.readableBytes();
        if (i < this.compressionThreshold) {
            byteBuf2.writeInt(0);
            byteBuf2.writeBytes(byteBuf);
        } else {
            byteBuf2.writeInt(i);
            try (Arena arena = Arena.ofConfined()) {
                byte[] input = new byte[i];
                byteBuf.getBytes(byteBuf.readerIndex(), input); // Get without advancing index
                MemorySegment inSeg = arena.allocate(i);
                inSeg.copyFrom(MemorySegment.ofArray(input));
                
                int maxOut = i + 64; // zlib overhead
                MemorySegment outSeg = arena.allocate(maxOut);
                
                int compressedLen = NativeBridge.invokeCompress(inSeg, i, outSeg, maxOut);
                if (compressedLen > 0) {
                    byte[] result = outSeg.asSlice(0, compressedLen).toArray(ValueLayout.JAVA_BYTE);
                    byteBuf2.writeBytes(result);
                    ci.cancel();
                }
            }
        }
    }
}
