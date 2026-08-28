//? if <=1.20.1 {
package wily.legacy.util;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.ai.behavior.Behavior;
import net.minecraft.world.entity.ai.behavior.BehaviorControl;
import net.minecraft.world.entity.npc.Villager;
import net.minecraft.world.entity.npc.VillagerData;
import net.minecraft.world.entity.npc.VillagerProfession;

public final class SpawnEggProfessionBehavior {
    private SpawnEggProfessionBehavior() {
    }

    public static BehaviorControl<Villager> assign(BehaviorControl<Villager> behavior) {
        return new Assign(behavior);
    }

    public static BehaviorControl<Villager> reset(BehaviorControl<Villager> behavior) {
        return new Reset(behavior);
    }

    private abstract static class Wrapper implements BehaviorControl<Villager> {
        protected final BehaviorControl<Villager> behavior;

        private Wrapper(BehaviorControl<Villager> behavior) {
            this.behavior = behavior;
        }

        @Override
        public Behavior.Status getStatus() {
            return behavior.getStatus();
        }

        @Override
        public void tickOrStop(ServerLevel level, Villager villager, long time) {
            behavior.tickOrStop(level, villager, time);
        }

        @Override
        public void doStop(ServerLevel level, Villager villager, long time) {
            behavior.doStop(level, villager, time);
        }

        @Override
        public String debugString() {
            return behavior.debugString();
        }
    }

    private static final class Assign extends Wrapper {
        private Assign(BehaviorControl<Villager> behavior) {
            super(behavior);
        }

        @Override
        public boolean tryStart(ServerLevel level, Villager villager, long time) {
            VillagerProfession originalProfession = villager.getVillagerData().getProfession();
            boolean replaceProfession = originalProfession != VillagerProfession.NONE && villager.getVillagerXp() == 0 && villager.getTags().contains("legacy_spawn_egg_profession");
            if (replaceProfession) {
                villager.setVillagerData(villager.getVillagerData().setProfession(VillagerProfession.NONE));
            }
            boolean started = behavior.tryStart(level, villager, time);
            VillagerData data = villager.getVillagerData();
            if (replaceProfession && data.getProfession() == VillagerProfession.NONE) {
                villager.setVillagerData(data.setProfession(originalProfession));
            } else if ((replaceProfession || originalProfession == VillagerProfession.NONE) && data.getProfession() != VillagerProfession.NONE) {
                villager.playSound(villager.getNotifyTradeSound(), 1.0f, villager.getVoicePitch());
                villager.removeTag("legacy_spawn_egg_profession");
            }
            return started;
        }
    }

    private static final class Reset extends Wrapper {
        private Reset(BehaviorControl<Villager> behavior) {
            super(behavior);
        }

        @Override
        public boolean tryStart(ServerLevel level, Villager villager, long time) {
            return !villager.getTags().contains("legacy_spawn_egg_profession") && behavior.tryStart(level, villager, time);
        }
    }
}
//?}
