package net.smmeyers.stagedspawnsaggro;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.EntityType;
import net.minecraftforge.common.ForgeConfigSpec;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.config.ModConfigEvent;
import net.minecraftforge.registries.ForgeRegistries;
import oshi.util.tuples.Pair;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;


@Mod.EventBusSubscriber(modid = StagedSpawnsAndAggression.MOD_ID, bus = Mod.EventBusSubscriber.Bus.MOD)
public class Config {
    private static final ForgeConfigSpec.Builder BUILDER = new ForgeConfigSpec.Builder();

    // List of mobs to ignore the player
    private static final ForgeConfigSpec.ConfigValue<List<? extends List<? extends String>>> AGGRO_RESTRICTED_MOBS = BUILDER
            .comment("""
                    This list contains lists of mobs which will not attack players of a given aggression difficulty level.\
                    
                    The number of lists defines how many aggression difficulty levels there are, and the order of the lists\
                    
                    determines the order at which mobs will start to target the player as they increase their spawn\
                    
                    aggression level. For example, with [["minecraft:creeper", "minecraft:skeleton"], ["alexsmobs:tiger"]],\
                    
                    when the player first joins the world, creepers, tigers, and skeletons will not target the player,\
                    
                    but after increasing the player's aggression difficulty level once, creepers and skeletons will attack\
                    
                    the player, while tigers will not. After increasing their aggression difficulty level once more, tigers\
                    
                    will also attack the player.\
                    
                    Harder groups of mobs should be after easier groups of mobs. Be mindful of quotes, commas, brackets,\
                    mob and mod names, etc.""")
            .defineListAllowEmpty("aggroRestrictedMobs", List.of(List.of()), Config::validateMobName);

    private static final ForgeConfigSpec.ConfigValue<List<? extends List<?>>> AGGRO_INCREASE_CONDITIONS = BUILDER
            .comment("""
                    This list contains lists of conditions which must be met to increase the player's aggression\
                    
                    difficulty level. The first element of each list is the condition type, and the second element is\
                    
                    the condition value. The condition types are as follows:\
                    
                    - DAYS_PLAYED: Condition is passed if player plays for a certain number of days.\
                    
                    - ADVANCEMENT: Condition is passed if player gets a certain advancement.\
                    
                    For example, with the previous example from the aggroRestrictedMobs, having the aggro increase\
                    
                    conditions as [["DAYS_PLAYED", 3], ["ADVANCEMENT", "minecraft:adventure/kill_a_mob"]], means that\
                    
                    creepers and skeletons will attack the player after playing for 3 days, and creepers, skeletons and\
                    
                    tigers will attack the player after the player gets the monster hunter advancement. Creepers and\
                    
                    skeletons will still attack the player even if they haven't played for 3 days, if they get the monster\
                    
                    hunter advancement first.\
                    
                    In other words, the order of the conditions matter. The harder conditions should be after the easier\
                    
                    ones, and it must match the order of mobs. The player can skip easier conditions if they accomplish\
                    
                    harder ones first. Be mindful of quotes, commas, brackets, and capitalization when entering the conditions.\
                    
                    Find the vanilla advancement IDs here: https://minecraft.fandom.com/wiki/Advancement#List_of_advancements\
                    
                    For your testing convenience, here's an example of a command to revoke the monster hunter advancement:\
                    
                    /advancement revoke @p only minecraft:adventure/kill_a_mob\
                    
                    Note that revoking an advancement or setting days lower will not decrease the difficulty level automatically,\
                    
                    You'll have to use the /spaa set difficulty command to do that.""")
                    // TODO: Make it so the player doesn't have to be mindful about capitalization at least
            .defineListAllowEmpty("aggroIncreaseConditions", List.of(List.of()), Config::validateStageIncreaseCondition);

    private static final ForgeConfigSpec.ConfigValue<List<? extends List<? extends String>>> SPAWN_RESTRICTED_MOBS = BUILDER
            .comment("""
                    This list contains lists of mobs which will not spawn near players of a given spawn difficulty level.\
                    
                    The number of lists defines how many spawn difficulty levels there are, and the order of the lists\
                    
                    determines the order at which mobs will start to spawn as they increase their spawn\
                    
                    difficulty level. For example, with [["minecraft:creeper", "minecraft:skeleton"], ["alexsmobs:tiger"]],\
                    
                    when the player first joins the world, creepers, tigers, and skeletons will not spawn near the player,\
                    
                    but after increasing the player's spawn difficulty level once, creepers and skeletons will spawn near\
                    
                    the player, while tigers will not. After increasing their spawn difficulty level once more, tigers\
                    
                    will also spawn near the player.\
                    
                    Harder groups of mobs should be after easier groups of mobs. Be mindful of quotes, commas, brackets,\
                    mob and mod names, etc.""")
            // TODO: Fix comment
            .defineListAllowEmpty("spawnRestrictedMobs", List.of(List.of()), Config::validateMobName);

    private static final ForgeConfigSpec.ConfigValue<List<? extends List<?>>> SPAWN_INCREASE_CONDITIONS = BUILDER
            .comment("""
                    This list contains lists of conditions which must be met to increase the player's spawn\
                    
                    difficulty level. The first element of each list is the condition type, and the second element is\
                    
                    the condition value. The condition types are as follows:\
                    
                    - DAYS_PLAYED: Condition is passed if player plays for a certain number of days.\
                    
                    - ADVANCEMENT: Condition is passed if player gets a certain advancement.\
                    
                    For example, with the previous example from the aggroRestrictedMobs, having the spawn increase\
                    
                    conditions as [["DAYS_PLAYED", 3], ["ADVANCEMENT", "minecraft:adventure/kill_a_mob"]], means that\
                    
                    creepers and skeletons will spawn near the player after playing for 3 days, and creepers, skeletons and\
                    
                    tigers will spawn near the player after the player gets the monster hunter advancement. Creepers and\
                    
                    skeletons will still spawn near the player even if they haven't played for 3 days, if they get the monster\
                    
                    hunter advancement first.\
                    
                    In other words, the order of the conditions matter. The harder conditions should be after the easier\
                    
                    ones, and it must match the order of mobs. The player can skip easier conditions if they accomplish\
                    
                    harder ones first. Be mindful of quotes, commas, brackets, and capitalization when entering the conditions.\
                    
                    Find the vanilla advancement IDs here: https://minecraft.fandom.com/wiki/Advancement#List_of_advancements\
                    
                    For your testing convenience, here's an example of a command to revoke the monster hunter advancement:\
                    
                    /advancement revoke @p only minecraft:adventure/kill_a_mob\
                    
                    Note that revoking an advancement or setting days lower will not decrease the difficulty level automatically,\
                    
                    You'll have to use the /spaa set difficulty command to do that.""")
            // TODO: Make it so the player doesn't have to be mindful about capitalization at least
            .defineListAllowEmpty("spawnIncreaseConditions", List.of(List.of()), Config::validateStageIncreaseCondition);

    private static final ForgeConfigSpec.IntValue SPAWN_RESTRICTION_RADIUS = BUILDER
            .comment("""
                    This value determines the radius around the player in which mobs will not spawn.\
                    
                    The default value is 8, which means that mobs will not spawn within 8 chunks of the player.\
                    
                    This value is in chunks, so a value of 1 means that mobs will not spawn within 16 blocks of the player.\
                    
                    Consider your simulation distance when setting this. You may wish to see mobs spawn in the distance\
                    
                    or not at all.""")
            .defineInRange("spawnRestrictionRadius", 8, 0, Integer.MAX_VALUE);

    private static final ForgeConfigSpec.BooleanValue PRINT_DAY = BUILDER
            .comment("""
                    Whether or not to briefly show the current day above the action bar when the day changes.\
                    
                    default: true""")
            // TODO: Fix comment.
            .define("printDay", true);

    private static final ForgeConfigSpec.BooleanValue PRINT_MOBS = BUILDER
            .comment("""
                    Whether or not to display the mobs that will spawn/attack the player when the stage increases in the chat.\
                    
                    default: false""")
            .define("printMobs", false);

    public static final ForgeConfigSpec.ConfigValue<List<? extends String>> AGGRO_STAGE_MESSAGES = BUILDER
            .comment("""
                    This list contains messages to be displayed when the player increases their spawn aggression difficulty level.\
                    
                    The number of messages must match how many lists are in the aggression restricted mob lists, and the order of the messages\
                    
                    determines the order at which they will be displayed as they increase their spawn\
                    
                    aggression difficulty level. Blank messages are allowed, and will not be displayed.""")
            .defineListAllowEmpty("aggroStageMessages", List.of("You feel a chill in the air.", ""), (o) -> {
                return o instanceof String;
            });

    public static final ForgeConfigSpec.ConfigValue<List<? extends String>> SPAWN_STAGE_MESSAGES = BUILDER
            .comment("""
                    This list contains messages to be displayed when the player increases their spawn difficulty level.\
                    
                    The number of messages must match how many lists are in the spawn restricted mob lists, and the order of the messages\
                    
                    determines the order at which they will be displayed as they increase their spawn\
                    
                    difficulty level. Blank messages are allowed, and will not be displayed.""")
            .defineListAllowEmpty("spawnStageMessages", List.of("", "You feel a sense of dread."), (o) -> {
                return o instanceof String;
            });

    static final ForgeConfigSpec SPEC = BUILDER.build();


    private static boolean validateMobName(final Object obj)
    {
        if (!(obj instanceof List<?>)) { return false; }
        for (Object mobNameObj : (List<?>) obj)
        {
            if (!(mobNameObj instanceof final String mobName && ForgeRegistries.ENTITY_TYPES.containsKey(new
                                                                                            ResourceLocation(mobName))))
            {
                return false;
            }
        }
        return true;
    }

    private static boolean validateStageIncreaseCondition(final Object obj)
    {
        if (!(obj instanceof List<?> increaseCond)) { return false; }
        if (increaseCond.size() != 2) { return false; }  //TODO: Unless we eventually have options that don't need a second argument.
        if (!(increaseCond.get(0) instanceof String arg0Str)) { return false; }

        // Check if the first element is a valid stage increase condition
        StagedSpawnsAndAggression.StageIncreaseCondition arg0;
        try {
            arg0 = StagedSpawnsAndAggression.StageIncreaseCondition.valueOf(arg0Str);
        }
        catch (IllegalArgumentException e) {
            return false;
        }

        switch (arg0) {
            case DAYS_PLAYED -> {
                if (!(increaseCond.get(1) instanceof Integer daysPlayed) || daysPlayed < 0) { return false; }
            }
            case ADVANCEMENT -> {
                if (!(increaseCond.get(1) instanceof String advancementName)) { return false; }
//                if (!validateAdvancementName(advancementName)) { return false; }
            }
            default -> throw new IllegalStateException("Unexpected value: " + arg0);
        }

        return true;
    }

    // // Doesn't work. no server because configs are loaded before the server starts
//    private static boolean validateAdvancementName(String advancementName)
//    {
//        var server = net.minecraftforge.server.ServerLifecycleHooks.getCurrentServer();
//        if (server == null) return false;
//
//        var advancementManager = server.getAdvancements();
//        return advancementManager.getAdvancement(new ResourceLocation(advancementName)) != null;
//    }

    public static List<Set<? extends EntityType<?>>> aggroRestrictedMobs;
    public static List<Pair<StagedSpawnsAndAggression.StageIncreaseCondition, Object>> aggroIncreaseConditions;
    public static List<Set<? extends EntityType<?>>> spawnRestrictedMobs;
    public static List<Pair<StagedSpawnsAndAggression.StageIncreaseCondition, Object>> spawnIncreaseConditions;
    public static int spawnRestrictionRadius;
    public static boolean printDay;
    public static boolean printMobs;
    public static List<String> aggroStageMessages;
    public static List<String> spawnStageMessages;


    private static List<Set<? extends EntityType<?>>> collectMobList(
                                          ForgeConfigSpec.ConfigValue<List<? extends List<? extends String>>> mobList) {
        return mobList.get().stream()
                .map(list -> ((List<?>) list).stream()
                        .map(mobName -> ForgeRegistries.ENTITY_TYPES.getValue(new
                                                                                    ResourceLocation((String) mobName)))
                        .collect(Collectors.toSet()))
                .collect(Collectors.toList());
    }

    private static List<Pair<StagedSpawnsAndAggression.StageIncreaseCondition, Object>> collectStageIncreaseConditions(
                                         ForgeConfigSpec.ConfigValue<List<? extends List<?>>> stageIncreaseConditions) {
        return stageIncreaseConditions.get().stream()
                .map(list -> {
//                    var conditionType = (String) ((List<?>) list).get(0);
//                    var conditionValue = ((List<?>) list).get(1);
                    var listObj = (List<?>) list;
                    if (listObj.size() < 2) return null;
                    var conditionType = (String) listObj.get(0);
                    var conditionValue = listObj.get(1);
                    return new Pair<>(StagedSpawnsAndAggression.StageIncreaseCondition.valueOf(conditionType),
                                                                                                        conditionValue);
                })
                .collect(Collectors.toList());
    }


    @SubscribeEvent
    static void onLoad(final ModConfigEvent event)
    {
        // Map the nested list of mob names to a List<Set<EntityType<?>>>
        aggroRestrictedMobs = collectMobList(AGGRO_RESTRICTED_MOBS);

        // Map the nested list of stage increase conditions to a List<Pair<StageIncreaseCondition, Object>>
        aggroIncreaseConditions = collectStageIncreaseConditions(AGGRO_INCREASE_CONDITIONS);

        spawnRestrictedMobs = collectMobList(SPAWN_RESTRICTED_MOBS);
        spawnIncreaseConditions = collectStageIncreaseConditions(SPAWN_INCREASE_CONDITIONS);
        spawnRestrictionRadius = SPAWN_RESTRICTION_RADIUS.get();
        printDay = PRINT_DAY.get();
        printMobs = PRINT_MOBS.get();
        aggroStageMessages = new ArrayList<>(AGGRO_STAGE_MESSAGES.get());
        spawnStageMessages = new ArrayList<>(SPAWN_STAGE_MESSAGES.get());



        // TODO: Make the validation functions tell the player when they did something wrong.
        // TODO: Ensure validation/conversion functions are somewhat bulletproof, especially where whitespace is concerned.
        // TODO: Wildcard option for mobs to ignore
        // TODO: commands on difficulty level increase
        // TODO: Ability to specify what MobSpawnTypes to prevent
        // TODO: Better data model. Maybe mob groups in objects with properties that allow for sequential or unordered stages. e.g. maybe... {GROUP_ID: 1, mobs: [creeper, skeleton], conditions: {DAYS_PLAYED: 3, KEYWHATEVER: VALUEWHATEVER, multicondition_mode: OR}, mobSpawnTypes: [NATURAL, SPAWNER], ALSO_UNLOCKS_GROUPS: [2, 3], reversal_conditions: {DIED_X_TIMES: 5}}
        // TODO: expanding on the above, maybe also ability to filter mobs by type, e.g. hostile, creature, whatever.
        // TODO: Option to specify where text is displayed. e.g. chat, action bar, title, etc.
        // TODO: Code docstrings and comments and whatnot
        // TODO: Message colors
        // TODO: Edit the config file to explain that aggro can't be restricted for every mob from every mod. Some will completely ignore it.
        // Maybe: Option for Unordered stages, where being at difficulty 2 doesn't automatically mean difficulty 1 is also active
        // Maybe: Multiple concurrent conditions for increasing spawn aggression difficulty. AKA, "AND" conditions. Must fulfill multiple conditions to increase spawn aggression difficulty.
        // Maybe: Conditions for decreasing spawn aggression difficulty
        // Huge Maybe: Loot tables changing based on aggression difficulty level

        // Ideas for conditions to increase spawn aggression difficulty:
        // - Player kills a mob
        // - Player gets killed by a mob
        // - Player gets a certain item
        // - A certain number of mobs are killed
        // - A certain number of ticks pass
        // - Tame a specific mob... (perhaps x number of times?)
    }
}
