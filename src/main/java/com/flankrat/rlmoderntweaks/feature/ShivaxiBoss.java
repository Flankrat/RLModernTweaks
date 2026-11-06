package com.flankrat.rlmoderntweaks.feature;

import com.flankrat.rlmoderntweaks.RLModernTweaks;
import com.flankrat.rlmoderntweaks.config.RLMTConfig;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerBossEvent;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.BossEvent;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.MeleeAttackGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.ai.goal.WaterAvoidingRandomStrollGoal;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.monster.Giant;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.level.levelgen.structure.Structure;
import net.minecraftforge.event.entity.EntityJoinLevelEvent;
import net.minecraftforge.event.entity.EntityLeaveLevelEvent;
import net.minecraftforge.event.entity.living.LivingDeathEvent;
import net.minecraftforge.event.entity.living.LivingDropsEvent;
import net.minecraftforge.event.entity.living.LivingEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.registries.ForgeRegistries;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.WeakHashMap;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Shivaxi: the giant in the Shivaxi monument becomes a boss with a boss bar, gear and AI
 * (vanilla giants have no AI). When it dies the second phase (Ice and Fire's Shivaxi Dragon)
 * spawns and it always drops the reward (LOCK's Master Key).
 * Port of ShivaxiBoss.js and ShivaxiBodyMasterKeyDrop.js.
 * <p>
 * Unlike the script, only giants inside the configured structures become Shivaxi, so the
 * giant in Ice City stays a plain giant. A giant is checked once, the first time it's loaded;
 * giants the old script already turned into Shivaxi (they have the "shivaxi_saved_health"
 * marker) stay Shivaxi.
 */
public final class ShivaxiBoss {
    /** Same markers the KubeJS script used, so bosses from before keep working. */
    private static final String MARKER = "shivaxi_saved_health";
    private static final String CHECKED = "rlmoderntweaks_shivaxi_checked";
    private static final String DRAGON_SPAWNED = "shivaxi_dragon_spawned";

    private static final Map<UUID, ServerBossEvent> BARS = new ConcurrentHashMap<>();
    /** Giant objects that already got their AI goals (goals aren't saved, so every load adds them again). */
    private static final Set<Giant> WITH_AI = Collections.newSetFromMap(new WeakHashMap<>());

    private ShivaxiBoss() {
    }

    @SubscribeEvent
    public static void onJoin(EntityJoinLevelEvent event) {
        if (!(event.getEntity() instanceof Giant giant) || !(event.getLevel() instanceof ServerLevel level)) return;
        if (!RLMTConfig.SHIVAXI_ENABLED.get()) return;

        CompoundTag data = giant.getPersistentData();
        if (!data.contains(MARKER)) {
            if (data.getBoolean(CHECKED)) return;
            data.putBoolean(CHECKED, true);
            if (!isInShivaxiStructure(level, giant.blockPosition())) return;
            initialize(giant);
        }

        giant.getAttribute(Attributes.ATTACK_DAMAGE).setBaseValue(RLMTConfig.SHIVAXI_DAMAGE.get());
        giant.getAttribute(Attributes.MAX_HEALTH).setBaseValue(RLMTConfig.SHIVAXI_HEALTH.get());
        float saved = data.getFloat(MARKER);
        if (saved > 0) giant.setHealth(saved);

        synchronized (WITH_AI) {
            if (WITH_AI.add(giant)) {
                giant.goalSelector.addGoal(2, new MeleeAttackGoal(giant, 1.0, false));
                giant.targetSelector.addGoal(2, new NearestAttackableTargetGoal<>(giant, Player.class, true));
                giant.goalSelector.addGoal(7, new WaterAvoidingRandomStrollGoal(giant, 1.0));
                giant.goalSelector.addGoal(8, new LookAtPlayerGoal(giant, Player.class, 16.0F));
                giant.goalSelector.addGoal(8, new RandomLookAroundGoal(giant));
                giant.setAggressive(true);
            }
        }
    }

    private static boolean isInShivaxiStructure(ServerLevel level, BlockPos pos) {
        var registry = level.registryAccess().registryOrThrow(Registries.STRUCTURE);
        for (String id : RLMTConfig.SHIVAXI_STRUCTURES.get()) {
            ResourceLocation key = ResourceLocation.tryParse(id);
            Structure structure = key == null ? null : registry.get(key);
            if (structure != null && level.structureManager().getStructureWithPieceAt(pos, structure).isValid()) return true;
        }
        return false;
    }

    private static void initialize(Giant giant) {
        float health = RLMTConfig.SHIVAXI_HEALTH.get().floatValue();
        giant.getPersistentData().putFloat(MARKER, health);
        giant.addTag("shivaxi_boss");
        giant.setPersistenceRequired();
        giant.setCustomName(Component.literal("Shivaxi"));
        giant.setCustomNameVisible(true);

        ItemStack sword = new ItemStack(Items.DIAMOND_SWORD);
        sword.enchant(Enchantments.SHARPNESS, 5);
        sword.enchant(Enchantments.FIRE_ASPECT, 2);
        sword.enchant(Enchantments.KNOCKBACK, 2);
        giant.setItemSlot(EquipmentSlot.MAINHAND, sword);
        giant.setDropChance(EquipmentSlot.MAINHAND, 1.0F);
        giant.setItemSlot(EquipmentSlot.OFFHAND, new ItemStack(Items.SHIELD));
        giant.setItemSlot(EquipmentSlot.HEAD, new ItemStack(Items.DIAMOND_HELMET));
        giant.setItemSlot(EquipmentSlot.CHEST, new ItemStack(Items.DIAMOND_CHESTPLATE));
        giant.setItemSlot(EquipmentSlot.LEGS, new ItemStack(Items.DIAMOND_LEGGINGS));
        giant.setItemSlot(EquipmentSlot.FEET, new ItemStack(Items.DIAMOND_BOOTS));
        giant.getAttribute(Attributes.MAX_HEALTH).setBaseValue(health);
        giant.setHealth(health);
        RLModernTweaks.LOGGER.info("Shivaxi awakened at {}", giant.blockPosition());
    }

    private static boolean isShivaxi(Mob mob) {
        return mob instanceof Giant && mob.getPersistentData().contains(MARKER);
    }

    /** Boss bar and saved health, once a second. */
    @SubscribeEvent
    public static void onTick(LivingEvent.LivingTickEvent event) {
        if (!(event.getEntity() instanceof Giant giant) || giant.level().isClientSide || giant.tickCount % 20 != 0) return;
        if (!isShivaxi(giant)) return;

        giant.getPersistentData().putFloat(MARKER, giant.getHealth());
        ServerBossEvent bar = BARS.computeIfAbsent(giant.getUUID(), id ->
                new ServerBossEvent(giant.getDisplayName(), BossEvent.BossBarColor.RED, BossEvent.BossBarOverlay.PROGRESS));
        bar.setProgress(Math.max(0.0F, Math.min(1.0F, giant.getHealth() / giant.getMaxHealth())));

        double range = RLMTConfig.SHIVAXI_BAR_RANGE.get();
        for (ServerPlayer player : List.copyOf(bar.getPlayers())) {
            if (!player.isAlive() || player.level() != giant.level() || player.distanceTo(giant) >= range) bar.removePlayer(player);
        }
        for (Player player : giant.level().players()) {
            if (player instanceof ServerPlayer serverPlayer && player.isAlive() && player.distanceTo(giant) < range
                    && !bar.getPlayers().contains(serverPlayer)) {
                bar.addPlayer(serverPlayer);
            }
        }
    }

    @SubscribeEvent
    public static void onLeave(EntityLeaveLevelEvent event) {
        if (event.getEntity() instanceof Giant) removeBar(event.getEntity().getUUID());
    }

    private static void removeBar(UUID id) {
        ServerBossEvent bar = BARS.remove(id);
        if (bar != null) bar.removeAllPlayers();
    }

    /** Second phase. */
    @SubscribeEvent
    public static void onDeath(LivingDeathEvent event) {
        if (!(event.getEntity() instanceof Giant giant) || !(giant.level() instanceof ServerLevel level) || !isShivaxi(giant)) return;
        removeBar(giant.getUUID());
        CompoundTag data = giant.getPersistentData();
        if (data.getBoolean(DRAGON_SPAWNED)) return;
        data.putBoolean(DRAGON_SPAWNED, true);

        ResourceLocation id = ResourceLocation.tryParse(RLMTConfig.SHIVAXI_PHASE_TWO.get());
        EntityType<?> type = id == null ? null : ForgeRegistries.ENTITY_TYPES.getValue(id);
        if (type == null || id.equals(ForgeRegistries.ENTITY_TYPES.getDefaultKey())) return;
        var entity = type.create(level);
        if (entity == null) return;
        entity.moveTo(giant.getX(), giant.getY() + 1.0, giant.getZ(), giant.getYRot(), 0.0F);
        if (entity instanceof Mob mob) {
            mob.finalizeSpawn(level, level.getCurrentDifficultyAt(mob.blockPosition()), MobSpawnType.EVENT, null, null);
        }
        level.addFreshEntity(entity);
    }

    /** The guaranteed reward; Looting doesn't change it. */
    @SubscribeEvent
    public static void onDrops(LivingDropsEvent event) {
        if (!(event.getEntity() instanceof Giant giant) || !isShivaxi(giant)) return;
        String[] parts = RLMTConfig.SHIVAXI_REWARD.get().trim().split("\\s+");
        if (parts.length == 0 || parts[0].isEmpty()) return;
        int count = 1;
        String itemId = parts[0];
        if (parts.length > 1) {
            try {
                count = Integer.parseInt(parts[0]);
            } catch (NumberFormatException ignored) {
            }
            itemId = parts[1];
        }
        ResourceLocation key = ResourceLocation.tryParse(itemId);
        Item item = key == null ? null : ForgeRegistries.ITEMS.getValue(key);
        if (item == null || item == Items.AIR) return;
        event.getDrops().add(new ItemEntity(giant.level(), giant.getX(), giant.getY(), giant.getZ(), new ItemStack(item, count)));
    }
}
