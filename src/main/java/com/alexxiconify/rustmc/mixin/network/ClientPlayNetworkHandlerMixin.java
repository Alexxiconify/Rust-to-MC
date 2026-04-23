package com.alexxiconify.rustmc.mixin.network;

import com.alexxiconify.rustmc.NativeBridge;
import net.minecraft.client.network.ClientPlayNetworkHandler;
import net.minecraft.network.packet.s2c.play.ChunkDataS2CPacket;
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
    @Unique private static volatile boolean coordAccessResolved;
    @Unique private static Method chunkXMethod;
    @Unique private static Method chunkZMethod;

    @Inject(method = "onChunkData(Lnet/minecraft/network/packet/s2c/play/ChunkDataS2CPacket;)V", at = @At("HEAD"), require = 0)
    private void rustmcOnChunkData(ChunkDataS2CPacket packet, CallbackInfo ci) {
        if (!com.alexxiconify.rustmc.RustMC.CONFIG.isEnableChunkIngestOffload()) {
            return;
        }
        int chunkX = resolveChunkCoord(packet, true);
        int chunkZ = resolveChunkCoord(packet, false);
        NativeBridge.processChunkData(coordPayload(chunkX, chunkZ), chunkX, chunkZ);
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
}