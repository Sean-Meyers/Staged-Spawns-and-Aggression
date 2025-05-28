package net.smmeyers.stagedspawnsaggro;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EntityType;
import net.minecraftforge.server.command.EnumArgument;

import java.util.Collection;
import java.util.List;
import java.util.Set;
import java.util.function.Function;

import static net.smmeyers.stagedspawnsaggro.Config.aggroRestrictedMobs;
import static net.smmeyers.stagedspawnsaggro.Config.spawnRestrictedMobs;

public class SPAACommands {
    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(Commands.literal("spaa").requires(
                                    commandSourceStack -> commandSourceStack.hasPermission(2))
            .then(Commands.literal("get")
                .then(Commands.literal("difficulty")
                    .then(Commands.argument("ALL | SPAWN | AGGRO", EnumArgument.enumArgument(AllOrSpawnOrAggro.class))
                        .then(Commands.argument("Player(s)", EntityArgument.players())
                            .executes(SPAACommands::getSpawnAggressionDifficulties)
                        )
                    )
                ).then(Commands.literal("days")
                    .then(Commands.argument("Player(s)", EntityArgument.players())
                        .executes(SPAACommands::getDays)
                    )
                ).then(Commands.literal("mobs")
                    .then(Commands.argument("ALL | SPAWN | AGGRO", EnumArgument.enumArgument(AllOrSpawnOrAggro.class))
                            .executes(SPAACommands::getConfigMobs)
                        .then(Commands.argument("Player", EntityArgument.player())
                            .executes(SPAACommands::getPlayerMobs)
                        )
                    )
                ).then(Commands.literal("spawn_radius")
                        .executes(SPAACommands::getSpawnRadius)
                )
            ).then(Commands.literal("set")
                .then(Commands.literal("difficulty")
                    .then(Commands.argument("ALL | SPAWN | AGGRO", EnumArgument.enumArgument(AllOrSpawnOrAggro.class))
                        .then(Commands.argument("Player(s)", EntityArgument.players())
                            .then(Commands.argument("difficulty", IntegerArgumentType.integer(0))
                                .executes(SPAACommands::setSpawnAggressionDifficulties)
                            )
                        )
                    )
                ).then(Commands.literal("days")
                    .then(Commands.argument("Player(s)", EntityArgument.players())
                        .then(Commands.argument("days", IntegerArgumentType.integer(0))
                            .executes(SPAACommands::setDays)
                        )
                    )
                ).then(Commands.literal("spawn_radius")
                    .then(Commands.argument("Spawn Radius", IntegerArgumentType.integer(0))
                        .executes(SPAACommands::setSpawnRadius)
                    )
                )
            )
        );

    }

    private static int setSpawnRadius(CommandContext<CommandSourceStack> context) {
        int spawnRadius = IntegerArgumentType.getInteger(context, "Spawn Radius");
        Config.spawnRestrictionRadius = spawnRadius;
        context.getSource().sendSuccess(() -> Component.literal("Spawn radius set to: "
                                                                                     + spawnRadius), false);
        return 1;
    }

    private static int getSpawnRadius(CommandContext<CommandSourceStack> commandSourceStackCommandContext) {
        commandSourceStackCommandContext.getSource().sendSuccess(() -> Component.literal("Spawn radius: "
                                                           + Config.spawnRestrictionRadius), false);
        return 1;
    }

    public enum AllOrSpawnOrAggro {
        ALL,
        SPAWN,
        AGGRO
    }

    private static int setDays(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        Collection<ServerPlayer> players = EntityArgument.getPlayers(context, "Player(s)");
        int days = IntegerArgumentType.getInteger(context, "days");

        for (ServerPlayer player : players) {
            ((PlayerMixinAccessor) player).setDaysPlayed(days);
            context.getSource().sendSuccess(() -> Component.literal("Set " + player.getName().getString()
                                                                     + "'s days played to " + days), false);
        }
        return 1;
    }

    private static int setSpawnAggressionDifficulties(CommandContext<CommandSourceStack> context) throws
                                                                                                CommandSyntaxException {
        Collection<ServerPlayer> players = EntityArgument.getPlayers(context, "Player(s)");
        int difficulty = IntegerArgumentType.getInteger(context, "difficulty");
        AllOrSpawnOrAggro mode = context.getArgument("ALL | SPAWN | AGGRO", AllOrSpawnOrAggro.class);
        StringBuilder message = new StringBuilder();
        for (ServerPlayer player : players) {
            if (mode == AllOrSpawnOrAggro.SPAWN || mode == AllOrSpawnOrAggro.ALL) {
                ((PlayerMixinAccessor) player).setSpawnDifficulty(difficulty);
                message.append("Set ").append(player.getName().getString()).append("'s spawn difficulty to ")
                                                                                       .append(difficulty).append("\n");
            }
            if (mode == AllOrSpawnOrAggro.AGGRO || mode == AllOrSpawnOrAggro.ALL) {
                ((PlayerMixinAccessor) player).setAggressionDifficulty(difficulty);
                message.append("Set ").append(player.getName().getString()).append("'s aggression difficulty to ")
                                                                                       .append(difficulty).append("\n");
            }
            context.getSource().sendSuccess(() -> Component.literal(message.toString()), false);
        }
        return 1;
    }

    private static int getPlayerMobs(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        ServerPlayer player = EntityArgument.getPlayer(context, "Player");
        int aggressionDifficulty = ((PlayerMixinAccessor) player).getAggressionDifficulty();

        getMobCommandHelper(context, aggressionDifficulty, player.getName().getString() + "'s mobs:\n");
        return 1;
    }

    private static int getConfigMobs(CommandContext<CommandSourceStack> context) {
        getMobCommandHelper(context, 0, "Mobs in config:\n");
        return 1;
    }

    private static void getMobCommandHelper(CommandContext<CommandSourceStack> context, int startIndex, String header) {
        AllOrSpawnOrAggro allOrSpawnOrAggro = context.getArgument("ALL | SPAWN | AGGRO", AllOrSpawnOrAggro.class);
        StringBuilder message = new StringBuilder(header);
        if (allOrSpawnOrAggro == AllOrSpawnOrAggro.SPAWN || allOrSpawnOrAggro == AllOrSpawnOrAggro.ALL) {
            mobMessageAppendingHelper(message, spawnRestrictedMobs, "Mobs restricted from spawning:", startIndex);
        }
        if (allOrSpawnOrAggro == AllOrSpawnOrAggro.AGGRO || allOrSpawnOrAggro == AllOrSpawnOrAggro.ALL) {
            mobMessageAppendingHelper(message, aggroRestrictedMobs, "Mobs restricted from attacking:", startIndex);
        }
        context.getSource().sendSuccess(() -> Component.literal(message.toString()), false);
    }

    private static void mobMessageAppendingHelper(StringBuilder message, List<Set<? extends EntityType<?>>> mobList,
                                                                                        String header, int startIndex) {
        message.append(header).append("\n");
        for (; startIndex < mobList.size(); startIndex++) {
            message.append("Stage ").append(startIndex).append(":\n");
            for (EntityType<?> mob : mobList.get(startIndex)) {
                message.append(BuiltInRegistries.ENTITY_TYPE.getKey(mob)).append("\n");
            }
        }
    }

    private static int getDays(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        String message = multiPlayerCommandOutputHelper(context, "Days Played:\n",
                                                                                    PlayerMixinAccessor::getDaysPlayed);
        context.getSource().sendSuccess(() -> Component.literal(message), false);
        return 1;
    }

    private static int getSpawnAggressionDifficulties(CommandContext<CommandSourceStack> context) throws
                                                                                                CommandSyntaxException {
        StringBuilder message = new StringBuilder();
        AllOrSpawnOrAggro mode = context.getArgument("ALL | SPAWN | AGGRO", AllOrSpawnOrAggro.class);
        if (mode == AllOrSpawnOrAggro.SPAWN || mode == AllOrSpawnOrAggro.ALL) {
            message.append(multiPlayerCommandOutputHelper(context,
                                        "Spawn Difficulties:\n", PlayerMixinAccessor::getSpawnDifficulty));
        }
        if (mode == AllOrSpawnOrAggro.AGGRO || mode == AllOrSpawnOrAggro.ALL) {
            message.append(multiPlayerCommandOutputHelper(context,
                              "Aggression Difficulties:\n", PlayerMixinAccessor::getAggressionDifficulty));
        }
        context.getSource().sendSuccess(() -> Component.literal(message.toString()), false);

        return 1;
    }

    private static String multiPlayerCommandOutputHelper(CommandContext<CommandSourceStack> context,
                                                           String messageHeader,
                                                           Function<PlayerMixinAccessor, Integer> getterFunction) throws
                                                                                                CommandSyntaxException {
        Collection<ServerPlayer> players = EntityArgument.getPlayers(context, "Player(s)");

        // Compose message in format "Player: Value"
        StringBuilder messageBuilder = new StringBuilder(messageHeader);
        for (ServerPlayer player : players) {
            messageBuilder.append(player.getName().getString()).append(": ").append(getterFunction.apply(
                                                                            (PlayerMixinAccessor) player)).append("\n");
        }
        return messageBuilder.toString();
    }
}
