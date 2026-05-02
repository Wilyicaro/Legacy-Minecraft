package wily.legacy.mixin.base;

import net.minecraft.core.BlockPos;
import net.minecraft.world.Difficulty;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.WitherSkullBlock;
import net.minecraft.world.level.block.entity.SkullBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.pattern.BlockPattern;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import wily.legacy.mobcaps.ConsoleMobCaps;
import wily.legacy.util.LegacyComponents;

@Mixin(WitherSkullBlock.class)
public abstract class WitherSkullBlockMixin {
    @Shadow
    private static BlockPattern getOrCreateWitherFull() {
        return null;
    }

    @Inject(method = "setPlacedBy", at = @At("HEAD"))
    public void setPlacedBy(Level level, BlockPos blockPos, BlockState blockState, LivingEntity livingEntity, ItemStack itemStack, CallbackInfo ci) {
        if (!level.isClientSide()) {
            if (level.getDifficulty() == Difficulty.PEACEFUL && livingEntity instanceof Player player && (blockState.is(Blocks.WITHER_SKELETON_SKULL) || blockState.is(Blocks.WITHER_SKELETON_WALL_SKULL)) && getOrCreateWitherFull().find(level, blockPos) != null) {
                player.sendOverlayMessage(LegacyComponents.PEACEFUL_SPAWN_TIP);
            }
        }
    }

    @Redirect(
        method = "checkSpawn(Lnet/minecraft/world/level/Level;Lnet/minecraft/core/BlockPos;Lnet/minecraft/world/level/block/entity/SkullBlockEntity;)V",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/world/entity/EntityType;create(Lnet/minecraft/world/level/Level;Lnet/minecraft/world/entity/EntitySpawnReason;)Lnet/minecraft/world/entity/Entity;"
        )
    )
    private static Entity checkSpawn(EntityType<?> type, Level level, EntitySpawnReason reason, Level world, BlockPos pos, SkullBlockEntity skullBlockEntity) {
        if (world instanceof ServerLevel serverLevel && !ConsoleMobCaps.canTriggerSummon(serverLevel, type)) {
            return null;
        }

        return type.create(level, reason);
    }
}
