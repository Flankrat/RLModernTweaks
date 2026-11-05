package com.flankrat.rlmoderntweaks.config;

import net.minecraftforge.common.ForgeConfigSpec;

import java.util.List;

/**
 * Common config, written to config/rlmoderntweaks-common.toml.
 * Every tweak has its own toggle so packs can turn pieces off without removing the mod.
 */
public final class RLMTConfig {

    public static final ForgeConfigSpec SPEC;

    // Player gravity
    public static final ForgeConfigSpec.BooleanValue GRAVITY_ENABLED;
    public static final ForgeConfigSpec.DoubleValue GRAVITY_VALUE;

    // Magic Focus + golden weapons
    public static final ForgeConfigSpec.BooleanValue MAGIC_FOCUS_ENABLED;
    public static final ForgeConfigSpec.ConfigValue<String> MAGIC_FOCUS_EFFECT;
    public static final ForgeConfigSpec.DoubleValue MAGIC_FOCUS_BONUS_PER_LEVEL;
    public static final ForgeConfigSpec.ConfigValue<List<? extends String>> MAGIC_FOCUS_WEAPONS;

    // Golden Osmosis (Talents)
    public static final ForgeConfigSpec.BooleanValue GOLDEN_OSMOSIS_ENABLED;
    public static final ForgeConfigSpec.ConfigValue<List<? extends String>> GOLDEN_OSMOSIS_EXTRA_ITEMS;

    // Hollow log hiding
    public static final ForgeConfigSpec.BooleanValue HOLLOW_LOGS_ENABLED;
    public static final ForgeConfigSpec.IntValue HOLLOW_LOGS_RADIUS;
    public static final ForgeConfigSpec.IntValue HOLLOW_LOGS_INTERVAL;

    // Banned enchantments
    public static final ForgeConfigSpec.ConfigValue<List<? extends String>> BANNED_ENCHANTMENTS;
    public static final ForgeConfigSpec.IntValue BANNED_ENCHANTMENTS_INTERVAL;

    // Fire items
    public static final ForgeConfigSpec.BooleanValue FIRE_ITEMS_ENABLED;
    public static final ForgeConfigSpec.ConfigValue<List<? extends String>> FIRE_ITEMS;
    public static final ForgeConfigSpec.IntValue FIRE_ITEMS_SECONDS;

    // Bark
    public static final ForgeConfigSpec.BooleanValue BARK_STRIP_ENABLED;
    public static final ForgeConfigSpec.ConfigValue<String> BARK_ITEM;
    public static final ForgeConfigSpec.IntValue BARK_MIN;
    public static final ForgeConfigSpec.IntValue BARK_MAX;
    public static final ForgeConfigSpec.BooleanValue BARK_FUEL_ENABLED;
    public static final ForgeConfigSpec.IntValue BARK_BURN_TIME;

    // Flat ground check for structures
    public static final ForgeConfigSpec.BooleanValue FLAT_GROUND_ENABLED;
    public static final ForgeConfigSpec.ConfigValue<List<? extends String>> FLAT_GROUND_STRUCTURES;
    public static final ForgeConfigSpec.IntValue FLAT_GROUND_MAX_DIFFERENCE;

    // Structures that don't keep the world's water
    public static final ForgeConfigSpec.BooleanValue NO_WATERLOGGING_ENABLED;
    public static final ForgeConfigSpec.ConfigValue<List<? extends String>> NO_WATERLOGGING_TEMPLATES;

    // Socketing
    public static final ForgeConfigSpec.BooleanValue SOCKETING_ENABLED;
    public static final ForgeConfigSpec.IntValue SOCKET_MAX;
    public static final ForgeConfigSpec.ConfigValue<List<? extends String>> SOCKET_ROLLS;
    public static final ForgeConfigSpec.ConfigValue<List<? extends String>> SOCKET_TIERS;
    public static final ForgeConfigSpec.ConfigValue<String> SOCKET_GEM_TAG;
    public static final ForgeConfigSpec.ConfigValue<List<? extends String>> SOCKET_GEMS;
    public static final ForgeConfigSpec.ConfigValue<String> SOCKET_DEFAULT_BONUS;

    static {
        ForgeConfigSpec.Builder b = new ForgeConfigSpec.Builder();

        b.comment("Item lists accept three kinds of entries:",
                "  \"modid:item\"      one item",
                "  \"modid:prefix*\"   every item from that mod whose name starts with prefix",
                "  \"#modid:tag\"      every item in an item tag");

        b.push("gravity");
        GRAVITY_ENABLED = b.comment("Give players 1.12-style gravity. Replaces the /attribute command in Global_Rules.js.")
                .define("enabled", true);
        GRAVITY_VALUE = b.comment("Player base value for forge:entity_gravity (vanilla is 0.08).")
                .defineInRange("value", 0.084, 0.0, 1.0);
        b.pop();

        b.push("magic_focus");
        MAGIC_FOCUS_ENABLED = b.comment("Direct melee hits with golden weapons deal extra damage while the attacker has Magic Focus.")
                .define("enabled", true);
        MAGIC_FOCUS_EFFECT = b.comment("Effect that grants the bonus.")
                .define("effect", "simple_difficulty:magic_focus");
        MAGIC_FOCUS_BONUS_PER_LEVEL = b.comment("Extra damage per effect level, as a fraction (0.5 = +50% per level).")
                .defineInRange("bonusPerLevel", 0.5, 0.0, 100.0);
        MAGIC_FOCUS_WEAPONS = b.comment("Weapons that get the bonus.")
                .defineListAllowEmpty(List.of("weapons"), () -> List.of(
                        "minecraft:golden_sword", "minecraft:golden_axe", "minecraft:golden_pickaxe",
                        "minecraft:golden_shovel", "minecraft:golden_hoe",
                        "bettersurvival:itemgoldhammer", "bettersurvival:itemgoldspear", "bettersurvival:itemgolddagger",
                        "bettersurvival:itemgoldbattleaxe", "bettersurvival:itemgoldnunchaku",
                        "spartanweaponry:golden_*"
                ), o -> o instanceof String);
        b.pop();

        b.push("golden_osmosis");
        GOLDEN_OSMOSIS_ENABLED = b.comment("Let Talents' Golden Osmosis skill also repair the items below, at the same rate as vanilla gold.")
                .define("enabled", true);
        GOLDEN_OSMOSIS_EXTRA_ITEMS = b.comment("Extra items Golden Osmosis treats as golden. Vanilla gold tools and armor already count.")
                .defineListAllowEmpty(List.of("extraItems"), () -> List.of(
                        "bettersurvival:itemgoldhammer", "bettersurvival:itemgoldspear", "bettersurvival:itemgolddagger",
                        "bettersurvival:itemgoldbattleaxe", "bettersurvival:itemgoldnunchaku",
                        "defiledlands:scale_golden_helmet", "defiledlands:scale_golden_chestplate",
                        "defiledlands:scale_golden_leggings", "defiledlands:scale_golden_boots",
                        "spartanshields:golden_basic_shield", "spartanshields:golden_tower_shield",
                        "spartanweaponry:golden_*"
                ), o -> o instanceof String);
        b.pop();

        b.push("hollow_logs");
        HOLLOW_LOGS_ENABLED = b.comment("Players inside a hollow log can't be attacked, and lose aggro, from mobs that can't see them.")
                .define("enabled", true);
        HOLLOW_LOGS_RADIUS = b.comment("Radius around a hidden player to drop mob aggro in.")
                .defineInRange("radius", 16, 1, 64);
        HOLLOW_LOGS_INTERVAL = b.comment("How often to check, in ticks.")
                .defineInRange("intervalTicks", 5, 1, 200);
        b.pop();

        b.push("banned_enchantments");
        BANNED_ENCHANTMENTS = b.comment("Enchantments removed from items in a player's inventory. Enchanted books are left alone.")
                .defineListAllowEmpty(List.of("enchantments"), () -> List.of("grapplemod:doublejumpenchantment"), o -> o instanceof String);
        BANNED_ENCHANTMENTS_INTERVAL = b.comment("How often to check the inventory, in ticks.")
                .defineInRange("intervalTicks", 20, 1, 1200);
        b.pop();

        b.push("fire_items");
        FIRE_ITEMS_ENABLED = b.comment("Hitting something with one of these items sets it on fire.")
                .define("enabled", true);
        FIRE_ITEMS = b.defineListAllowEmpty(List.of("items"), () -> List.of("minecraft:torch", "minecraft:blaze_rod"), o -> o instanceof String);
        FIRE_ITEMS_SECONDS = b.defineInRange("seconds", 2, 1, 60);
        b.pop();

        b.push("bark");
        BARK_STRIP_ENABLED = b.comment("Stripping a log that burns with an axe drops bark.")
                .define("stripDropsEnabled", true);
        BARK_ITEM = b.define("item", "farmersdelight:tree_bark");
        BARK_MIN = b.defineInRange("minDrop", 1, 0, 64);
        BARK_MAX = b.defineInRange("maxDrop", 3, 0, 64);
        BARK_FUEL_ENABLED = b.comment("Bark items (the item above, and any item whose name ends in _bark) burn for a short time.")
                .define("fuelEnabled", true);
        BARK_BURN_TIME = b.comment("Burn time in ticks (200 = one item smelted).")
                .defineInRange("burnTime", 100, 0, 32000);
        b.pop();

        b.push("flat_ground");
        FLAT_GROUND_ENABLED = b.comment("Only let the structures below spawn on fairly flat ground, so they don't end up with",
                        "tall dirt or sand walls on sloped land. Rejected spots are skipped (the structure becomes",
                        "somewhat rarer in hilly areas). /locate uses the same check.")
                .define("enabled", true);
        FLAT_GROUND_STRUCTURES = b.comment("Structure ids to check.")
                .defineListAllowEmpty(List.of("structures"), () -> List.of(
                        "rec_migrated:rlcraft_plainscamp", "rec_migrated:rlcraft_plainscampruined",
                        "rec_migrated:rlcraft_plainswindmill", "rec_migrated:rlcraft_deserttown"
                ), o -> o instanceof String);
        FLAT_GROUND_MAX_DIFFERENCE = b.comment("Largest allowed height difference, in blocks, between the highest and lowest of",
                        "9 ground samples (corners, edge midpoints and center of the structure's footprint).")
                .defineInRange("maxHeightDifference", 6, 0, 256);
        b.pop();

        b.push("no_waterlogging");
        NO_WATERLOGGING_ENABLED = b.comment("Place the structure templates below without keeping the water already in the world.",
                        "Without this, every stair, slab, fence, leaf block etc. a structure places below sea level",
                        "becomes waterlogged and floods the inside of the structure.")
                .define("enabled", true);
        NO_WATERLOGGING_TEMPLATES = b.comment("Template ids (the \"location\" in the template pool), not structure ids.")
                .defineListAllowEmpty(List.of("templates"), () -> List.of(
                        "rec_migrated:p_cultisland", "rec_migrated:p_islandcity", "rec_migrated:flagship",
                        "rec_migrated:p_largeship", "rec_migrated:p_smallship"
                ), o -> o instanceof String);
        b.pop();

        b.push("socketing");
        SOCKETING_ENABLED = b.comment("Gem socketing (a stand-in until Socketed is ported from 1.12.2).",
                        "Loot gear can roll sockets. Gem in main hand + gear in off hand + right-click to socket it;",
                        "Socket Extraction Tool in main hand + gear in off hand + right-click to take the last gem out.",
                        "Turning this off also turns off the bonuses of gems already socketed.")
                .define("enabled", true);
        SOCKET_MAX = b.comment("Most sockets a piece of gear can have.")
                .defineInRange("maxSockets", 3, 1, 8);
        SOCKET_ROLLS = b.comment("Weighted chances for how many sockets damageable chest, mob and fishing loot rolls.",
                        "Format: \"sockets=weight\"")
                .defineListAllowEmpty(List.of("socketRolls"), () -> List.of("0=50", "1=30", "2=15", "3=5"),
                        o -> o instanceof String);
        SOCKET_TIERS = b.comment("Per socket count: color and the name prefixes to pick from for loot that rolled that many.",
                        "Format: \"sockets;color;Name1,Name2,...\" (color = a Minecraft color name like aqua, light_purple, gold)")
                .defineListAllowEmpty(List.of("tiers"), () -> List.of(
                        "1;aqua;Notched,Chipped,Gem-Set",
                        "2;light_purple;Inlaid,Twin-Set,Gem-Bound",
                        "3;gold;Resplendent,Crowned,Tri-Set"
                ), o -> o instanceof String);
        SOCKET_GEM_TAG = b.comment("Item tag of items that can be socketed.")
                .define("gemTag", "forge:gems");
        SOCKET_GEMS = b.comment("Gem bonuses.",
                        "Format: \"item;attribute;amount;operation;tooltip label;extract fail chance\"",
                        "operation = ADDITION, MULTIPLY_BASE or MULTIPLY_TOTAL. Extract fail chance 0 to 1: chance the gem breaks.")
                .defineListAllowEmpty(List.of("gems"), () -> List.of(
                        "minecraft:diamond;minecraft:generic.armor;1.5;ADDITION;+1.5 Armor;0.5",
                        "minecraft:emerald;minecraft:generic.max_health;2.0;ADDITION;+1 Heart Max Health;0.5",
                        "minecraft:amethyst_shard;minecraft:generic.attack_speed;0.1;MULTIPLY_BASE;+10% Attack Speed;0.5",
                        "minecraft:lapis_lazuli;minecraft:generic.movement_speed;0.05;MULTIPLY_BASE;+5% Speed;0.5",
                        "minecraft:quartz;minecraft:generic.attack_damage;2.0;ADDITION;+2 Attack Damage;0.5",
                        "minecraft:prismarine_crystals;minecraft:generic.armor_toughness;1.0;ADDITION;+1 Armor Toughness;0.5"
                ), o -> o instanceof String);
        SOCKET_DEFAULT_BONUS = b.comment("Bonus for any other gem in the gem tag.",
                        "Format: \"attribute;amount;operation;tooltip label;extract fail chance\"")
                .define("defaultBonus", "minecraft:generic.knockback_resistance;0.1;ADDITION;+10% Knockback Resistance;0.5");
        b.pop();

        SPEC = b.build();
    }

    private RLMTConfig() {
    }
}
