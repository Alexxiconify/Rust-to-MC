package com.alexxiconify.rustmc.mixin.network;

import com.alexxiconify.rustmc.NativeBridge;
import com.alexxiconify.rustmc.RustMC;
import net.minecraft.client.network.ClientPlayNetworkHandler;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.network.packet.s2c.play.ChunkDataS2CPacket;
import io.netty.buffer.Unpooled;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.lang.reflect.Method;
import java.util.concurrent.atomic.AtomicInteger;

@Mixin(ClientPlayNetworkHandler.class)
public class ClientPlayNetworkHandlerMixin {
    @Unique private static final long VALIDATION_LOG_INTERVAL_NS = 5_000_000_000L;
    @Unique private static final int SNAPSHOT_SAMPLE_MASK = 0x07;
    @Unique private static volatile long lastValidationLogNs;
    @Unique private static final AtomicInteger ingestSequence = new AtomicInteger(0);
    @Unique private static Method cachedWriteMethod;

    @Inject(method = "onChunkData", at = @At("HEAD"), cancellable = false)
    private void rustmcOnChunkData(ChunkDataS2CPacket packet, CallbackInfo ci) {
        if (!RustMC.CONFIG.isEnableChunkIngestOffload()) return;

        if ((ingestSequence.incrementAndGet() & SNAPSHOT_SAMPLE_MASK) != 0) return;

        // Use public getter methods provided by the packet class
        int chunkX = packet.getChunkX();
        int chunkZ = packet.getChunkZ();

        byte[] payload = snapshotPayload(packet, chunkX, chunkZ);
        if (payload.length > 8) {
            NativeBridge.processChunkData(payload, chunkX, chunkZ);
        }

        if (RustMC.CONFIG.isEnableChunkIngestValidation()) {
            maybeLogValidationStats(chunkX, chunkZ, payload.length);
        }
    }

    @Unique
    private static byte[] coordPayload(int chunkX, int chunkZ) {
        return new byte[] {
          (byte) (chunkX >>> 24), (byte) (chunkX >>> 16), (byte) (chunkX >>> 8), (byte) chunkX,
          (byte) (chunkZ >>> 24), (byte) (chunkZ >>> 16), (byte) (chunkZ >>> 8), (byte) chunkZ
        };
    }

    @Unique
    private static byte[] snapshotPayload(ChunkDataS2CPacket packet, int x, int z) {
        // Access the chunk data object directly via public API
        var chunkData = packet.getChunkData();

        if (chunkData == null) return coordPayload(x, z);

        try {
            if (cachedWriteMethod == null) {
                // Cached method lookup for the internal write() call
                cachedWriteMethod = chunkData.getClass().getMethod("write", PacketByteBuf.class);
            }

            PacketByteBuf buf = new PacketByteBuf(Unpooled.buffer(32768));
            try {
                cachedWriteMethod.invoke(chunkData, buf);
                int len = buf.readableBytes();
                if (len <= 0) return coordPayload(x, z);

                byte[] out = new byte[len];
                buf.readBytes(out);
                return out;
            } finally {
                buf.release();
            }
        } catch (Exception e) {
            return coordPayload(x, z);
        }
    }

    @Unique
    private static void maybeLogValidationStats(int chunkX, int chunkZ, int payloadBytes) {
        long now = System.nanoTime();
        if (now - lastValidationLogNs < VALIDATION_LOG_INTERVAL_NS) return;

        lastValidationLogNs = now;
        long[] javaStats = NativeBridge.getChunkIngestStats();
        long[] nativeStats = NativeBridge.getMetrics(false);

        RustMC.LOGGER.info(
          "[Rust-MC] Chunk ingest validation: chunk=({}, {}), payload={}B, attempts={}, forwards={}, failures={}, avgJNI={}us, nativePackets={}, nativeBytes={}B",
          chunkX, chunkZ, payloadBytes, javaStats[0], javaStats[1], javaStats[2], javaStats[3],
          (nativeStats.length > 3 ? nativeStats[3] : 0L),
          (nativeStats.length > 4 ? nativeStats[4] : 0L)
        );
    }
}