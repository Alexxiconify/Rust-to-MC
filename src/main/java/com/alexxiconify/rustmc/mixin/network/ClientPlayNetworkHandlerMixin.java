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

// Preview chunk-ingest hook: forwards lightweight chunk metadata through JNI.
// Full decode replacement stays disabled until parity validation is complete.
@Mixin(ClientPlayNetworkHandler.class)
public class ClientPlayNetworkHandlerMixin {
    @Unique private static final long VALIDATION_LOG_INTERVAL_NS = 5_000_000_000L;
    @Unique private static volatile boolean coordAccessResolved;
    @Unique private static volatile long lastValidationLogNs;
    @Unique private static Method chunkXMethod;
    @Unique private static Method chunkZMethod;

    @Inject(method = "onChunkData(Lnet/minecraft/network/packet/s2c/play/ChunkDataS2CPacket;)V", at = @At("HEAD"), require = 0)
    private void rustmcOnChunkData(ChunkDataS2CPacket packet, CallbackInfo ci) {
        if (!com.alexxiconify.rustmc.RustMC.CONFIG.isEnableChunkIngestOffload()) {
            return;
        }
        int chunkX = resolveChunkCoord(packet, true);
        int chunkZ = resolveChunkCoord(packet, false);
        byte[] payload = snapshotPayload(packet, chunkX, chunkZ);
        NativeBridge.processChunkData(payload, chunkX, chunkZ);
        maybeLogValidationStats(chunkX, chunkZ, payload.length);
    }

    @Unique
    private static int resolveChunkCoord(Object packet, boolean x) {
        if (packet == null) {
            return 0;
        }
        if (!coordAccessResolved) {
            resolveCoordAccess(packet.getClass());
        }
        try {
            Method m = x ? chunkXMethod : chunkZMethod;
            if (m != null) {
                Object out = m.invoke(packet);
                if (out instanceof Number n) {
                    return n.intValue();
                }
            }
        } catch (Exception ignored) {
            // Keep ingest path fail-safe; fallback chunk coord is 0.
        }
        return 0;
    }

    @Unique
    private static synchronized void resolveCoordAccess(Class<?> packetClass) {
        if (coordAccessResolved) {
            return;
        }
        chunkXMethod = findNoArgIntMethod(packetClass, "getChunkX", "getX");
        chunkZMethod = findNoArgIntMethod(packetClass, "getChunkZ", "getZ");
        coordAccessResolved = true;
    }

    @Unique
    private static Method findNoArgIntMethod(Class<?> type, String... names) {
        for (String name : names) {
            try {
                Method m = type.getDeclaredMethod(name);
                if (m.getParameterCount() == 0 && (m.getReturnType() == int.class || Number.class.isAssignableFrom(m.getReturnType()))) {
                    return m;
                }
            } catch (NoSuchMethodException ignored) {
                // Try next alias.
            }
        }
        return null;
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
        if (packet == null) {
            return coordPayload(chunkX, chunkZ);
        }
        byte[] packetBytes = extractByteArray(packet);
        if (packetBytes.length > 0) {
            return packetBytes;
        }
        Object chunkData = invokeNoArg(packet, "getChunkData");
        byte[] chunkDataBytes = extractByteArray(chunkData);
        if (chunkDataBytes.length > 0) {
            return chunkDataBytes;
        }
        PacketByteBuf buf = new PacketByteBuf(Unpooled.buffer(16_384));
        try {
            if (!invokeWriteWithPacketByteBuf(chunkData, buf)) {
                return coordPayload(chunkX, chunkZ);
            }
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
    private static Object invokeNoArg(Object target, String methodName) {
        if (target == null) {
            return null;
        }
        try {
            Method method = target.getClass().getMethod(methodName);
            return method.invoke(target);
        } catch (Exception ignored) {
            return null;
        }
    }

    @Unique
    private static boolean invokeWriteWithPacketByteBuf(Object target, PacketByteBuf buf) {
        if (target == null || buf == null) {
            return false;
        }
        try {
            Method method = target.getClass().getMethod("write", PacketByteBuf.class);
            method.invoke(target, buf);
            return true;
        } catch (Exception ignored) {
            return false;
        }
    }

    @Unique
    private static byte[] extractByteArray(Object target) {
        if (target == null) {
            return new byte[0];
        }
        for (Method method : target.getClass().getMethods()) {
            if (method.getParameterCount() != 0 || method.getReturnType() != byte[].class) {
                continue;
            }
            try {
                Object out = method.invoke(target);
                if (out instanceof byte[] bytes && bytes.length > 0) {
                    return bytes;
                }
            } catch (Exception ignored) {
                // Probe next candidate byte[] getter.
            }
        }
        return new byte[0];
    }

    @Unique
    private static void maybeLogValidationStats(int chunkX, int chunkZ, int payloadBytes) {
        if (!RustMC.CONFIG.isEnableChunkIngestValidation()) {
            return;
        }
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
            "[Rust-MC] Chunk ingest validation: chunk=({}, {}), payload={}B, attempts={}, forwards={}, failures={}, avgJNI={}us, nativePackets={}, nativeBytes={}B",
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