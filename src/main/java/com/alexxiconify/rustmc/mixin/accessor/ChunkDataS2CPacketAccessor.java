package com.alexxiconify.rustmc.mixin.accessor;

import net.minecraft.network.packet.s2c.play.ChunkDataS2CPacket;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(ChunkDataS2CPacket.class)
public interface ChunkDataS2CPacketAccessor {
    @Accessor("chunkX")
    int getX();

    @Accessor("chunkZ")
    int getZ();

    @Accessor("chunkData")
    Object getChunkData();
}
