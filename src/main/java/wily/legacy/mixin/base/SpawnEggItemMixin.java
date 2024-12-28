package wily.legacy.mixin.base;

import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.item.SpawnEggItem;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(SpawnEggItem.class)
public abstract class SpawnEggItemMixin {
    @Redirect(method = "spawnOffspringFromSpawnEgg", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/Mob;setCustomName(Lnet/minecraft/network/chat/Component;)V"))
    public void spawnOffspringFromSpawnEgg(Mob instance, Component component) {
        instance.setCustomName(component);
        instance.setPersistenceRequired();
    }
}
