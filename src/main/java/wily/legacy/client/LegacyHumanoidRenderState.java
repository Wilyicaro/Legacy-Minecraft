//? if >=1.21.2 {
package wily.legacy.client;

import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemUseAnimation;
import wily.factoryapi.base.client.FactoryRenderStateExtension;

public class LegacyHumanoidRenderState implements FactoryRenderStateExtension<LivingEntity> {
    public int itemUseDuration;
    public ItemUseAnimation useAnim;

    @Override
    public Class<LivingEntity> getEntityClass() {
        return LivingEntity.class;
    }

    @Override
    public void extractToRenderState(LivingEntity entity, float partialTicks) {
        itemUseDuration = entity.getUseItem().getUseDuration(entity);
        useAnim = entity.getUseItem().getUseAnimation();
    }
}
//?}
