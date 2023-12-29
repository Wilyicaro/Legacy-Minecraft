package wily.legacy.mixin;

import net.minecraft.core.BlockPos;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.CompoundContainer;
import net.minecraft.world.Container;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ChestMenu;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.entity.ChestBlockEntity;
import net.minecraft.world.level.block.entity.ContainerOpenersCounter;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Mutable;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import wily.legacy.inventory.LegacyChestMenu;

@Mixin(ChestBlockEntity.class)
public abstract class ChestBlockEntityMixin {
    @Mutable
    @Shadow @Final private ContainerOpenersCounter openersCounter;

    @Shadow protected abstract void signalOpenCount(Level arg, BlockPos arg2, BlockState arg3, int i, int j);

    @Shadow
    static void playSound(Level arg, BlockPos arg2, BlockState arg3, SoundEvent arg4) {
    }
    ChestBlockEntity self(){
        return (ChestBlockEntity) (Object)this;
    }

    @Inject(method = "<init>(Lnet/minecraft/world/level/block/entity/BlockEntityType;Lnet/minecraft/core/BlockPos;Lnet/minecraft/world/level/block/state/BlockState;)V", at = @At("RETURN"))
    protected void init(BlockEntityType<?> blockEntityType, BlockPos blockPos, BlockState blockState, CallbackInfo ci) {
        this.openersCounter = new ContainerOpenersCounter(){

            @Override
            protected void onOpen(Level level, BlockPos blockPos, BlockState blockState) {
                playSound(level, blockPos, blockState, SoundEvents.CHEST_OPEN);
            }

            @Override
            protected void onClose(Level level, BlockPos blockPos, BlockState blockState) {
                playSound(level, blockPos, blockState, SoundEvents.CHEST_CLOSE);
            }

            @Override
            protected void openerCountChanged(Level level, BlockPos blockPos, BlockState blockState, int i, int j) {
                signalOpenCount(level, blockPos, blockState, i, j);
            }

            @Override
            protected boolean isOwnContainer(Player player) {
                if (player.containerMenu instanceof LegacyChestMenu c) {
                    Container container = c.getContainer();
                    return container == self() || container instanceof CompoundContainer && ((CompoundContainer)container).contains(self());
                }
                return false;
            }
        };
    }
    @Inject(method = "createMenu", at = @At("HEAD"), cancellable = true)
    protected void createMenu(int i, Inventory inventory, CallbackInfoReturnable<AbstractContainerMenu> cir) {
        cir.setReturnValue(LegacyChestMenu.threeRows(i, inventory, (Container) this));
    }
}
