package wily.legacy.mixin.base;

import net.minecraft.world.InteractionHand;
//? <=1.21.1 {
import net.minecraft.world.InteractionResultHolder;
//?} else {
/*import net.minecraft.world.InteractionResult;
*///?}
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.*;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;
import wily.factoryapi.base.config.FactoryConfig;
import wily.legacy.config.LegacyCommonOptions;

@Mixin(SwordItem.class)
public class SwordItemMixin extends Item {


    public SwordItemMixin(Properties properties) {
        super(properties);
    }

    @Override
    public int getUseDuration(ItemStack itemStack/*? if >=1.20.5 {*/, LivingEntity livingEntity/*?}*/) {
        if (FactoryConfig.hasCommonConfigEnabled(LegacyCommonOptions.legacySwordBlocking)) return 7200;
        return super.getUseDuration(itemStack/*? if >=1.20.5 {*/, livingEntity/*?}*/);
    }

    //? <1.21.2 {
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand interactionHand) {
        if (FactoryConfig.hasCommonConfigEnabled(LegacyCommonOptions.legacySwordBlocking) && !player.getOffhandItem().getUseAnimation().equals(UseAnim.BLOCK)) {
            player.startUsingItem(interactionHand);
            return InteractionResultHolder.consume( player.getItemInHand(interactionHand));
        }
        return super.use(level, player, interactionHand);
    }
    //?} else {
    /*public InteractionResult use(Level level, Player player, InteractionHand interactionHand) {
        if (FactoryConfig.hasCommonConfigEnabled(LegacyCommonOptions.legacySwordBlocking) && !player.getOffhandItem().getUseAnimation().equals(ItemUseAnimation.BLOCK)) {
            player.startUsingItem(interactionHand);
            return InteractionResult.CONSUME;
        }
        return super.use(level, player, interactionHand);
    }
    *///?}

    @Override
    public /*? if <1.21.2 {*/UseAnim/*?} else {*//*ItemUseAnimation*//*?}*/ getUseAnimation(ItemStack itemStack) {
        if (FactoryConfig.hasCommonConfigEnabled(LegacyCommonOptions.legacySwordBlocking)) return /*? if <1.21.2 {*/UseAnim/*?} else {*//*ItemUseAnimation*//*?}*/.BLOCK;
        return super.getUseAnimation(itemStack);
    }
}
