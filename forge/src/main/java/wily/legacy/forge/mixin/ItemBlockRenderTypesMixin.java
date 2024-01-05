package wily.legacy.forge.mixin;

import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.client.RenderTypeHelper;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import wily.legacy.LegacyMinecraftClient;

@Mixin(RenderTypeHelper.class)
public class ItemBlockRenderTypesMixin {
    @Inject(method = "getFallbackItemRenderType", at = @At("HEAD"), cancellable = true, remap = false)
    private static void getRenderType(ItemStack stack, BakedModel model, boolean cull, CallbackInfoReturnable<RenderType> cir) {
        boolean blockItem = stack.getItem() instanceof BlockItem;
        if (!blockItem && LegacyMinecraftClient.itemRenderTypeOverride != null && !cull) cir.setReturnValue(LegacyMinecraftClient.itemRenderTypeOverride);
        if ((blockItem || cull) && LegacyMinecraftClient.blockItemRenderTypeOverride != null ) cir.setReturnValue(LegacyMinecraftClient.blockItemRenderTypeOverride);
    }
}
