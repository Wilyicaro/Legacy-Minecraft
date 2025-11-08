package wily.legacy.mixin.base;

import net.minecraft.tags.ItemTags;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
//? if <1.21.2 {
/*import net.minecraft.world.item.UseAnim;
import net.minecraft.world.InteractionResultHolder;
*///?} else {
import net.minecraft.world.item.ItemUseAnimation;
//?}
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import wily.factoryapi.base.config.FactoryConfig;
import wily.legacy.config.LegacyCommonOptions;
//? if <1.20.5 {
/*import wily.legacy.util.ItemAccessor;
 *///?}

@Mixin(Item.class)
public class ItemMixin /*? if <1.20.5 {*//*implements ItemAccessor*//*?}*/ {
    //? if <1.20.5 {
    /*@Mutable @Shadow @Final private int maxStackSize;

    @Override
    public void setMaxStackSize(int i) {
        maxStackSize = i;
    }

    @Override
    public void setRecordLengthInTicks(int i) {
    }
    *///?}

    @Unique
    private boolean isSword() {
        return ((Item) (Object) this).builtInRegistryHolder().is(ItemTags.SWORDS);
    }

    @Inject(method = "getUseDuration", at = @At("HEAD"), cancellable = true)
    public void getUseDuration(CallbackInfoReturnable<Integer> cir) {
        if (isSword() && FactoryConfig.hasCommonConfigEnabled(LegacyCommonOptions.legacySwordBlocking))
            cir.setReturnValue(7200);
    }

    //? <1.21.2 {
    /*@Inject(method = "use", at = @At("HEAD"), cancellable = true)
    public void use(Level level, Player player, InteractionHand interactionHand, CallbackInfoReturnable<InteractionResultHolder<ItemStack>> cir) {
        if (isSword() && FactoryConfig.hasCommonConfigEnabled(LegacyCommonOptions.legacySwordBlocking) && (interactionHand == InteractionHand.OFF_HAND || !player.getOffhandItem().getUseAnimation().equals(UseAnim.BLOCK))) {
            player.startUsingItem(interactionHand);
            cir.setReturnValue(InteractionResultHolder.consume(player.getItemInHand(interactionHand)));
        }
    }
    *///?} else {
    @Inject(method = "use", at = @At("HEAD"), cancellable = true)
    public void use(Level level, Player player, InteractionHand interactionHand, CallbackInfoReturnable<InteractionResult> cir) {
        if (isSword() && FactoryConfig.hasCommonConfigEnabled(LegacyCommonOptions.legacySwordBlocking) && (interactionHand == InteractionHand.OFF_HAND || !player.getOffhandItem().getUseAnimation().equals(ItemUseAnimation.BLOCK))) {
            player.startUsingItem(interactionHand);
            cir.setReturnValue(InteractionResult.CONSUME);
        }
    }
    //?}

    @Inject(method = "getUseAnimation", at = @At("HEAD"), cancellable = true)
    public void getUseAnimation(ItemStack itemStack, CallbackInfoReturnable</*? if <1.21.2 {*//*UseAnim*//*?} else {*/ItemUseAnimation/*?}*/> cir) {
        if (isSword() && FactoryConfig.hasCommonConfigEnabled(LegacyCommonOptions.legacySwordBlocking))
            cir.setReturnValue(/*? if <1.21.2 {*//*UseAnim*//*?} else {*/ItemUseAnimation/*?}*/.BLOCK);
    }
}
