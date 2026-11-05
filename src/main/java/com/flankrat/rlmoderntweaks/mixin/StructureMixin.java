package com.flankrat.rlmoderntweaks.mixin;

import com.flankrat.rlmoderntweaks.feature.FlatGroundCheck;
import net.minecraft.world.level.levelgen.structure.Structure;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.Optional;

/**
 * Hooks the flat ground check into structure placement. findValidGenerationPoint is used both
 * when a structure is generated and by /locate, so both agree on where a structure can be.
 */
@Mixin(Structure.class)
public abstract class StructureMixin {

    @Inject(method = "findValidGenerationPoint", at = @At("RETURN"), cancellable = true, require = 1)
    private void rlmoderntweaks$flatGroundCheck(Structure.GenerationContext context,
                                                CallbackInfoReturnable<Optional<Structure.GenerationStub>> cir) {
        Optional<Structure.GenerationStub> stub = cir.getReturnValue();
        if (stub.isEmpty()) return;

        Structure self = (Structure) (Object) this;
        if (!FlatGroundCheck.applies(self, context)) return;

        if (!FlatGroundCheck.isFlatEnough(stub.get().getPiecesBuilder(), context)) {
            cir.setReturnValue(Optional.empty());
        }
    }
}
