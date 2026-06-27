package wily.legacy.mixin.base;

import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.TamableAnimal;
import net.minecraft.world.entity.animal.Cat;
//? if <1.21.5 {
import net.minecraft.world.entity.animal.Wolf;
//?} else {
/*import net.minecraft.world.entity.animal.wolf.Wolf;
*///?}
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import wily.factoryapi.base.config.FactoryConfig;
import wily.legacy.Legacy4J;
import wily.legacy.config.LegacyCommonOptions;

@Mixin({Wolf.class, Cat.class})
public class TamableAnimalCollarDyeMixin {
    @Inject(method = "mobInteract", at = @At("HEAD"), cancellable = true)
    private void legacy$dyeCollarWithLegacyIngredients(Player player, InteractionHand hand, CallbackInfoReturnable<InteractionResult> cir) {
        if (!FactoryConfig.hasCommonConfigEnabled(LegacyCommonOptions.legacyMobInteractions)) return;
        TamableAnimal animal = (TamableAnimal) (Object) this;
        if (!animal.isTame() || !animal.isOwnedBy(player)) return;
        ItemStack item = player.getItemInHand(hand);
        DyeColor color = Legacy4J.getDyeColorOrNull(item.getItem());
        if (color == null || item.getItem() == Legacy4J.getDyeItem(color) || !legacy$canDyeCollar(color)) return;
        animal.level().playSound(player, animal, SoundEvents.DYE_USE, SoundSource.PLAYERS, 1.0f, 1.0f);
        if (!animal.level().isClientSide()) {
            legacy$setCollarColor(color);
            if (!player.getAbilities().instabuild) item.shrink(1);
        }
        cir.setReturnValue(InteractionResult.SUCCESS);
    }

    @Unique
    private boolean legacy$canDyeCollar(DyeColor color) {
        Object animal = this;
        if (animal instanceof Wolf wolf) return wolf.getCollarColor() != color;
        return animal instanceof Cat cat && cat.getCollarColor() != color;
    }

    @Unique
    private void legacy$setCollarColor(DyeColor color) {
        Object animal = this;
        if (animal instanceof Wolf wolf) ((WolfAccessor) wolf).callSetCollarColor(color);
        else if (animal instanceof Cat cat) ((CatAccessor) cat).callSetCollarColor(color);
    }
}
