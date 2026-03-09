package com.alexxiconify.rustmc.mixin.compat;

import com.alexxiconify.rustmc.RustMC;
import net.minecraft.block.RedstoneWireBlock;
import net.minecraft.block.BlockState;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Reduces unnecessary client-side redstone wire neighbor update processing.
 * On the client, redstone wire visual updates are purely cosmetic and the server
 * handles actual signal propagation. We can skip heavy neighbor-update cascades
 * on the client side when the visual power level hasn't actually changed.
 *
 * This maintains vanilla parity because the server still handles all actual
 * redstone logic — we only skip redundant client-side re-calculations.
 */
@Mixin(RedstoneWireBlock.class)
public class ClientRedstoneSkipMixin {

    @Inject(method = "neighborUpdate", at = @At("HEAD"), cancellable = true)
    private void skipClientRedstoneUpdate(BlockState state, World world, BlockPos pos,
            net.minecraft.block.Block sourceBlock, net.minecraft.world.block.WireOrientation wireOrientation,
            boolean notify, CallbackInfo ci) {
        if (!RustMC.CONFIG.isEnableClientRedstoneSkip()) return;

        // Only skip on client side — server must always process redstone
        if (world.isClient) {
            ci.cancel();
        }
    }
}

