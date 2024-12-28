package wily.legacy.mixin.base;

import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.CyclingSlotBackground;
import net.minecraft.client.renderer.texture.TextureAtlas;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.inventory.Slot;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import wily.factoryapi.base.client.FactoryGuiGraphics;
import wily.legacy.client.screen.LegacyIconHolder;
import wily.legacy.inventory.LegacySlotDisplay;
import wily.legacy.util.ScreenUtil;

@Mixin(CyclingSlotBackground.class)
public class CyclingSlotBackgroundMixin {
    @Inject(method = "renderIcon", at = @At("HEAD"), cancellable = true)
    private void renderIcon(Slot slot, ResourceLocation resourceLocation, float f, GuiGraphics guiGraphics, int i, int j, CallbackInfo ci) {
        if (slot instanceof LegacySlotDisplay) ci.cancel();
        else return;
        LegacyIconHolder holder = ScreenUtil.iconHolderRenderer.slotBounds(slot);
        holder.renderScaled(guiGraphics, i + slot.x,j + slot.y, ()->{
            RenderSystem.enableBlend();
            FactoryGuiGraphics.of(guiGraphics).setColor(1.0f,1.0f,1.0f,f);
            //? if <1.21.4 {
            /*TextureAtlasSprite textureAtlasSprite = Minecraft.getInstance().getTextureAtlas(TextureAtlas.LOCATION_BLOCKS).apply(resourceLocation);
            FactoryGuiGraphics.of(guiGraphics).blit(0, 0, 0, 16, 16, textureAtlasSprite);
            *///?} else {
            FactoryGuiGraphics.of(guiGraphics).blitSprite(resourceLocation, 0, 0, 16, 16);
            //?}
            FactoryGuiGraphics.of(guiGraphics).clearColor();
            RenderSystem.disableBlend();
        });
    }
}
