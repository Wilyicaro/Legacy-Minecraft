package wily.legacy.mixin.base;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionResult;
//? if <1.20.5 {
/*import net.minecraft.world.InteractionHand;
*///?}
import net.minecraft.world.entity.player.Player;
//? if <1.21.11 {
import net.minecraft.world.item.ItemStack;
//?}
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
//? if >=1.21.11 {
/*import net.minecraft.world.level.block.Block;
*///?}
import net.minecraft.world.level.block.SweetBerryBushBlock;
//? if >=1.21.11 {
/*import net.minecraft.world.level.block.VegetationBlock;
*///?}
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.gameevent.GameEvent;
//? if >=1.21.11 {
/*import net.minecraft.world.level.storage.loot.BuiltInLootTables;
*///?}
import net.minecraft.world.phys.BlockHitResult;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import wily.factoryapi.base.config.FactoryConfig;
import wily.legacy.config.LegacyCommonOptions;

@Mixin(SweetBerryBushBlock.class)
public abstract class SweetBerryBushBlockMixin /*? if >=1.21.11 {*//*extends VegetationBlock *//*?}*/ {
    //? if >=1.21.11 {
    /*protected SweetBerryBushBlockMixin(Properties properties) {
        super(properties);
    }
    *///?}

    @Inject(method = /*? if <1.20.5 {*//*"use"*//*?} else {*/"useWithoutItem"/*?}*/, at = @At("HEAD"), cancellable = true)
    private void legacy$useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, /*? if <1.20.5 {*//*InteractionHand interactionHand, *//*?}*/BlockHitResult hitResult, CallbackInfoReturnable<InteractionResult> cir) {
        int age = state.getValue(SweetBerryBushBlock.AGE);
        //? if <1.20.5 {
        /*if (player.getMainHandItem().is(Items.BONE_MEAL) || player.getOffhandItem().is(Items.BONE_MEAL)) {
            cir.setReturnValue(InteractionResult.PASS);
            return;
        }
        *///?}
        if (age >= 3 && (player.getMainHandItem().is(Items.BONE_MEAL) || player.getOffhandItem().is(Items.BONE_MEAL))) {
            cir.setReturnValue(InteractionResult.PASS);
            return;
        }
        if (!FactoryConfig.hasCommonConfigEnabled(LegacyCommonOptions.legacyWorldInteractions) || age <= 1) {
            return;
        }

        if (level instanceof ServerLevel serverLevel) {
            //? if >=1.21.11 {
            /*Block.dropFromBlockInteractLootTable(
                    serverLevel,
                    BuiltInLootTables.HARVEST_SWEET_BERRY_BUSH,
                    state,
                    level.getBlockEntity(pos),
                    null,
                    player,
                    (serverlvl, stack) -> player.getInventory().placeItemBackInInventory(stack)
            );
            *///?} else {
            int count = 1 + level.random.nextInt(2);
            if (age >= 3) {
                count++;
            }
            player.getInventory().placeItemBackInInventory(new ItemStack(Items.SWEET_BERRIES, count));
            //?}
            serverLevel.playSound(null, pos, SoundEvents.SWEET_BERRY_BUSH_PICK_BERRIES, SoundSource.BLOCKS, 1.0F, 0.8F + serverLevel.getRandom().nextFloat() * 0.4F);
            BlockState newState = state.setValue(SweetBerryBushBlock.AGE, 1);
            serverLevel.setBlock(pos, newState, 2);
            serverLevel.gameEvent(GameEvent.BLOCK_CHANGE, pos, GameEvent.Context.of(player, newState));
        }

        cir.setReturnValue(InteractionResult.SUCCESS);
    }
}
