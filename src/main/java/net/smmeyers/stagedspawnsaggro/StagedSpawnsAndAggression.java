package net.smmeyers.stagedspawnsaggro;    // TODO: Figure out a better name than smmeyers later. Note, using a reverse domain name/your name is to prevent conflicts

import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.AABB;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.living.MobSpawnEvent;
import net.minecraftforge.event.entity.player.AdvancementEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.config.ModConfig;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import oshi.util.tuples.Pair;

import java.util.List;
import java.util.Objects;
import java.util.Set;

import static net.smmeyers.stagedspawnsaggro.Config.*;
import static net.smmeyers.stagedspawnsaggro.StagedSpawnsAndAggression.StageIncreaseCondition.ADVANCEMENT;

// The value here should match an entry in the META-INF/mods.toml file
@Mod(StagedSpawnsAndAggression.MOD_ID)
public class StagedSpawnsAndAggression {
    // Define mod id in a common place for everything to reference
    public static final String MOD_ID = "stagedspawnsaggro";

    public StagedSpawnsAndAggression(FMLJavaModLoadingContext context) {
        IEventBus modEventBus = context.getModEventBus();
        MinecraftForge.EVENT_BUS.register(this);    // Register ourselves for server and other game events we are interested in
        context.registerConfig(ModConfig.Type.COMMON, Config.SPEC);    // Register our mod's ForgeConfigSpec so that Forge can create and load the config file for us
    }

    @Mod.EventBusSubscriber(modid = MOD_ID, bus = Mod.EventBusSubscriber.Bus.FORGE)
    public static class ForgeEvents {
        private static long lastTime = -1;
        @SubscribeEvent // TODO:  Switch how this event is registered so it can be registered only if the config option is set. maybe. Future me: but why tho? Futurer me: save tick performance for those who don't use the day condition.
        public static void onWorldTick(TickEvent.LevelTickEvent event) {
            if (event.phase == TickEvent.Phase.END && !event.level.isClientSide) {
                long currentTime = event.level.getDayTime() % 24000L;
                if (currentTime < lastTime) {
                    event.level.players().forEach(player -> {
                        PlayerMixinAccessor playerAccessor = (PlayerMixinAccessor) player;
                        playerAccessor.setDaysPlayed(playerAccessor.getDaysPlayed() + 1);
                    });
                }
                lastTime = currentTime;
            }
        }

        @SubscribeEvent
        public static void onClone(PlayerEvent.Clone event) {
            if (event.isWasDeath()) {
                Player newPlayer = event.getEntity();
                Player oldPlayer = event.getOriginal();

                PlayerMixinAccessor newPlayerAccessor = (PlayerMixinAccessor) newPlayer;
                PlayerMixinAccessor oldPlayerAccessor = (PlayerMixinAccessor) oldPlayer;

                newPlayerAccessor.setAggressionDifficulty(oldPlayerAccessor.getAggressionDifficulty());
                newPlayerAccessor.setDaysPlayed(oldPlayerAccessor.getDaysPlayed());
                newPlayerAccessor.setSpawnDifficulty(oldPlayerAccessor.getSpawnDifficulty());
            }
        }

        @SubscribeEvent
        public static void onAdvancement(AdvancementEvent.AdvancementEarnEvent event) {
            PlayerMixinAccessor player = (PlayerMixinAccessor) event.getEntity();
            String advancementId = event.getAdvancement().getId().toString();
            player.setAggressionDifficulty(advancementConditionHelper(aggroIncreaseConditions,
                                                                      player.getAggressionDifficulty(), advancementId));
            player.setSpawnDifficulty(advancementConditionHelper(spawnIncreaseConditions, player.getSpawnDifficulty(),
                                                                                                        advancementId));
        }

        private static int advancementConditionHelper(
                                            List<Pair<StageIncreaseCondition, Object>> stageIncreaseConditions,
                                                                               int currentStage, String advancementId) {
            int stage = currentStage;
            for (int i = 0; i < stageIncreaseConditions.size(); i++) {
                if (stageIncreaseConditions.get(i) != null && stageIncreaseConditions.get(i).getA().equals(ADVANCEMENT)
                                   && Objects.equals(stageIncreaseConditions.get(i).getB().toString(), advancementId)) {
                    // TODO:  Make it possible to decrease spawn aggression difficulty without letting it decrease when it isn't supposed to.
                    // Currently, by only setting the difficulty if it's higher than the current difficulty, we can't decrease it,
                    // but if we don't do it this way, it may override the difficulty set by other conditions.
                    if (currentStage < i + 1) {
                        stage = i + 1;
                    }
                }
            }
            return stage;
        }


        @SubscribeEvent
        public static void onRegisterCommands(RegisterCommandsEvent event) {
            SPAACommands.register(event.getDispatcher());
        }

        @SubscribeEvent
        public static void onSpawnPlacementCheck(MobSpawnEvent.SpawnPlacementCheck event) {
            // Deny spawn if mob is in list and within config specified chunks of a player at too low of a difficulty.
            // TODO:  Make this a config option
            if (event.getSpawnType() == MobSpawnType.NATURAL) {
                //get all players within config specified chunks of the spawn location
                List<Player> players = event.getLevel().getEntitiesOfClass(Player.class,
                                                  new AABB(event.getPos()).inflate(
                                                                            Config.spawnRestrictionRadius * 16));
                for (Player player : players) {
                    if (mobListHasRestrictedMob((PlayerMixinAccessor) player, spawnRestrictedMobs,
                                                                                               event.getEntityType())) {
                        event.setResult(MobSpawnEvent.SpawnPlacementCheck.Result.DENY);
                        return;
                    }
                }
            }
        }
    }

    // Check if the mob is in the mob list for the player's difficulty stage.
    public static boolean mobListHasRestrictedMob(PlayerMixinAccessor playerAccessor,
                                                    List<Set<? extends EntityType<?>>> mobList, EntityType<?> mobType) {
        for (int i = playerAccessor.getAggressionDifficulty(); i < mobList.size(); i++) {
            if (mobList.get(i) != null && mobList.get(i).contains(mobType)) {
                return true;
            }
        }
        return false;
    }

    public enum StageIncreaseCondition {
        DAYS_PLAYED,
        ADVANCEMENT,
//        TICKS_PLAYED,
//        DAYS_SINCE_DEATH,
//        HOSTILE_KILL_COUNT,
//        KILL_MOB,
//        ITEM_GET,

        // Ideas for conditions to increase spawn aggression difficulty:
        // - Player kills a mob
        // - Player gets killed by a mob
        // - Player gets an advancement
        // - Player gets a certain item
        // - A certain number of days pass
        // - A certain number of mobs are killed
        // - A certain number of ticks pass
    }

}
