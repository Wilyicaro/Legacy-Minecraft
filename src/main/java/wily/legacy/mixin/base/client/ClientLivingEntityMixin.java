package wily.legacy.mixin.base.client;

import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import wily.factoryapi.util.FactoryItemUtil;
import wily.legacy.client.AnimatedCharacterRenderer;

@Mixin(LivingEntity.class)
public abstract class ClientLivingEntityMixin extends Entity {
    public ClientLivingEntityMixin(EntityType<?> arg, Level arg2) {
        super(arg, arg2);
    }

    @Inject(method = "onEquipItem", at = @At("HEAD"))
    public void onEquipItem(EquipmentSlot arg, ItemStack itemStack, ItemStack itemStack2, CallbackInfo ci) {
        if (((Entity)this) == Minecraft.getInstance().player && !FactoryItemUtil.equalItems(itemStack, itemStack2) && !this.firstTick){
            AnimatedCharacterRenderer.updateTime(1500);
        }
    }
}
