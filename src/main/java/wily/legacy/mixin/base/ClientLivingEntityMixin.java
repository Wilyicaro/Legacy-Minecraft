package wily.legacy.mixin.base;

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
import wily.legacy.Legacy4J;
import wily.legacy.util.ScreenUtil;

@Mixin(LivingEntity.class)
public abstract class ClientLivingEntityMixin extends Entity {
    public ClientLivingEntityMixin(EntityType<?> arg, Level arg2) {
        super(arg, arg2);
    }

    @Inject(method = "onEquipItem", at = @At("HEAD"))
    public void onEquipItem(EquipmentSlot arg, ItemStack itemStack, ItemStack itemStack2, CallbackInfo ci) {
        if (!FactoryItemUtil.equalItems(itemStack, itemStack2) && !this.firstTick){
            ScreenUtil.updateAnimatedCharacterTime(1500);
        }
    }
}
