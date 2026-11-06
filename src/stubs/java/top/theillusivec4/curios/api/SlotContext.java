package top.theillusivec4.curios.api;

import net.minecraft.world.entity.LivingEntity;

/** Compile-only stub of the Curios API; the real class comes from Curios at runtime. */
public record SlotContext(String identifier, LivingEntity entity, int index, boolean cosmetic, boolean visible) {
}
