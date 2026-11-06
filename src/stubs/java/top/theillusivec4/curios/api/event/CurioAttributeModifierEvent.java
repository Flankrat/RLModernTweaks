package top.theillusivec4.curios.api.event;

import com.google.common.collect.Multimap;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.eventbus.api.Event;
import top.theillusivec4.curios.api.SlotContext;

import java.util.Collection;
import java.util.UUID;

/** Compile-only stub of the Curios API; the real class comes from Curios at runtime. */
public class CurioAttributeModifierEvent extends Event {
    public Multimap<Attribute, AttributeModifier> getModifiers() { throw new UnsupportedOperationException(); }
    public Multimap<Attribute, AttributeModifier> getOriginalModifiers() { throw new UnsupportedOperationException(); }
    public boolean addModifier(Attribute attribute, AttributeModifier modifier) { throw new UnsupportedOperationException(); }
    public Collection<AttributeModifier> removeAttribute(Attribute attribute) { throw new UnsupportedOperationException(); }
    public SlotContext getSlotContext() { throw new UnsupportedOperationException(); }
    public ItemStack getItemStack() { throw new UnsupportedOperationException(); }
    public UUID getUuid() { throw new UnsupportedOperationException(); }
}
