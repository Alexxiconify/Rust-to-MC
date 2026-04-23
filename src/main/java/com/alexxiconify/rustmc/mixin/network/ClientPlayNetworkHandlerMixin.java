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

// Preview chunk-ingest hook: forwards lightweight chunk metadata through JNI.
// Full decode replacement stays disabled until parity validation is complete.
@Mixin(ClientPlayNetworkHandler.class)
public class ClientPlayNetworkHandlerMixin {
    @Unique private static final long VALIDATION_LOG_INTERVAL_NS = 5_000_000_000L;
    @Unique private static final int SNAPSHOT_SAMPLE_MASK = 0x1F; // 1 out of 32 packets
    @Unique private static volatile boolean coordAccessResolved;
    @Unique private static volatile long lastValidationLogNs;
    @Unique private static final AtomicInteger ingestSequence = new AtomicInteger(0);
    @Unique private static Method chunkXMethod;
    @Unique private static Method chunkZMethod;
    @Unique private static volatile boolean payloadAccessResolved;
    @Unique private static Method packetBytesMethod;
    @Unique private static Method packetChunkDataMethod;
    @Unique private static Method chunkDataBytesMethod;
    @Unique private static Method chunkDataWriteMethod;

    @Inject(method = "onChunkData(Lnet/minecraft/network/packet/s2c/play/ChunkDataS2CPacket;)V", at = @At("HEAD"), require = 0)
    private void rustmcOnChunkData(ChunkDataS2CPacket packet, CallbackInfo ci) {
        if (!com.alexxiconify.rustmc.RustMC.CONFIG.isEnableChunkIngestOffload()) {
            return;
        }
        boolean validationEnabled = RustMC.CONFIG.isEnableChunkIngestValidation();
        boolean shouldSampleSnapshot = validationEnabled && ((ingestSequence.incrementAndGet() & SNAPSHOT_SAMPLE_MASK) == 0);
        // Preview mode guard: avoid per-packet JNI/alloc cost unless a sampled validation snapshot is requested.
        if (!shouldSampleSnapshot) {
            return;
        }
        int chunkX = resolveChunkCoord(packet, true);
        int chunkZ = resolveChunkCoord(packet, false);
        byte[] payload = snapshotPayload(packet, chunkX, chunkZ);
        if (payload.length <= 8) {
            return;
        }
        NativeBridge.processChunkData(payload, chunkX, chunkZ);
        maybeLogValidationStats(validationEnabled, chunkX, chunkZ, payload.length, shouldSampleSnapshot);
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
        if (!payloadAccessResolved) {
            resolvePayloadAccess(packet.getClass());
        }
        byte[] packetBytes = invokeByteArray(packetBytesMethod, packet);
        if (packetBytes.length > 0) {
            return packetBytes;
        }
        Object chunkData = invokeNoArg(packetChunkDataMethod, packet);
        byte[] chunkDataBytes = invokeByteArray(chunkDataBytesMethod, chunkData);
        if (chunkDataBytes.length > 0) {
            return chunkDataBytes;
        }
        PacketByteBuf buf = new PacketByteBuf(Unpooled.buffer(16_384));
        try {
            if (!invokeWriteWithPacketByteBuf(chunkDataWriteMethod, chunkData, buf)) {
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
    private static synchronized void resolvePayloadAccess(Class<?> packetClass) {
        if (payloadAccessResolved) {
            return;
        }
        packetBytesMethod = findNoArgByteArrayMethod(packetClass, "getChunkData", "getData", "getBytes");
        try {
            Method method = packetClass.getMethod("getChunkData");
            if (method.getParameterCount() == 0) {
                packetChunkDataMethod = method;
            }
        } catch (NoSuchMethodException ignored) {
            packetChunkDataMethod = null;
        }
        if (packetChunkDataMethod != null) {
            Class<?> chunkDataClass = packetChunkDataMethod.getReturnType();
            chunkDataBytesMethod = findNoArgByteArrayMethod(chunkDataClass, "getData", "getBytes");
            chunkDataWriteMethod = findWriteMethod(chunkDataClass);
        }
        payloadAccessResolved = true;
    }


    @Unique
    private static Method findNoArgByteArrayMethod(Class<?> type, String... names) {
        for (String name : names) {
            try {
                Method method = type.getMethod(name);
                if (method.getParameterCount() == 0 && method.getReturnType() == byte[].class) {
                    return method;
                }
            } catch (NoSuchMethodException ignored) {
                // Try next alias.
            }
        }
        return null;
    }

    @Unique
    private static Method findWriteMethod(Class<?> type) {
        try {
            Method method = type.getMethod("write", PacketByteBuf.class);
            if (method.getParameterCount() == 1) {
                return method;
            }
        } catch (NoSuchMethodException ignored) {
            // ChunkData may not expose a writable snapshot path.
        }
        return null;
    }

    @Unique
    private static Object invokeNoArg(Method method, Object target) {
        if (target == null || method == null) {
            return null;
        }
        try {
            return method.invoke(target);
        } catch (Exception ignored) {
            return null;
        }
    }

    @Unique
    private static byte[] invokeByteArray(Method method, Object target) {
        if (target == null || method == null) {
            return new byte[0];
        }
        try {
            Object out = method.invoke(target);
            if (out instanceof byte[] bytes && bytes.length > 0) {
                return bytes;
            }
        } catch (Exception ignored) {
            // Keep preview path fail-safe.
        }
        return new byte[0];
    }


    @Unique
    private static boolean invokeWriteWithPacketByteBuf(Method method, Object target, PacketByteBuf buf) {
        if (target == null || buf == null || method == null) {
            return false;
        }
        try {
            method.invoke(target, buf);
            return true;
        } catch (Exception ignored) {
            return false;
        }
    }

    @Unique
    private static void maybeLogValidationStats(boolean validationEnabled, int chunkX, int chunkZ, int payloadBytes, boolean sampledPayload) {
        if (!validationEnabled) {
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
            "[Rust-MC] Chunk ingest validation: chunk=({}, {}), payload={}B, sampledPayload={}, attempts={}, forwards={}, failures={}, avgJNI={}us, nativePackets={}, nativeBytes={}B",
            chunkX,
            chunkZ,
            payloadBytes,
            sampledPayload,
            javaStats[0],
            javaStats[1],
            javaStats[2],
            javaStats[3],
            nativePackets,
            nativeBytes
        );
    }
}