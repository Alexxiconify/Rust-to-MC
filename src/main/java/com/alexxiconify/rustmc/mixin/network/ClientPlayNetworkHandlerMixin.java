package com.alexxiconify.rustmc.mixin.network;

import com.alexxiconify.rustmc.NativeBridge;
import com.alexxiconify.rustmc.RustMC;
import com.alexxiconify.rustmc.mixin.accessor.ChunkDataS2CPacketAccessor;
import net.minecraft.client.network.ClientPlayNetworkHandler;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.network.packet.s2c.play.ChunkDataS2CPacket;
import io.netty.buffer.Unpooled;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.concurrent.atomic.AtomicInteger;

// Preview chunk-ingest hook: forwards lightweight chunk metadata through JNI.
// Optimized with accessors to avoid reflection overhead on every chunk packet.
@Mixin(ClientPlayNetworkHandler.class)
public class ClientPlayNetworkHandlerMixin {
    @Unique private static final long VALIDATION_LOG_INTERVAL_NS = 5_000_000_000L;
    @Unique private static final int SNAPSHOT_SAMPLE_MASK = 0x07; // 1 out of 8 packets
    @Unique private static volatile long lastValidationLogNs;
    @Unique private static final AtomicInteger ingestSequence = new AtomicInteger(0);

    @Inject(method = "onChunkData(Lnet/minecraft/network/packet/s2c/play/ChunkDataS2CPacket;)V", at = @At("HEAD"), require = 0)
    private void rustmcOnChunkData(ChunkDataS2CPacket packet, CallbackInfo ci) {
        if (!com.alexxiconify.rustmc.RustMC.CONFIG.isEnableChunkIngestOffload()) {
            return;
        }
        boolean shouldSampleSnapshot = (ingestSequence.incrementAndGet() & SNAPSHOT_SAMPLE_MASK) == 0;
        
        if (!shouldSampleSnapshot) {
            return;
        }

        ChunkDataS2CPacketAccessor accessor = (ChunkDataS2CPacketAccessor) packet;
        int chunkX = accessor.getX();
        int chunkZ = accessor.getZ();
        
        byte[] payload = snapshotPayload(packet, chunkX, chunkZ);
        if (payload.length <= 8) {
            return;
        }
        NativeBridge.processChunkData(payload, chunkX, chunkZ);
        
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
    private static byte[] snapshotPayload(ChunkDataS2CPacket packet, int chunkX, int chunkZ) {
        ChunkDataS2CPacketAccessor accessor = (ChunkDataS2CPacketAccessor) packet;
        Object chunkData = accessor.getChunkData();
        
        if (chunkData == null) {
            return coordPayload(chunkX, chunkZ);
        }

        // We still use a small bit of reflection for the inner ChunkData if we can't find a direct way,
        // but the main packet access is now reflection-free.
        PacketByteBuf buf = new PacketByteBuf(Unpooled.buffer(16_384));
        try {
            // For 1.21.1, ChunkData has a public write(PacketByteBuf) method.
            java.lang.reflect.Method writeMethod = chunkData.getClass().getMethod("write", PacketByteBuf.class);
            writeMethod.invoke(chunkData, buf);
            
            int len = buf.readableBytes();
            if (len <= 0) {
                return coordPayload(chunkX, chunkZ);
            }
            byte[] out = new byte[len];
            buf.getBytes(buf.readerIndex(), out);
            return out;
        } catch (Exception ignored) {
            return coordPayload(chunkX, chunkZ);
        } finally {
            buf.release();
        }
    }

    @Unique
    private static void maybeLogValidationStats(int chunkX, int chunkZ, int payloadBytes) {
        long now = System.nanoTime();
        if (now - lastValidationLogNs < VALIDATION_LOG_INTERVAL_NS) {
            return;
        }
        lastValidationLogNs = now;
        long[] javaStats = NativeBridge.getChunkIngestStats();
        long[] nativeStats = NativeBridge.getMetrics(false);
        long nativePackets = nativeStats.length > 3 ? nativeStats[3] : 0L;
        long nativeBytes = nativeStats.length > 4 ? nativeStats[4] : 0L;
        RustMC.LOGGER.info(
            "[Rust-MC] Chunk ingest validation: chunk=({}, {}), payload={}B, sampledPayload=true, attempts={}, forwards={}, failures={}, avgJNI={}us, nativePackets={}, nativeBytes={}B",
            chunkX,
            chunkZ,
            payloadBytes,
            javaStats[0],
            javaStats[1],
            javaStats[2],
            javaStats[3],
            nativePackets,
            nativeBytes
        );
    }
}