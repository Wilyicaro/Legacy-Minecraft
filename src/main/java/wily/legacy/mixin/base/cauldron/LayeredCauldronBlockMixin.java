package wily.legacy.mixin.base.cauldron;

import com.llamalad7.mixinextras.injector.ModifyReceiver;
import net.minecraft.core.BlockPos;
import net.minecraft.core.cauldron.CauldronInteraction;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.BucketItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
//? if <1.20.5 {
/*import net.minecraft.world.item.alchemy.PotionUtils;
*///?}
import net.minecraft.world.level.Level;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.Property;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.phys.BlockHitResult;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import wily.legacy.Legacy4J;
import wily.legacy.block.entity.WaterCauldronBlockEntity;
import wily.legacy.init.LegacyRegistries;
import wily.legacy.util.LegacyTags;

import java.util.Map;

import static wily.legacy.init.LegacyRegistries.isInvalidCauldron;

@Mixin(LayeredCauldronBlock.class)
public abstract class LayeredCauldronBlockMixin extends AbstractCauldronBlock implements EntityBlock {
    public LayeredCauldronBlockMixin(Properties properties, /*? if >1.20.1 {*/CauldronInteraction.InteractionMap/*?} else {*//*Map<Item, CauldronInteraction>*//*?}*/ interactionMap) {
        super(properties, interactionMap);
    }
    public void animateTick(BlockState blockState, Level level, BlockPos blockPos, RandomSource randomSource) {
        if (!blockState.is(LegacyTags.WATER_CAULDRONS)) return;
        level.getBlockEntity(blockPos, LegacyRegistries.WATER_CAULDRON_BLOCK_ENTITY.get()).ifPresent(be->{
            if (be.potion == null || be.hasWater()) return;
            //? if <1.20.5 {
            /*int i = PotionUtils.getColor(be.potion.value());
            double d = (double)(i >> 16 & 0xFF) / 255.0;
            double e = (double)(i >> 8 & 0xFF) / 255.0;
            double f = (double)(i & 0xFF) / 255.0;
            level.addParticle(ParticleTypes.ENTITY_EFFECT, (2.0 * level.random.nextDouble() - 1.0) / 4 + blockPos.getX(), level.random.nextDouble() + blockPos.getY(), (2.0 * level.random.nextDouble() - 1.0) / 4 + blockPos.getZ(), d, e, f);
            *///?} else {
            be.potion.value().getEffects().forEach(m->{
                if (level.random.nextInt(4) == 0) {
                    level.addParticle(m.getParticleOptions(), (2.0 * level.random.nextDouble() - 1.0) / 4 + blockPos.getX(), level.random.nextDouble() + blockPos.getY(), (2.0 * level.random.nextDouble() - 1.0) / 4 + blockPos.getZ(), 1.0f, 1.0f, 1.0f);
                }
            });
            //?}
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
        return blockState.is(LegacyTags.WATER_CAULDRONS) ? new WaterCauldronBlockEntity(blockPos,blockState) : null;
    }
}
