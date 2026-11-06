package com.flankrat.rlmoderntweaks.feature;

import com.flankrat.rlmoderntweaks.RLModernTweaks;
import com.flankrat.rlmoderntweaks.config.RLMTConfig;
import net.minecraft.ChatFormatting;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.monster.Zombie;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraftforge.event.entity.EntityJoinLevelEvent;
import net.minecraftforge.event.entity.living.LivingDropsEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.registries.ForgeRegistries;

import java.util.ArrayList;
import java.util.List;

/**
 * Player bosses: a newly spawned zombie can rarely become one of the RLCraft player bosses,
 * with a player head, gear and more health. They always drop the configured rewards.
 * Port of playerbosses.js. Only zombies that spawn are rolled, not ones loaded with a chunk.
 */
public final class PlayerBosses {
    private static final String TAG = "rlmoderntweaks_player_boss";

    private record Boss(String name, double health, double attack, String mainhand, String chest, String skin) {}

    private PlayerBosses() {
    }

    private static List<Boss> bosses() {
        List<Boss> list = new ArrayList<>();
        for (String line : RLMTConfig.PLAYER_BOSSES.get()) {
            String[] p = line.split(";");
            if (p.length < 6) continue;
            try {
                list.add(new Boss(p[0].trim(), Double.parseDouble(p[1].trim()), Double.parseDouble(p[2].trim()),
                        p[3].trim(), p[4].trim(), p[5].trim()));
            } catch (NumberFormatException e) {
                RLModernTweaks.LOGGER.warn("Bad player_bosses entry \"{}\"", line);
            }
        }
        return list;
    }

    private static ItemStack stack(String id) {
        ResourceLocation key = ResourceLocation.tryParse(id);
        Item item = key == null ? null : ForgeRegistries.ITEMS.getValue(key);
        return item == null || item == Items.AIR ? ItemStack.EMPTY : new ItemStack(item);
    }

    @SubscribeEvent
    public static void onJoin(EntityJoinLevelEvent event) {
        if (event.getLevel().isClientSide() || event.loadedFromDisk()) return;
        if (event.getEntity().getType() != net.minecraft.world.entity.EntityType.ZOMBIE) return;
        if (!RLMTConfig.PLAYER_BOSSES_ENABLED.get()) return;
        Zombie zombie = (Zombie) event.getEntity();
        if (zombie.getPersistentData().getBoolean(TAG)) return;
        if (zombie.getRandom().nextDouble() >= RLMTConfig.PLAYER_BOSSES_CHANCE.get()) return;
        List<Boss> pool = bosses();
        if (pool.isEmpty()) return;
        Boss boss = pool.get(zombie.getRandom().nextInt(pool.size()));

        zombie.getPersistentData().putBoolean(TAG, true);
        zombie.setCustomName(Component.literal(boss.name()).withStyle(ChatFormatting.RED, ChatFormatting.BOLD));
        zombie.setCustomNameVisible(true);
        zombie.getAttribute(Attributes.MAX_HEALTH).setBaseValue(boss.health());
        zombie.setHealth((float) boss.health());
        zombie.getAttribute(Attributes.MOVEMENT_SPEED).setBaseValue(0.35);
        zombie.getAttribute(Attributes.ATTACK_DAMAGE).setBaseValue(boss.attack());

        zombie.setItemSlot(EquipmentSlot.MAINHAND, stack(boss.mainhand()));
        zombie.setItemSlot(EquipmentSlot.CHEST, stack(boss.chest()));
        ItemStack head = new ItemStack(Items.PLAYER_HEAD);
        head.getOrCreateTag().putString("SkullOwner", boss.skin());
        zombie.setItemSlot(EquipmentSlot.HEAD, head);
        zombie.setDropChance(EquipmentSlot.MAINHAND, 0.1F);
        zombie.setDropChance(EquipmentSlot.CHEST, 0.1F);
        zombie.setDropChance(EquipmentSlot.HEAD, 0.1F);
        zombie.setPersistenceRequired();
        RLModernTweaks.LOGGER.info("[Player Bosses] Spawned {} at {}", boss.name(), zombie.blockPosition());
    }

    private static boolean isBoss(Zombie zombie) {
        CompoundTag data = zombie.getPersistentData();
        if (data.getBoolean(TAG)) return true;
        // Bosses made by the old KubeJS script only have their name.
        if (!zombie.hasCustomName()) return false;
        String name = zombie.getCustomName().getString();
        for (Boss boss : bosses()) {
            if (name.contains(boss.name())) return true;
        }
        return false;
    }

    @SubscribeEvent
    public static void onDrops(LivingDropsEvent event) {
        if (!(event.getEntity() instanceof Zombie zombie) || zombie.level().isClientSide || !isBoss(zombie)) return;
        for (String line : RLMTConfig.PLAYER_BOSS_REWARDS.get()) {
            String[] p = line.trim().split("\\s+");
            int count = 1;
            String id = p[0];
            if (p.length > 1) {
                try {
                    count = Integer.parseInt(p[0]);
                } catch (NumberFormatException ignored) {
                }
                id = p[1];
            }
            ItemStack stack = stack(id);
            if (stack.isEmpty()) continue;
            stack.setCount(count);
            event.getDrops().add(new ItemEntity(zombie.level(), zombie.getX(), zombie.getY(), zombie.getZ(), stack));
        }
    }
}
