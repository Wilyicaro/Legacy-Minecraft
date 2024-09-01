package wily.legacy.mixin;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.inventory.CyclingSlotBackground;
import net.minecraft.client.renderer.texture.TextureAtlas;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.inventory.Slot;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import wily.legacy.client.screen.LegacyIconHolder;
import wily.legacy.inventory.LegacySlotDisplay;
import wily.legacy.util.ScreenUtil;

@Mixin(CyclingSlotBackground.class)
public class CyclingSlotBackgroundMixin {
    @Inject(method = "renderIcon", at = @At("HEAD"), cancellable = true)
    private void renderIcon(Slot slot, ResourceLocation resourceLocation, float f, PoseStack poseStack, int i, int j, CallbackInfo ci) {
        if (slot instanceof LegacySlotDisplay) ci.cancel();
        else return;
        LegacyIconHolder holder = ScreenUtil.iconHolderRenderer.slotBounds(slot);
        poseStack.pose().pushPose();
        poseStack.pose().translate(i + slot.x,j + slot.y,0);
        poseStack.pose().scale(holder.getSelectableWidth() / 16f,holder.getSelectableHeight() / 16f,holder.getSelectableHeight() / 16f);
        TextureAtlasSprite textureAtlasSprite = Minecraft.getInstance().getTextureAtlas(TextureAtlas.LOCATION_BLOCKS).apply(resourceLocation);
        poseStack.blit(0, 0, 0, 16, 16, textureAtlasSprite, 1.0f, 1.0f, 1.0f, f);
        poseStack.pose().popPose();
    }
}
