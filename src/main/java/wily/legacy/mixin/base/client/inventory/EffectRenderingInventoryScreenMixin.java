package wily.legacy.mixin.base.client.inventory;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.navigation.ScreenRectangle;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
//? if <1.21.2 {
/*import net.minecraft.client.gui.screens.inventory.EffectRenderingInventoryScreen;
 *///?} else {
import net.minecraft.client.gui.screens.inventory.EffectsInInventory;
//?}
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import wily.legacy.client.screen.LegacyMenuAccess;
import wily.legacy.util.client.LegacyRenderUtil;

@Mixin(/*? if <1.21.2 {*//*EffectRenderingInventoryScreen*//*?} else {*/EffectsInInventory/*?}*/.class)
public abstract class EffectRenderingInventoryScreenMixin /*? if <1.21.2 {*//*extends AbstractContainerScreen*//*?}*/ {
    //? if <1.21.2 {
    /*public EffectRenderingInventoryScreenMixin(AbstractContainerMenu abstractContainerMenu, Inventory inventory, Component component) {
        super(abstractContainerMenu, inventory, component);
    }
    *///?} else {
    @Shadow
    @Final
    private AbstractContainerScreen<?> screen;

    //?}
    @Inject(method = "renderEffects", at = @At("HEAD"), cancellable = true)
    public void render(GuiGraphics guiGraphics, int i, int j, CallbackInfo ci) {
        ci.cancel();
        //? if <1.21.2 {
        /*super.render(guiGraphics, i, j, f);
        ScreenUtil.renderContainerEffects(guiGraphics, leftPos, topPos, imageWidth, imageHeight, i, j);
        *///?} else {
        if (screen instanceof LegacyMenuAccess<?> a) {
            ScreenRectangle rec = a.getMenuRectangle();
            LegacyRenderUtil.renderContainerEffects(guiGraphics, rec.left(), rec.top(), rec.width(), rec.height(), i, j);
        }
        //?}
    }
}
