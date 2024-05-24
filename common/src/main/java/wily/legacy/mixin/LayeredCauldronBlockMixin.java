package wily.legacy.mixin;

import net.minecraft.core.BlockPos;
import net.minecraft.core.cauldron.CauldronInteraction;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.ItemInteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.BucketItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.alchemy.PotionContents;
import net.minecraft.world.item.alchemy.Potions;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.block.AbstractCauldronBlock;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.LayeredCauldronBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.phys.BlockHitResult;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import wily.legacy.block.entity.WaterCauldronBlockEntity;
import wily.legacy.init.LegacyRegistries;

import static wily.legacy.init.LegacyRegistries.isInvalidCauldron;

@Mixin(LayeredCauldronBlock.class)
public abstract class LayeredCauldronBlockMixin extends AbstractCauldronBlock implements EntityBlock {
    public LayeredCauldronBlockMixin(Properties properties, CauldronInteraction.InteractionMap interactionMap) {
        super(properties, interactionMap);
    }
    protected ItemInteractionResult useItemOn(ItemStack itemStack, BlockState blockState, Level level, BlockPos blockPos, Player player, InteractionHand interactionHand, BlockHitResult blockHitResult) {
        if (LegacyRegistries.isInvalidCauldron(blockState,level,blockPos) && player.getItemInHand(interactionHand).getItem() instanceof BucketItem) return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
        return super.useItemOn(itemStack, blockState, level, blockPos, player, interactionHand, blockHitResult);
    }
    public void animateTick(BlockState blockState, Level level, BlockPos blockPos, RandomSource randomSource) {
        if (!blockState.is(Blocks.WATER_CAULDRON)) return;
        level.getBlockEntity(blockPos, LegacyRegistries.WATER_CAULDRON_BLOCK_ENTITY.get()).ifPresent(be->{
            if (be.potion == null || be.potion == Potions.WATER) return;
            be.potion.value().getEffects().forEach(m->{
                if (level.random.nextInt(5) == 0) {
                    level.addParticle(m.getParticleOptions(), (2.0 * level.random.nextDouble() - 1.0) / 4 + blockPos.getX(), level.random.nextDouble() + blockPos.getY(), (2.0 * level.random.nextDouble() - 1.0) / 4 + blockPos.getZ(), 1.0f, 1.0f, 1.0f);
                }
            });

        });
    }
    @Inject(method = "handlePrecipitation", at = @At("HEAD"), cancellable = true)
    public void handlePrecipitation(BlockState blockState, Level level, BlockPos blockPos, Biome.Precipitation precipitation, CallbackInfo ci) {
        if (isInvalidCauldron(blockState,level,blockPos)) ci.cancel();
    }
    @Inject(method = "receiveStalactiteDrip", at = @At("HEAD"), cancellable = true)
    public void receiveStalactiteDrip(BlockState blockState, Level level, BlockPos blockPos, Fluid fluid, CallbackInfo ci) {
        if (isInvalidCauldron(blockState,level,blockPos)) ci.cancel();
    }
    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos blockPos, BlockState blockState) {
        return blockState.is(Blocks.WATER_CAULDRON) ? new WaterCauldronBlockEntity(blockPos,blockState) : null;
    }
}
