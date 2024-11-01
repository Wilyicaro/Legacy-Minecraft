package wily.legacy.entity;

import com.google.common.collect.ImmutableMap;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.ai.behavior.Behavior;
import net.minecraft.world.entity.animal.IronGolem;
import net.minecraft.world.entity.npc.Villager;

import java.util.List;

public class BabyVillagerFollowIronGolemBehavior extends Behavior<Villager> {
    private IronGolem ironGolem;
    private boolean tookGolemRose;
    private int takeGolemRoseTick;

    public BabyVillagerFollowIronGolemBehavior() {
        super(ImmutableMap.of(),60,160);
    }

    @Override
    protected boolean checkExtraStartConditions(ServerLevel serverLevel, Villager villager) {
        if (villager.getAge() >= 0) {
            return false;
        } else if (!serverLevel.isDay()) {
            return false;
        } else {
            List<IronGolem> list = serverLevel.getEntitiesOfClass(IronGolem.class, villager.getBoundingBox().inflate(6.0D, 2.0D, 6.0D));
            if (list.isEmpty()) {
                return false;
            } else {
                for(IronGolem ironGolem : list) {
                    if (ironGolem.getOfferFlowerTick() > 0) {
                        this.ironGolem = ironGolem;
                        break;
                    }
                }

                return this.ironGolem != null;
            }
        }
    }

    @Override
    protected boolean canStillUse(ServerLevel serverLevel, Villager livingEntity, long l) {
        return ironGolem.getOfferFlowerTick() > 0;
    }

    @Override
    protected void start(ServerLevel serverLevel, Villager livingEntity, long l) {
        this.takeGolemRoseTick = serverLevel.random.nextInt(320);
        this.tookGolemRose = false;
        livingEntity.getNavigation().stop();
    }

    @Override
    protected void stop(ServerLevel serverLevel, Villager livingEntity, long l) {
        this.ironGolem = null;
        livingEntity.getNavigation().stop();
    }

    @Override
    protected void tick(ServerLevel serverLevel, Villager livingEntity, long l) {
        livingEntity.getLookControl().setLookAt(this.ironGolem, 30.0F, 30.0F);
        if (this.ironGolem.getOfferFlowerTick() == takeGolemRoseTick) {
            livingEntity.getNavigation().moveTo(this.ironGolem, 0.5D);
            this.tookGolemRose = true;
        }

        if (this.tookGolemRose && livingEntity.distanceTo(this.ironGolem) < 4.0D) {
            this.ironGolem.offerFlower(false);
            livingEntity.getNavigation().stop();
        }
    }
}
