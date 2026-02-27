package com.alexxiconify.rustmc.mixin;

import java.lang.foreign.Arena;
import java.lang.foreign.MemorySegment;
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

@Mixin(PacketDeflater.class)
public class PacketDeflaterMixin {
    @Shadow @Final private Deflater deflater;
    @Shadow private int compressionThreshold;

    @SuppressWarnings("preview")
    @Inject(method = "encode", at = @At("HEAD"), cancellable = true)
    private void onEncode(ChannelHandlerContext ctx, ByteBuf in, ByteBuf out, CallbackInfo ci) {
        if (!NativeBridge.isReady() || !RustMC.CONFIG.isUseNativeCompression()) return;

        int readable = in.readableBytes();
        if (readable < this.compressionThreshold) {
            // Below threshold: write uncompressed with 0-length header (vanilla behaviour)
            out.writeInt(0);
            out.writeBytes(in);
            ci.cancel();
            return;
        }

        // Worst-case zlib output: input + 50% + 128 byte header
        int maxOut = readable + (readable >> 1) + 128;
        try (Arena arena = Arena.ofConfined()) {
            byte[] inputBytes = new byte[readable];
            in.getBytes(in.readerIndex(), inputBytes);

            MemorySegment inSeg  = arena.allocate(readable);
            MemorySegment outSeg = arena.allocate(maxOut);
            inSeg.copyFrom(MemorySegment.ofArray(inputBytes));

            int compressedLen = NativeBridge.invokeCompress(inSeg, readable, outSeg, maxOut);
            if (compressedLen > 0) {
                out.writeInt(readable); // uncompressed size header
                out.writeBytes(outSeg.asSlice(0, compressedLen).toArray(ValueLayout.JAVA_BYTE));
                ci.cancel();
                // fall through to vanilla if compressedLen <= 0
            }
        }
    }
}
