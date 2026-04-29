package wily.legacy.mixin.base.client.inventory;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.navigation.ScreenRectangle;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.gui.screens.inventory.EffectsInInventory;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import wily.legacy.client.screen.LegacyMenuAccess;
import wily.legacy.util.client.LegacyRenderUtil;

@Mixin(EffectsInInventory.class)
public abstract class EffectRenderingInventoryScreenMixin {
    @Shadow
    @Final
    private AbstractContainerScreen<?> screen;

    @Inject(method = "renderEffects", at = @At("HEAD"), cancellable = true)
    public void extractRenderState(GuiGraphicsExtractor GuiGraphicsExtractor, int i, int j, CallbackInfo ci) {
        ci.cancel();
        //? if <1.21.2 {
        /*super.extractRenderState(GuiGraphicsExtractor, i, j, f);
        ScreenUtil.renderContainerEffects(GuiGraphicsExtractor, leftPos, topPos, imageWidth, imageHeight, i, j);
        *///?} else {
        if (screen instanceof LegacyMenuAccess<?> a) {
            ScreenRectangle rec = a.getMenuRectangle();
            LegacyRenderUtil.renderContainerEffects(GuiGraphicsExtractor, rec.left(), rec.top(), rec.width(), rec.height(), i, j);
        }
        //?}
    }
}
