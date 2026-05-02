//? if >=1.21.2 {
package wily.legacy.client;

import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemUseAnimation;
import wily.factoryapi.base.client.FactoryRenderStateExtension;
import wily.legacy.Legacy4JClient;

import java.util.UUID;

public class LegacyLivingEntityRenderState implements FactoryRenderStateExtension<LivingEntity> {
    public int itemUseDuration;
    public ItemUseAnimation useAnim;
    public boolean fireImmune;
    public boolean hostInvisible;
    public UUID uuid;

    @Override
    public Class<LivingEntity> getEntityClass() {
        return LivingEntity.class;
    }

    @Override
    public void extractToRenderState(LivingEntity entity, float partialTicks) {
        itemUseDuration = entity.getUseItem().getUseDuration(entity);
        useAnim = entity.getUseItem().getUseAnimation();
        fireImmune = entity.fireImmune();
        hostInvisible = entity instanceof Player player && Legacy4JClient.isHostInvisible(player);
        if (entity instanceof Player player) uuid = player.getGameProfile().id();
    }
}
//?}
