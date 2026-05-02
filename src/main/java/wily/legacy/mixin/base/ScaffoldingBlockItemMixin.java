package wily.legacy.mixin.base;

import com.llamalad7.mixinextras.injector.v2.WrapWithCondition;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ScaffoldingBlockItem;
import net.minecraft.world.item.context.BlockPlaceContext;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(ScaffoldingBlockItem.class)
public class ScaffoldingBlockItemMixin {
    @WrapWithCondition(method = "updatePlacementContext", at = @At(value = "INVOKE", target = "Lnet/minecraft/server/level/ServerPlayer;sendBuildLimitMessage(ZI)V"))
    public boolean updatePlacementContext(ServerPlayer instance, boolean showOverlay, int maxY, BlockPlaceContext context) {
        return !instance.level().isOutsideBuildHeight(context.getClickedPos());
    }
}
