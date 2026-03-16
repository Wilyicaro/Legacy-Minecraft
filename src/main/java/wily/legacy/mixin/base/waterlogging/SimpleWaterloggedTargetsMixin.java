package wily.legacy.mixin.base.waterlogging;

import net.minecraft.core.BlockPos;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.AbstractCauldronBlock;
import net.minecraft.world.level.block.AbstractBannerBlock;
import net.minecraft.world.level.block.CarpetBlock;
import net.minecraft.world.level.block.EndRodBlock;
import net.minecraft.world.level.block.ShulkerBoxBlock;
import org.spongepowered.asm.mixin.Mixin;
import wily.legacy.util.LegacySimpleWaterloggedBlock;

@Mixin({AbstractBannerBlock.class, AbstractCauldronBlock.class, CarpetBlock.class, EndRodBlock.class, ShulkerBoxBlock.class})
public abstract class SimpleWaterloggedTargetsMixin implements LegacySimpleWaterloggedBlock {
}
