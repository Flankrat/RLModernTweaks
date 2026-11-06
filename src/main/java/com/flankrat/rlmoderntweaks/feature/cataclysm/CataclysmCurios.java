package com.flankrat.rlmoderntweaks.feature.cataclysm;

import com.flankrat.rlmoderntweaks.config.RLMTConfig;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.registries.ForgeRegistries;
import top.theillusivec4.curios.api.SlotAttribute;
import top.theillusivec4.curios.api.event.CurioAttributeModifierEvent;

import java.util.UUID;

/**
 * Cataclysm gives some of its curios their bonuses only in its own Curios slots. The pack puts
 * them in the shared ring, belt and charm slots, so this adds the bonuses there.
 * Port of cataclysm_curios_ring_attribute_compat.js. Only registered when Curios is installed.
 */
public final class CataclysmCurios {
    private CataclysmCurios() {
    }

    /** Same name and UUID as Cataclysm's own AttributeContainer.createModifier, so the modifiers match. */
    private static AttributeModifier cataclysmModifier(Attribute attribute, double amount, String name) {
        ResourceLocation key = ForgeRegistries.ATTRIBUTES.getKey(attribute);
        String path = key == null ? "unknown" : key.getPath();
        return new AttributeModifier(UUID.nameUUIDFromBytes(("cataclysm:" + name).getBytes()),
                String.format("%s_%s_modifier", name, path), amount, AttributeModifier.Operation.ADDITION);
    }

    @SubscribeEvent
    public static void onCurioAttributes(CurioAttributeModifierEvent event) {
        if (!RLMTConfig.CATACLYSM_CURIOS_ENABLED.get()) return;
        ResourceLocation itemKey = ForgeRegistries.ITEMS.getKey(event.getItemStack().getItem());
        if (itemKey == null) return;
        String item = itemKey.toString();
        String slot = event.getSlotContext().identifier();
        int index = event.getSlotContext().index();

        if ((item.equals("cataclysm:belt_of_beginner") || item.equals("cataclysm:belt_of_monstrosity")) && slot.equals("belt")) {
            // Both belts give talisman +2; move that to the shared charm slots.
            SlotAttribute charm = SlotAttribute.getOrCreate("charm");
            event.removeAttribute(SlotAttribute.getOrCreate("talisman"));
            if (!event.getModifiers().containsKey(charm)) {
                event.addModifier(charm, new AttributeModifier(event.getUuid(), "Cataclysm belt charm slots", 2.0,
                        AttributeModifier.Operation.ADDITION));
            }
        } else if (item.equals("cataclysm:ring_of_grudged") && slot.equals("ring")) {
            Attribute crit = ForgeRegistries.ATTRIBUTES.getValue(new ResourceLocation("cataclysm", "additional_critical_damage"));
            if (crit != null && !event.getOriginalModifiers().containsKey(crit)) {
                event.addModifier(crit, cataclysmModifier(crit, 10.0, "ring_" + index));
            }
        } else if (item.equals("cataclysm:unbreakable_skull") && slot.equals("charm")) {
            String name = "charm_" + index;
            event.addModifier(Attributes.ARMOR, cataclysmModifier(Attributes.ARMOR, 1.0, name));
            event.addModifier(Attributes.ARMOR_TOUGHNESS, cataclysmModifier(Attributes.ARMOR_TOUGHNESS, 1.0, name));
        }
    }
}
