package wily.legacy.mixin.base;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
//? if >=1.20.5 && <1.21.2 {
import net.minecraft.world.ItemInteractionResult;
//?}
import net.minecraft.world.entity.Entity;
//? if >=1.21.5 {
/*import net.minecraft.world.entity.InsideBlockEffectApplier;
*///?}
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.CampfireBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import wily.factoryapi.base.config.FactoryConfig;
import wily.legacy.config.LegacyCommonOptions;

@Mixin(CampfireBlock.class)
public class CampfireBlockMixin {
    @Inject(method = "entityInside", at = @At("HEAD"))
    private void legacy$entityInside(BlockState blockState, Level level, BlockPos blockPos, Entity entity, /*? if >=1.21.5 {*//*InsideBlockEffectApplier insideBlockEffectApplier, *//*?}*//*? if >=1.21.6 {*//*boolean bl, *//*?}*/CallbackInfo ci) {
        if (!FactoryConfig.hasCommonConfigEnabled(LegacyCommonOptions.legacyWorldInteractions)) {
            return;
        }
        if (level instanceof ServerLevel serverLevel && entity instanceof LivingEntity && entity.isOnFire() && entity.mayInteract(serverLevel, blockPos) && CampfireBlock.canLight(blockState)) {
            level.setBlock(blockPos, blockState.setValue(CampfireBlock.LIT, true), 11);
        }
    }

    @Inject(method = /*? if <1.20.5 {*//*"use"*//*?} else {*/"useItemOn"/*?}*/, at = @At("RETURN"))
    private void legacy$playFoodPlacementSound(/*? if <1.20.5 {*//*?} else {*/ItemStack itemStack, /*?}*/BlockState blockState, Level level, BlockPos blockPos, Player player, InteractionHand interactionHand, BlockHitResult blockHitResult, CallbackInfoReturnable</*? if <1.20.5 || >=1.21.2 {*//*InteractionResult*//*?} else {*/ItemInteractionResult/*?}*/> cir) {
        if (FactoryConfig.hasCommonConfigEnabled(LegacyCommonOptions.legacyAudio) && legacy$placedFood(cir.getReturnValue())) {
            level.playSound(null, blockPos, SoundEvents.ITEM_FRAME_ADD_ITEM, SoundSource.BLOCKS, 1.0f, 1.0f);
        }
    }

    private static boolean legacy$placedFood(/*? if <1.20.5 || >=1.21.2 {*//*InteractionResult*//*?} else {*/ItemInteractionResult/*?}*/ result) {
        //? if <1.20.5 || >=1.21.2 {
        /*return result.consumesAction();
        *///?} else {
        return result == ItemInteractionResult.SUCCESS || result == ItemInteractionResult.CONSUME || result == ItemInteractionResult.CONSUME_PARTIAL;
        //?}
    }
}
