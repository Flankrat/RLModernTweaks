package com.flankrat.rlmoderntweaks.feature;

import com.flankrat.rlmoderntweaks.config.RLMTConfig;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.BlockTags;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.common.ToolActions;
import net.minecraftforge.event.furnace.FurnaceFuelBurnTimeEvent;
import net.minecraftforge.event.level.BlockEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.registries.ForgeRegistries;

/**
 * Bark tweaks:
 * <ul>
 *     <li>Stripping a log that burns with an axe drops bark (port of log_stripping.js).
 *     Unlike the script, this only fires when an axe strips a block (not on every right-click),
 *     respects other mods cancelling the strip, and doesn't damage the axe a second time
 *     (vanilla already does).</li>
 *     <li>Bark burns for a short time (port of FuranceBurnTime_script.js).</li>
 * </ul>
 */
public final class BarkTweaks {

    private static volatile Item barkItem;
    private static volatile boolean barkResolved;

    private BarkTweaks() {
    }

    public static void refresh() {
        barkItem = null;
        barkResolved = false;
    }

    private static Item bark() {
        if (!barkResolved) {
            ResourceLocation id = ResourceLocation.tryParse(RLMTConfig.BARK_ITEM.get());
            Item item = id == null ? null : ForgeRegistries.ITEMS.getValue(id);
            barkItem = item == Items.AIR ? null : item;
            barkResolved = true;
        }
        return barkItem;
    }

    // LOWEST + receiveCanceled=false: only drop bark if no other mod cancelled the strip.
    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void onToolModification(BlockEvent.BlockToolModificationEvent event) {
        if (event.isSimulated() || event.getToolAction() != ToolActions.AXE_STRIP) return;
        if (!RLMTConfig.BARK_STRIP_ENABLED.get()) return;

        LevelAccessor accessor = event.getLevel();
        if (accessor.isClientSide() || !(accessor instanceof Level level)) return;

        // The stripped state is worked out after this event, so filter on what's being stripped:
        // unstripped logs and wood that burn (the same check log_stripping.js used).
        BlockState state = event.getState();
        if (!state.is(BlockTags.LOGS_THAT_BURN)) return;
        ResourceLocation blockId = ForgeRegistries.BLOCKS.getKey(state.getBlock());
        if (blockId != null && blockId.getPath().contains("stripped")) return;

        Item item = bark();
        if (item == null) return;

        int min = RLMTConfig.BARK_MIN.get();
        int max = Math.max(min, RLMTConfig.BARK_MAX.get());
        RandomSource random = level.getRandom();
        int count = min + (max > min ? random.nextInt(max - min + 1) : 0);
        if (count <= 0) return;

        BlockPos pos = event.getPos();
        Block.popResourceFromFace(level, pos, Direction.UP, new ItemStack(item, count));
    }

    @SubscribeEvent
    public static void onFuelBurnTime(FurnaceFuelBurnTimeEvent event) {
        if (!RLMTConfig.BARK_FUEL_ENABLED.get()) return;

        ItemStack stack = event.getItemStack();
        if (stack.isEmpty()) return;

        Item item = stack.getItem();
        boolean isBark = item == bark();
        if (!isBark) {
            ResourceLocation id = ForgeRegistries.ITEMS.getKey(item);
            isBark = id != null && id.getPath().endsWith("_bark");
        }
        if (isBark) {
            event.setBurnTime(RLMTConfig.BARK_BURN_TIME.get());
        }
    }
}
