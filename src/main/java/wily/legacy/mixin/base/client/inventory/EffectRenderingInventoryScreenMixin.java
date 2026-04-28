package wily.legacy.mixin.base.client.inventory;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.navigation.ScreenRectangle;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.gui.screens.inventory.EffectsInInventory;
import net.minecraft.world.effect.MobEffectInstance;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import wily.legacy.client.screen.LegacyMenuAccess;
import wily.legacy.util.client.LegacyRenderUtil;

import java.util.Collection;

@Mixin(EffectsInInventory.class)
public abstract class EffectRenderingInventoryScreenMixin {
    @Shadow
    @Final
    private AbstractContainerScreen<?> screen;

    @Inject(method = "renderEffects", at = @At("HEAD"), cancellable = true)
    public void render(GuiGraphics guiGraphics, Collection<MobEffectInstance> p_453354_, int p_370153_, int p_365612_, int mouseX, int mouseY, int p_457235_, CallbackInfo ci) {
        ci.cancel();
        if (screen instanceof LegacyMenuAccess<?> a) {
            ScreenRectangle rec = a.getMenuRectangle();
            LegacyRenderUtil.renderContainerEffects(guiGraphics, rec.left(), rec.top(), rec.width(), rec.height(), mouseX, mouseY);
        }
    }
}
