package net.smmeyers.stagedspawnsaggro.mixin;

import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.ai.targeting.TargetingConditions;
import net.minecraft.world.entity.player.Player;
import net.smmeyers.stagedspawnsaggro.PlayerMixinAccessor;
import net.smmeyers.stagedspawnsaggro.StagedSpawnsAndAggression;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import javax.annotation.Nullable;
import java.util.Collection;
import java.util.function.Predicate;

import static net.smmeyers.stagedspawnsaggro.Config.aggroRestrictedMobs;

@Mixin(NearestAttackableTargetGoal.class)
public class NearestAttackableTargetGoalMixin
{
    @Shadow protected TargetingConditions targetConditions;

    @Inject(method = "<init>(Lnet/minecraft/world/entity/Mob;Ljava/lang/Class;IZZLjava/util/function/Predicate;)V", at = @At("RETURN"))
    private <T extends LivingEntity> void onNearestAttackableTargetGoalConstructed(Mob pMob,
                                                                                   Class<T> pTargetType,
                                                                                   int pRandomInterval,
                                                                                   boolean pMustSee,
                                                                                   boolean pMustReach,
                                                                                   @Nullable Predicate<LivingEntity> pTargetPredicate,
                                                                                   CallbackInfo ci) {
        if (pTargetType == Player.class && aggroRestrictedMobs.stream().flatMap(Collection::stream).toList().contains(pMob.getType())) {
            // Wrap pTargetPredicate with our own
            this.targetConditions.selector((LivingEntity pLivingEntity) -> {
                boolean originalPredicateTested = pTargetPredicate == null || pTargetPredicate.test(pLivingEntity);
                if (pLivingEntity instanceof PlayerMixinAccessor player) {
                    if (StagedSpawnsAndAggression.mobListHasRestrictedMob(player, aggroRestrictedMobs, pMob.getType())) {
                        return false;
                    }
                }
                return originalPredicateTested;
            });
        }
    }
}
