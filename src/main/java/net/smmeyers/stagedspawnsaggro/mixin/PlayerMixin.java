package net.smmeyers.stagedspawnsaggro.mixin;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.player.Player;
import net.smmeyers.stagedspawnsaggro.PlayerMixinAccessor;
import net.smmeyers.stagedspawnsaggro.StagedSpawnsAndAggression;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import oshi.util.tuples.Pair;

import java.util.List;
import java.util.Set;

import static net.smmeyers.stagedspawnsaggro.Config.*;
import static net.smmeyers.stagedspawnsaggro.StagedSpawnsAndAggression.StageIncreaseCondition.DAYS_PLAYED;

// TODO: Organize... also probably document this class.

@Mixin(Player.class)
public abstract class PlayerMixin implements PlayerMixinAccessor
{
    @Shadow public abstract void displayClientMessage(Component pChatComponent, boolean pActionBar);

    @Unique private int aggressionDifficulty = 0;
    @Unique private int spawnDifficulty = 0;
    @Unique private int daysPlayed = 0;

    @Override public int getAggressionDifficulty() {
        return this.aggressionDifficulty;
    }
    @Override public int getDaysPlayed() {
        return this.daysPlayed;
    }

    @Override
    public void setAggressionDifficulty(int aggressionDifficulty) {
        this.aggressionDifficulty = aggressionDifficulty;
        stageIncreaseMessage(aggroStageMessages, aggressionDifficulty, aggroRestrictedMobs,
                                                                     "The following mobs can now attack you:\n");
    }

    @Override public int getSpawnDifficulty() { return this.spawnDifficulty; }

    @Override
    public void setSpawnDifficulty(int spawnDifficulty) {
        this.spawnDifficulty = spawnDifficulty;
        stageIncreaseMessage(spawnStageMessages, spawnDifficulty, spawnRestrictedMobs,
                                                                 "The following mobs can now spawn near you:\n");
    }

    private void stageIncreaseMessage(List<String> messageList, int stage, List<Set<? extends EntityType<?>>> mobList,
                                                                                                        String header) {
        // TODO:
        if (printMobs) {
            StringBuilder message = new StringBuilder(header);
            if (stage - 1 < mobList.size() && stage - 1 >= 0) {
                for (EntityType<?> mob : mobList.get(stage - 1)) {
                    message.append(BuiltInRegistries.ENTITY_TYPE.getKey(mob)).append(", ");
                }
            }
            if (!message.isEmpty()) {
                this.displayClientMessage(Component.literal(message.toString()), false);
            }
        }

        // null, out of bounds, and empty string check
        if (messageList == null || messageList.isEmpty() || !(stage - 1 < messageList.size() && stage - 1 >= 0)) {
            return;
        }
        String message = messageList.get(stage - 1);
        if (message != null && !message.isEmpty()) {
            this.displayClientMessage(Component.literal(message), false); // TODO: Consider if putting this in chat rather than action bar would be better. or vice versa
        }
    }

    @Override
    public void setDaysPlayed(int daysPlayed)
    {
        this.daysPlayed = daysPlayed;
        this.setAggressionDifficulty(dayConditionHelper(aggroIncreaseConditions, this.getAggressionDifficulty()));
        this.setSpawnDifficulty(dayConditionHelper(spawnIncreaseConditions, this.getSpawnDifficulty()));
        if (printDay) {
            this.displayClientMessage(Component.literal("Day " + this.getDaysPlayed()), true);
        }
    }

    private int dayConditionHelper(List<Pair<StagedSpawnsAndAggression.StageIncreaseCondition, Object>> stageIncreaseConditions, int currentStage) {
        int stage = currentStage;
        for (int i = 0; i < stageIncreaseConditions.size(); i++) {
            // If the condition is days played, and we've played for longer than the specified amount.
            if (stageIncreaseConditions.get(i).getA().equals(DAYS_PLAYED) && (int) stageIncreaseConditions.get(i).getB() <= this.daysPlayed) {
                // TODO:  Make it possible to decrease spawn aggression difficulty without letting it decrease when it isn't supposed to.
                // Currently, by only setting the difficulty if it's higher than the current difficulty, we can't decrease it,
                // but if we don't do it this way, it may override the difficulty set by other conditions.
                // This only matters for when we want to add conditions that decrease the difficulty. Right now, decreasing it with commands works fine.
                if (currentStage < i + 1) {
                    stage = i + 1;
                }
            }
        }
        return stage;
    }


    @Inject(method = "addAdditionalSaveData", at = @At("HEAD"))
    private void onSave(CompoundTag tag, CallbackInfo ci) {
        tag.putInt("SpawnAggressionDifficulty", this.aggressionDifficulty);
        tag.putInt("DaysPlayed", this.daysPlayed);
    }

    @Inject(method = "readAdditionalSaveData", at = @At("HEAD"))
    private void onLoad(CompoundTag tag, CallbackInfo ci) {
        if (tag.contains("SpawnAggressionDifficulty")) {
            this.aggressionDifficulty = tag.getInt("SpawnAggressionDifficulty");
        }
        if (tag.contains("DaysPlayed")) {
            this.daysPlayed = tag.getInt("DaysPlayed");
        }
    }
}
