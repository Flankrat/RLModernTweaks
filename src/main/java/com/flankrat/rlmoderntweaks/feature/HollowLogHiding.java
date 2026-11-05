package com.flankrat.rlmoderntweaks.feature;

import com.flankrat.rlmoderntweaks.config.RLMTConfig;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.living.LivingAttackEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.registries.ForgeRegistries;

import java.util.List;

/**
 * Players inside a hollow log (Quark) hide from mobs that can't see them:
 * those mobs drop their target, and their attacks are cancelled.
 * Port of hidey_hollow_holes.js. Its EntityEvents.spawned part is left out: a mob that just
 * spawned has no target yet, so it never did anything.
 */
public final class HollowLogHiding {

    private static final TagKey<Block> HOLLOW_LOGS =
            TagKey.create(Registries.BLOCK, new ResourceLocation("quark", "hollow_logs"));

    private HollowLogHiding() {
    }

    public static boolean isInsideHollowLog(Player player) {
        BlockState state = player.level().getBlockState(player.blockPosition());
        if (state.isAir()) return false;
        if (state.is(HOLLOW_LOGS)) return true;

        ResourceLocation id = ForgeRegistries.BLOCKS.getKey(state.getBlock());
        if (id == null) return false;
        String path = id.getPath();
        return path.contains("hollow") && path.contains("log");
    }

    @SubscribeEvent
    public static void onPlayerTick(TickEvent.PlayerTickEvent event) {
        if (event.phase != TickEvent.Phase.END || event.side.isClient()) return;
        if (!RLMTConfig.HOLLOW_LOGS_ENABLED.get()) return;

        Player player = event.player;
        if (player.tickCount % RLMTConfig.HOLLOW_LOGS_INTERVAL.get() != 0) return;
        if (!isInsideHollowLog(player)) return;

        int radius = RLMTConfig.HOLLOW_LOGS_RADIUS.get();
        List<Mob> hunters = player.level().getEntitiesOfClass(Mob.class,
                player.getBoundingBox().inflate(radius),
                mob -> mob.getTarget() == player);

        for (Mob mob : hunters) {
            if (!mob.hasLineOfSight(player)) {
                mob.setTarget(null);
                mob.setLastHurtByMob(null);
            }
        }
    }

    @SubscribeEvent
    public static void onLivingAttack(LivingAttackEvent event) {
        if (!(event.getEntity() instanceof Player player) || player.level().isClientSide()) return;
        if (!RLMTConfig.HOLLOW_LOGS_ENABLED.get()) return;
        if (!(event.getSource().getEntity() instanceof Mob attacker)) return;

        if (isInsideHollowLog(player) && !attacker.hasLineOfSight(player)) {
            event.setCanceled(true);
        }
    }
}
