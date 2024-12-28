//? if >=1.21.2 {
package wily.legacy.client;

import net.minecraft.world.entity.npc.AbstractVillager;
import wily.factoryapi.base.client.FactoryRenderStateExtension;

public class LegacyVillagerRenderState implements FactoryRenderStateExtension<AbstractVillager> {
    public boolean isTrading;

    @Override
    public Class<AbstractVillager> getEntityClass() {
        return AbstractVillager.class;
    }

    @Override
    public void extractToRenderState(AbstractVillager entity, float partialTicks) {
        isTrading = entity.isTrading();
    }
}
//?}
