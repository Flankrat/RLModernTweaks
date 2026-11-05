package com.flankrat.rlmoderntweaks.util;

import com.flankrat.rlmoderntweaks.RLModernTweaks;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.registries.ForgeRegistries;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Matches items against a config list. Entries can be:
 * <ul>
 *     <li>{@code modid:item} - one item</li>
 *     <li>{@code modid:prefix*} - every item in that namespace whose path starts with prefix</li>
 *     <li>{@code #modid:tag} - every item in an item tag</li>
 * </ul>
 */
public final class ItemMatcher {

    public static final ItemMatcher EMPTY = new ItemMatcher(Set.of(), List.of(), List.of());

    private final Set<ResourceLocation> ids;
    private final List<String[]> prefixes;
    private final List<TagKey<Item>> tags;

    private ItemMatcher(Set<ResourceLocation> ids, List<String[]> prefixes, List<TagKey<Item>> tags) {
        this.ids = ids;
        this.prefixes = prefixes;
        this.tags = tags;
    }

    public static ItemMatcher of(List<? extends String> entries) {
        Set<ResourceLocation> ids = new HashSet<>();
        List<String[]> prefixes = new ArrayList<>();
        List<TagKey<Item>> tags = new ArrayList<>();

        for (String raw : entries) {
            String entry = raw.trim();
            if (entry.isEmpty()) continue;

            if (entry.startsWith("#")) {
                ResourceLocation tag = ResourceLocation.tryParse(entry.substring(1));
                if (tag != null) {
                    tags.add(TagKey.create(Registries.ITEM, tag));
                } else {
                    RLModernTweaks.LOGGER.warn("Ignoring invalid item tag in config: {}", entry);
                }
            } else if (entry.endsWith("*")) {
                String body = entry.substring(0, entry.length() - 1);
                int colon = body.indexOf(':');
                if (colon > 0) {
                    prefixes.add(new String[]{body.substring(0, colon), body.substring(colon + 1)});
                } else {
                    RLModernTweaks.LOGGER.warn("Ignoring invalid item prefix in config (needs modid:): {}", entry);
                }
            } else {
                ResourceLocation id = ResourceLocation.tryParse(entry);
                if (id != null) {
                    ids.add(id);
                } else {
                    RLModernTweaks.LOGGER.warn("Ignoring invalid item id in config: {}", entry);
                }
            }
        }
        return new ItemMatcher(Set.copyOf(ids), List.copyOf(prefixes), List.copyOf(tags));
    }

    public boolean test(ItemStack stack) {
        if (stack.isEmpty()) return false;

        for (TagKey<Item> tag : tags) {
            if (stack.is(tag)) return true;
        }

        if (ids.isEmpty() && prefixes.isEmpty()) return false;
        ResourceLocation id = ForgeRegistries.ITEMS.getKey(stack.getItem());
        if (id == null) return false;
        if (ids.contains(id)) return true;

        for (String[] prefix : prefixes) {
            if (id.getNamespace().equals(prefix[0]) && id.getPath().startsWith(prefix[1])) return true;
        }
        return false;
    }
}
