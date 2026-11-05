package com.flankrat.rlmoderntweaks.mixin;

import com.flankrat.rlmoderntweaks.feature.NoWaterlogging;
import com.mojang.datafixers.util.Either;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.level.levelgen.structure.pools.SinglePoolElement;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructurePlaceSettings;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplate;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Lets listed jigsaw templates be placed without keeping the water that was already in the world.
 * Normally every stair, slab, fence, leaf block etc. placed where ocean water was becomes
 * waterlogged, which turns it into a water source inside the structure.
 */
@Mixin(SinglePoolElement.class)
public abstract class SinglePoolElementMixin {

    @Shadow
    @Final
    protected Either<ResourceLocation, StructureTemplate> template;

    @Inject(method = "getSettings", at = @At("RETURN"), require = 1)
    private void rlmoderntweaks$noWaterlogging(Rotation rotation, BoundingBox box, boolean keepJigsaws,
                                               CallbackInfoReturnable<StructurePlaceSettings> cir) {
        this.template.left().ifPresent(id -> {
            if (NoWaterlogging.applies(id)) cir.getReturnValue().setKeepLiquids(false);
        });
    }
}
