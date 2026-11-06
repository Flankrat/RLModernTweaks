package com.flankrat.rlmoderntweaks.feature.items;

import com.flankrat.rlmoderntweaks.config.RLMTConfig;
import net.minecraft.ChatFormatting;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.SwordItem;
import net.minecraft.world.item.Tiers;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.living.LivingDeathEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.UUID;

/**
 * Blade of Herobrine: an unbreakable golden sword. Mobs killed while holding it give its
 * wielder a soul; right-click spends souls on a shockwave that moves forward in 5 steps,
 * getting wider and weaker, and burns and knocks back what it hits.
 * Port of the three blade_of_herobrine.js scripts (item, ability, tooltip).
 * <p>
 * Souls are kept in the player's data under "bladeCharges", like the script did.
 */
public class BladeOfHerobrineItem extends SwordItem {
    private static final String SOULS = "bladeCharges";
    private static final int STEPS = 5;
    private static final List<Wave> WAVES = new ArrayList<>();

    public BladeOfHerobrineItem(Properties properties) {
        super(Tiers.GOLD, 9, -2.4F, properties);
    }

    @Override
    public boolean isDamageable(ItemStack stack) {
        return false;
    }

    @Override
    public Component getName(ItemStack stack) {
        return super.getName(stack).copy().withStyle(ChatFormatting.RED);
    }

    @Override
    public void appendHoverText(ItemStack stack, @Nullable Level level, List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.empty());
        tooltip.add(Component.literal("★ LEGENDARY WEAPON ★").withStyle(ChatFormatting.GOLD, ChatFormatting.BOLD));
        tooltip.add(Component.literal("Forged from the calcified remains of an ancient wither,").withStyle(ChatFormatting.DARK_PURPLE, ChatFormatting.ITALIC));
        tooltip.add(Component.literal("this blade hungers for the life force of your foes.").withStyle(ChatFormatting.DARK_PURPLE, ChatFormatting.ITALIC));
        tooltip.add(Component.empty());
        tooltip.add(Component.literal("Right-Click").withStyle(ChatFormatting.YELLOW)
                .append(Component.literal(": Releases a sweeping unholy shockwave.").withStyle(ChatFormatting.GRAY)));
        int cost = RLMTConfig.HEROBRINE_SOUL_COST.get();
        tooltip.add(Component.literal("Ability Cost").withStyle(ChatFormatting.AQUA)
                .append(Component.literal(": " + cost + (cost == 1 ? " Soul." : " Souls.")).withStyle(ChatFormatting.GRAY)));
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (!RLMTConfig.HEROBRINE_ENABLED.get()) return InteractionResultHolder.pass(stack);
        if (level.isClientSide || !(player instanceof ServerPlayer serverPlayer)) return InteractionResultHolder.success(stack);

        int max = RLMTConfig.HEROBRINE_MAX_SOULS.get();
        int cost = RLMTConfig.HEROBRINE_SOUL_COST.get();
        int souls = player.getPersistentData().getInt(SOULS);
        if (souls < cost) {
            player.displayClientMessage(Component.literal("Not enough essence! Charges: " + souls + "/" + max).withStyle(ChatFormatting.RED), true);
            return InteractionResultHolder.fail(stack);
        }
        souls -= cost;
        player.getPersistentData().putInt(SOULS, souls);
        player.displayClientMessage(Component.literal("Charges remaining: " + souls + "/" + max).withStyle(ChatFormatting.GREEN), true);

        level.playSound(null, player.getX(), player.getY(), player.getZ(), SoundEvents.EVOKER_CAST_SPELL, SoundSource.PLAYERS, 1.0F, 0.8F);
        level.playSound(null, player.getX(), player.getY(), player.getZ(), SoundEvents.WITHER_SHOOT, SoundSource.PLAYERS, 0.8F, 1.2F);

        Vec3 look = player.getLookAngle().normalize();
        double horizontal = Math.sqrt(look.x * look.x + look.z * look.z);
        double hx = horizontal > 0 ? look.x / horizontal : 0;
        double hz = horizontal > 0 ? look.z / horizontal : 1;
        synchronized (WAVES) {
            WAVES.add(new Wave(serverPlayer.getUUID(), level.dimension(), player.position(), look, -hz, hx));
        }
        player.getCooldowns().addCooldown(this, RLMTConfig.HEROBRINE_COOLDOWN.get());
        return InteractionResultHolder.consume(stack);
    }

    /** A kill while holding the blade in either hand gives a soul. */
    @SubscribeEvent
    public static void onKill(LivingDeathEvent event) {
        if (!RLMTConfig.HEROBRINE_ENABLED.get()) return;
        if (!(event.getSource().getEntity() instanceof ServerPlayer player) || event.getEntity() == player) return;
        if (!(player.getMainHandItem().getItem() instanceof BladeOfHerobrineItem)
                && !(player.getOffhandItem().getItem() instanceof BladeOfHerobrineItem)) return;

        int max = RLMTConfig.HEROBRINE_MAX_SOULS.get();
        int souls = player.getPersistentData().getInt(SOULS);
        if (souls < max) {
            souls++;
            player.getPersistentData().putInt(SOULS, souls);
            player.displayClientMessage(Component.literal("Soul absorbed! (" + souls + "/" + max + ")").withStyle(ChatFormatting.DARK_AQUA), true);
        } else {
            player.displayClientMessage(Component.literal("Soul pool full! (" + max + "/" + max + ")").withStyle(ChatFormatting.YELLOW), true);
        }
    }

    /** Moves every active shockwave one step every other tick. */
    @SubscribeEvent
    public static void onServerTick(TickEvent.ServerTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;
        synchronized (WAVES) {
            if (WAVES.isEmpty()) return;
            Iterator<Wave> it = WAVES.iterator();
            while (it.hasNext()) {
                if (!it.next().tick(event.getServer())) it.remove();
            }
        }
    }

    private static final class Wave {
        final UUID caster;
        final ResourceKey<Level> dimension;
        final Vec3 origin;
        final Vec3 look;
        final double sideX;
        final double sideZ;
        final Set<UUID> hit = new HashSet<>();
        int step = 1;
        int ticks;

        Wave(UUID caster, ResourceKey<Level> dimension, Vec3 origin, Vec3 look, double sideX, double sideZ) {
            this.caster = caster;
            this.dimension = dimension;
            this.origin = origin;
            this.look = look;
            this.sideX = sideX;
            this.sideZ = sideZ;
        }

        /** Returns false once the wave is finished or can't continue. */
        boolean tick(MinecraftServer server) {
            if (++ticks < 2) return true;
            ticks = 0;
            ServerLevel level = server.getLevel(dimension);
            ServerPlayer player = server.getPlayerList().getPlayer(caster);
            if (level == null || player == null) return false;

            double distance = step * 3.0;
            double cx = origin.x + look.x * distance;
            double cy = origin.y + 0.2 + look.y * distance;
            double cz = origin.z + look.z * distance;
            double width = 1.0 + step * 0.8;

            for (int i = -5; i <= 5; i++) {
                double offset = i / 5.0 * width;
                level.sendParticles(ParticleTypes.INSTANT_EFFECT, cx + sideX * offset, cy, cz + sideZ * offset, 1, 0, 0, 0, 0);
            }

            double half = width + 0.5;
            double minY = look.y > 0 ? -3.5 : -2.0;
            double maxY = look.y < 0 ? 4.5 : 3.0;
            AABB box = new AABB(cx - half, cy + minY, cz - half, cx + half, cy + maxY, cz + half);
            double maxDamage = RLMTConfig.HEROBRINE_MAX_DAMAGE.get();
            double minDamage = RLMTConfig.HEROBRINE_MIN_DAMAGE.get();
            float damage = (float) Mth.lerp((step - 1) / (double) (STEPS - 1), maxDamage, minDamage);

            for (LivingEntity target : level.getEntitiesOfClass(LivingEntity.class, box)) {
                if (target == player || !hit.add(target.getUUID())) continue;
                target.hurt(level.damageSources().indirectMagic(player, player), damage);
                target.setRemainingFireTicks(60);
                double dx = target.getX() - cx;
                double dz = target.getZ() - cz;
                double dist = Math.sqrt(dx * dx + dz * dz);
                if (dist > 0) {
                    target.setDeltaMovement(dx / dist * 0.5, 0.3, dz / dist * 0.5);
                    target.hurtMarked = true;
                }
            }
            return ++step <= STEPS;
        }
    }
}
