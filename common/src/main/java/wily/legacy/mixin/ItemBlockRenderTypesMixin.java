package wily.legacy.mixin;

import net.minecraft.client.renderer.ItemBlockRenderTypes;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import wily.legacy.LegacyMinecraftClient;

@Mixin(ItemBlockRenderTypes.class)
public class ItemBlockRenderTypesMixin {
    @Inject(method = "getRenderType(Lnet/minecraft/world/item/ItemStack;Z)Lnet/minecraft/client/renderer/RenderType;", at = @At("HEAD"), cancellable = true)
    private static void getRenderType(ItemStack itemStack, boolean bl, CallbackInfoReturnable<RenderType> cir) {
        boolean blockItem = itemStack.getItem() instanceof BlockItem;
        if (!blockItem && LegacyMinecraftClient.itemRenderTypeOverride != null && !bl) cir.setReturnValue(LegacyMinecraftClient.itemRenderTypeOverride);
        if ((blockItem || bl) && LegacyMinecraftClient.blockItemRenderTypeOverride != null ) cir.setReturnValue(LegacyMinecraftClient.blockItemRenderTypeOverride);
    }
}
