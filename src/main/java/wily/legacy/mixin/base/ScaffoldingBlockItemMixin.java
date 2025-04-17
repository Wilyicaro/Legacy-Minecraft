package wily.legacy.mixin.base;

import com.llamalad7.mixinextras.injector.v2.WrapWithCondition;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ScaffoldingBlockItem;
import net.minecraft.world.item.context.BlockPlaceContext;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(ScaffoldingBlockItem.class)
public class ScaffoldingBlockItemMixin {
    @WrapWithCondition(method = "updatePlacementContext", at = @At(value = "INVOKE", target = "Lnet/minecraft/server/level/ServerPlayer;sendSystemMessage(Lnet/minecraft/network/chat/Component;Z)V"))
    public boolean updatePlacementContext(ServerPlayer instance, Component arg, boolean bl, BlockPlaceContext context) {
        return !instance.level().isOutsideBuildHeight(context.getClickedPos());
    }
}
