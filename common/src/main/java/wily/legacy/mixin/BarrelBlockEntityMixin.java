package wily.legacy.mixin;

import net.minecraft.core.BlockPos;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.CompoundContainer;
import net.minecraft.world.Container;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BarrelBlockEntity;
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

@Mixin(BarrelBlockEntity.class)
public abstract class BarrelBlockEntityMixin {
    @Mutable
    @Shadow @Final private ContainerOpenersCounter openersCounter;

    @Shadow abstract void playSound(BlockState arg, SoundEvent arg2);

    @Shadow abstract void updateBlockState(BlockState arg, boolean bl);

    BarrelBlockEntity self(){
        return (BarrelBlockEntity) (Object)this;
    }

    @Inject(method = "<init>", at = @At("RETURN"))
    protected void init(BlockPos blockPos, BlockState blockState, CallbackInfo ci) {
        this.openersCounter = new ContainerOpenersCounter(){
            @Override
            protected void onOpen(Level level, BlockPos blockPos, BlockState blockState) {
                playSound(blockState, SoundEvents.BARREL_OPEN);
                updateBlockState(blockState, true);
            }
            @Override
            protected void onClose(Level level, BlockPos blockPos, BlockState blockState) {
                playSound(blockState, SoundEvents.BARREL_CLOSE);
                updateBlockState(blockState, false);
            }
            @Override
            protected void openerCountChanged(Level level, BlockPos blockPos, BlockState blockState, int i, int j) {

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
