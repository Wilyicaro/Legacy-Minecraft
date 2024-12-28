//? if >=1.21.2 {
package wily.legacy.client;

import net.minecraft.world.entity.projectile.FireworkRocketEntity;
import wily.factoryapi.base.client.FactoryRenderStateExtension;

public class LegacyFireworkRenderState implements FactoryRenderStateExtension<FireworkRocketEntity> {
    public float xRot;
    public float yRot;

    @Override
    public Class<FireworkRocketEntity> getEntityClass() {
        return FireworkRocketEntity.class;
    }

    @Override
    public void extractToRenderState(FireworkRocketEntity entity, float partialTicks) {
        xRot = entity.getXRot(partialTicks);
        yRot = entity.getYRot(partialTicks);
    }
}
//?}
