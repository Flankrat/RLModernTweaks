package top.theillusivec4.curios.api;

import net.minecraft.world.entity.ai.attributes.Attribute;

/** Compile-only stub of the Curios API; the real class comes from Curios at runtime. */
public class SlotAttribute extends Attribute {
    protected SlotAttribute(String identifier) {
        super("curios.slot." + identifier, 0);
    }

    public static SlotAttribute getOrCreate(String identifier) {
        throw new UnsupportedOperationException();
    }
}
