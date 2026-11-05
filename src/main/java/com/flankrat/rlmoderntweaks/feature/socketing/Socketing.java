package com.flankrat.rlmoderntweaks.feature.socketing;

import com.flankrat.rlmoderntweaks.RLModernTweaks;
import com.flankrat.rlmoderntweaks.config.RLMTConfig;
import com.flankrat.rlmoderntweaks.init.ModItems;
import com.flankrat.rlmoderntweaks.network.KnownGemsPacket;
import com.flankrat.rlmoderntweaks.network.ModNetwork;
import net.minecraft.ChatFormatting;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.TagKey;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.event.ItemAttributeModifierEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.network.PacketDistributor;
import net.minecraftforge.registries.ForgeRegistries;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;

/**
 * Gem socketing, a stand-in until fonnymunkey's Socketed (FermiumModding) is ported from 1.12.2.
 * Port of socketed_gems_startup.js, socketed_logic_server.js and socketed_tooltips_client.js.
 * <ul>
 *     <li>Only loot gear (chests, mob drops, fishing) rolls sockets, see {@link SocketLootModifier}.
 *         Crafted gear never has the "Sockets" tag, so it can never be socketed.</li>
 *     <li>Gem in main hand + gear in off hand + right-click: socket the gem into the next empty slot.</li>
 *     <li>Socket Extraction Tool in main hand + gear in off hand + right-click: take out the last
 *         gem; each gem has its own chance to break instead.</li>
 *     <li>A gem's effect is hidden on its tooltip until the player has socketed one of that type.</li>
 *     <li>The bonus is an attribute modifier on the gear, in the slot the gear is normally used in.</li>
 * </ul>
 * Item NBT is the same as the scripts used (Sockets, SocketsUsed, SocketedGem_N, SocketedGemLabel_N),
 * so gear socketed by the scripts keeps working.
 */
public final class Socketing {

    public static final String TAG_SOCKETS = "Sockets";
    public static final String TAG_USED = "SocketsUsed";
    public static final String TAG_GEM = "SocketedGem_";
    public static final String TAG_LABEL = "SocketedGemLabel_";

    /** Player persistent data key holding the gems this player has discovered. */
    private static final String KNOWN_KEY = RLModernTweaks.MODID + ":known_gems";

    public record GemBonus(ResourceLocation attribute, double amount, AttributeModifier.Operation operation,
                           String label, double extractFail) {
    }

    public record Tier(ChatFormatting color, List<String> names) {
    }

    private record Roll(int sockets, int weight) {
    }

    private static volatile Map<ResourceLocation, GemBonus> gems = Map.of();
    private static volatile GemBonus defaultBonus =
            new GemBonus(new ResourceLocation("minecraft:generic.knockback_resistance"), 0.1,
                    AttributeModifier.Operation.ADDITION, "+10% Knockback Resistance", 0.5);
    private static volatile List<Roll> rolls = List.of();
    private static volatile Map<Integer, Tier> tiers = Map.of();
    private static volatile TagKey<Item> gemTag = TagKey.create(Registries.ITEM, new ResourceLocation("forge", "gems"));

    private Socketing() {
    }

    // ---- Config -------------------------------------------------------------------------------

    public static void refresh() {
        Map<ResourceLocation, GemBonus> newGems = new HashMap<>();
        for (String entry : RLMTConfig.SOCKET_GEMS.get()) {
            String[] parts = entry.split(";", 2);
            if (parts.length < 2) {
                warn("gem", entry);
                continue;
            }
            ResourceLocation item = ResourceLocation.tryParse(parts[0].trim());
            GemBonus bonus = parseBonus(parts[1]);
            if (item == null || bonus == null) warn("gem", entry);
            else newGems.put(item, bonus);
        }
        gems = Map.copyOf(newGems);

        GemBonus parsedDefault = parseBonus(RLMTConfig.SOCKET_DEFAULT_BONUS.get());
        if (parsedDefault != null) defaultBonus = parsedDefault;
        else warn("defaultBonus", RLMTConfig.SOCKET_DEFAULT_BONUS.get());

        List<Roll> newRolls = new ArrayList<>();
        for (String entry : RLMTConfig.SOCKET_ROLLS.get()) {
            String[] parts = entry.split("=");
            try {
                newRolls.add(new Roll(Integer.parseInt(parts[0].trim()), Integer.parseInt(parts[1].trim())));
            } catch (RuntimeException e) {
                warn("socketRolls", entry);
            }
        }
        rolls = List.copyOf(newRolls);

        Map<Integer, Tier> newTiers = new HashMap<>();
        for (String entry : RLMTConfig.SOCKET_TIERS.get()) {
            String[] parts = entry.split(";");
            try {
                int count = Integer.parseInt(parts[0].trim());
                ChatFormatting color = ChatFormatting.getByName(parts[1].trim());
                List<String> names = new ArrayList<>();
                for (String name : parts[2].split(",")) {
                    if (!name.isBlank()) names.add(name.trim());
                }
                if (color == null || !color.isColor() || names.isEmpty()) throw new IllegalArgumentException();
                newTiers.put(count, new Tier(color, List.copyOf(names)));
            } catch (RuntimeException e) {
                warn("tiers", entry);
            }
        }
        tiers = Map.copyOf(newTiers);

        ResourceLocation tagId = ResourceLocation.tryParse(RLMTConfig.SOCKET_GEM_TAG.get().replace("#", "").trim());
        if (tagId != null) gemTag = TagKey.create(Registries.ITEM, tagId);
    }

    /** Parses "attribute;amount;OPERATION;label;extractFail". */
    private static GemBonus parseBonus(String text) {
        String[] parts = text.split(";");
        if (parts.length != 5) return null;
        try {
            ResourceLocation attribute = ResourceLocation.tryParse(parts[0].trim());
            if (attribute == null) return null;
            return new GemBonus(attribute, Double.parseDouble(parts[1].trim()),
                    AttributeModifier.Operation.valueOf(parts[2].trim().toUpperCase()),
                    parts[3].trim(), Double.parseDouble(parts[4].trim()));
        } catch (RuntimeException e) {
            return null;
        }
    }

    private static void warn(String option, String entry) {
        RLModernTweaks.LOGGER.warn("[Socketing] Ignoring invalid {} entry in config: \"{}\"", option, entry);
    }

    // ---- Shared helpers (also used by the loot modifier and the client tooltips) -------------

    public static boolean enabled() {
        return RLMTConfig.SOCKETING_ENABLED.get();
    }

    public static int maxSocketsAllowed() {
        return RLMTConfig.SOCKET_MAX.get();
    }

    public static GemBonus bonusFor(ResourceLocation gemId) {
        GemBonus bonus = gems.get(gemId);
        return bonus != null ? bonus : defaultBonus;
    }

    public static boolean isGem(ItemStack stack) {
        if (stack.isEmpty() || !stack.is(gemTag)) return false;
        ResourceLocation id = ForgeRegistries.ITEMS.getKey(stack.getItem());
        // KubeJS items were excluded by the script (they were its own tool items).
        return id != null && !id.getNamespace().equals("kubejs");
    }

    public static boolean isExtractor(ItemStack stack) {
        return stack.is(ModItems.SOCKET_EXTRACTOR.get());
    }

    /** Max sockets on a piece of gear (0 = not socketable). SocketsUsed counts too, for old gear. */
    public static int maxSockets(CompoundTag tag) {
        if (tag == null) return 0;
        return Math.min(maxSocketsAllowed(), Math.max(tag.getInt(TAG_SOCKETS), tag.getInt(TAG_USED)));
    }

    /** Tier for a socket count; falls back to the closest lower tier that is defined. */
    public static Tier tierFor(int sockets) {
        Map<Integer, Tier> current = tiers;
        for (int count = sockets; count > 0; count--) {
            Tier tier = current.get(count);
            if (tier != null) return tier;
        }
        return new Tier(ChatFormatting.AQUA, List.of("Socketed"));
    }

    public static int rollSocketCount() {
        List<Roll> current = rolls;
        int total = 0;
        for (Roll roll : current) total += Math.max(0, roll.weight());
        if (total <= 0) return 0;
        int pick = ThreadLocalRandom.current().nextInt(total);
        for (Roll roll : current) {
            pick -= Math.max(0, roll.weight());
            if (pick < 0) return Math.min(roll.sockets(), maxSocketsAllowed());
        }
        return 0;
    }

    // ---- Socketing and extraction ---------------------------------------------------------------

    @SubscribeEvent
    public static void onRightClick(PlayerInteractEvent.RightClickItem event) {
        if (!enabled() || event.getHand() != InteractionHand.MAIN_HAND) return;
        Player player = event.getEntity();
        ItemStack main = event.getItemStack();
        ItemStack gear = player.getOffhandItem();
        if (gear.isEmpty() || !gear.isDamageableItem()) return;

        boolean gem = isGem(main);
        boolean extractor = isExtractor(main);
        if (!gem && !extractor) return;

        event.setCanceled(true);
        event.setCancellationResult(InteractionResult.SUCCESS);
        if (player.level().isClientSide()) return;

        int max = maxSockets(gear.getTag());
        if (max <= 0) {
            tell(player, "This item has no sockets. Only gear found as loot can have sockets.", ChatFormatting.RED);
            return;
        }
        if (gem) socketGem(player, main, gear, max);
        else extractGem(player, gear);
    }

    private static void socketGem(Player player, ItemStack gemStack, ItemStack gear, int max) {
        CompoundTag nbt = gear.getOrCreateTag();
        int used = nbt.getInt(TAG_USED);
        if (used >= max) {
            tell(player, "All sockets on this item are already filled!", ChatFormatting.RED);
            return;
        }
        ResourceLocation gemId = ForgeRegistries.ITEMS.getKey(gemStack.getItem());
        GemBonus bonus = bonusFor(gemId);
        int slot = used + 1;
        nbt.putInt(TAG_USED, slot);
        nbt.putString(TAG_GEM + slot, gemId.toString());
        nbt.putString(TAG_LABEL + slot, bonus.label());
        if (!player.isCreative()) gemStack.shrink(1);

        boolean learned = learn(player, gemId);
        tell(player, "Socketed into slot " + slot + ": " + bonus.label() + (learned ? " (effect discovered!)" : ""),
                ChatFormatting.GREEN);
    }

    private static void extractGem(Player player, ItemStack gear) {
        CompoundTag nbt = gear.getOrCreateTag();
        int used = nbt.getInt(TAG_USED);
        if (used <= 0) {
            tell(player, "There are no gems to extract on this item!", ChatFormatting.RED);
            return;
        }
        String gemText = nbt.getString(TAG_GEM + used);
        nbt.remove(TAG_GEM + used);
        nbt.remove(TAG_LABEL + used);
        if (used - 1 > 0) nbt.putInt(TAG_USED, used - 1);
        else nbt.remove(TAG_USED);

        // Always the last filled socket; each gem has its own chance to break.
        ResourceLocation gemId = ResourceLocation.tryParse(gemText);
        Item gemItem = gemId == null ? null : ForgeRegistries.ITEMS.getValue(gemId);
        double failChance = gemId == null ? 1.0 : bonusFor(gemId).extractFail();
        if (gemItem != null && gemItem != net.minecraft.world.item.Items.AIR
                && ThreadLocalRandom.current().nextDouble() >= failChance) {
            ItemStack returned = new ItemStack(gemItem);
            if (!player.getInventory().add(returned)) player.drop(returned, false);
            tell(player, "The gem came out intact!", ChatFormatting.YELLOW);
        } else {
            tell(player, "The gem shattered during extraction!", ChatFormatting.RED);
        }
    }

    private static void tell(Player player, String message, ChatFormatting color) {
        player.sendSystemMessage(Component.literal(message).withStyle(color));
    }

    // ---- Gem discovery ------------------------------------------------------------------------

    private static ListTag knownList(Player player) {
        return player.getPersistentData().getCompound(Player.PERSISTED_NBT_TAG).getList(KNOWN_KEY, Tag.TAG_STRING);
    }

    public static Set<String> knownGems(Player player) {
        Set<String> known = new LinkedHashSet<>();
        ListTag list = knownList(player);
        for (int i = 0; i < list.size(); i++) known.add(list.getString(i));
        return known;
    }

    /** Marks a gem as discovered. Returns true if it wasn't known yet. */
    private static boolean learn(Player player, ResourceLocation gemId) {
        Set<String> known = knownGems(player);
        if (!known.add(gemId.toString())) return false;
        saveKnown(player, known);
        sync(player);
        return true;
    }

    private static void saveKnown(Player player, Set<String> known) {
        ListTag list = new ListTag();
        for (String id : known) list.add(StringTag.valueOf(id));
        CompoundTag root = player.getPersistentData();
        CompoundTag persisted = root.getCompound(Player.PERSISTED_NBT_TAG);
        persisted.put(KNOWN_KEY, list);
        root.put(Player.PERSISTED_NBT_TAG, persisted);
    }

    private static void sync(Player player) {
        if (player instanceof ServerPlayer serverPlayer) {
            ModNetwork.CHANNEL.send(PacketDistributor.PLAYER.with(() -> serverPlayer),
                    new KnownGemsPacket(List.copyOf(knownGems(player))));
        }
    }

    @SubscribeEvent
    public static void onLogin(PlayerEvent.PlayerLoggedInEvent event) {
        Player player = event.getEntity();
        migrateKubeJSStages(player);
        sync(player);
    }

    /**
     * The script stored discovered gems as KubeJS stages ("socket_known_minecraft_diamond"),
     * which KubeJS keeps as entity tags. Carry them over so players don't lose what they found.
     */
    private static void migrateKubeJSStages(Player player) {
        Set<String> stages = player.getTags();
        if (stages.stream().noneMatch(stage -> stage.startsWith("socket_known_"))) return;
        Set<String> known = knownGems(player);
        boolean changed = false;
        for (var holder : ForgeRegistries.ITEMS.tags().getTag(gemTag)) {
            ResourceLocation id = ForgeRegistries.ITEMS.getKey(holder);
            if (id == null) continue;
            String stage = "socket_known_" + id.toString().replaceAll("[^a-z0-9_]", "_");
            if (stages.contains(stage)) {
                changed |= known.add(id.toString());
                player.removeTag(stage);
            }
        }
        if (changed) saveKnown(player, known);
    }

    // ---- Attribute bonuses ----------------------------------------------------------------------

    @SubscribeEvent
    public static void onAttributeModifiers(ItemAttributeModifierEvent event) {
        if (!enabled()) return;
        ItemStack stack = event.getItemStack();
        CompoundTag tag = stack.getTag();
        if (tag == null) return;
        int used = Math.min(tag.getInt(TAG_USED), maxSocketsAllowed());
        if (used <= 0) return;

        // Only in the slot the item is normally used in.
        EquipmentSlot slot = event.getSlotType();
        if (slot != LivingEntity.getEquipmentSlotForItem(stack)) return;

        for (int i = 1; i <= used; i++) {
            ResourceLocation gemId = ResourceLocation.tryParse(tag.getString(TAG_GEM + i));
            if (gemId == null || tag.getString(TAG_GEM + i).isEmpty()) continue;
            GemBonus bonus = bonusFor(gemId);
            Attribute attribute = ForgeRegistries.ATTRIBUTES.getValue(bonus.attribute());
            if (attribute == null) continue;
            // Same UUIDs as the script used: stable per socket index + slot, so bonuses never stack.
            UUID uuid = UUID.nameUUIDFromBytes(("socketing:" + i + ":" + slot.getName()).getBytes(StandardCharsets.UTF_8));
            event.addModifier(attribute, new AttributeModifier(uuid, "Gem Socket Slot " + i, bonus.amount(), bonus.operation()));
        }
    }
}
