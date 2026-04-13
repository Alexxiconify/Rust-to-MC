package com.alexxiconify.rustmc.mixin;
import net.minecraft.block.AbstractBlock;
import net.minecraft.block.PointedDripstoneBlock;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
@Mixin(AbstractBlock.AbstractBlockState.class)
public abstract class BlockStateMixin {
    // Fixes the Vanilla Tweaks 3D Dripstone resource pack culling bug where
    // the inside of the Dripstone is visible because the connecting face is excessively culled.
    @Inject(method = "isOpaque", at = @At("HEAD"), cancellable = true)
    private void fixDripstoneCulling(CallbackInfoReturnable<Boolean> cir) {
        AbstractBlock.AbstractBlockState self = (AbstractBlock.AbstractBlockState) (Object) this;
        if (self.getBlock() instanceof PointedDripstoneBlock) {
            // Pointed Dripstone models from VanillaTweaks shouldn't be treated as
            // fully opaque blocks that aggressively cull adjacent faces.
            cir.setReturnValue(false);
        }
    }
}