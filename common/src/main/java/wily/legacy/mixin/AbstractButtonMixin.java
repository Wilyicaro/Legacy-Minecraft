package wily.legacy.mixin;

import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.Util;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractButton;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import wily.legacy.client.LegacyGuiGraphics;
import wily.legacy.util.LegacySprites;
import wily.legacy.util.ScreenUtil;

@Mixin(AbstractButton.class)
public abstract class
AbstractButtonMixin extends AbstractWidget {
    @Shadow public abstract void renderString(GuiGraphics guiGraphics, Font font, int i);

    @Unique
    long lastTimePressed;

    public AbstractButtonMixin(int i, int j, int k, int l, Component component) {
        super(i, j, k, l, component);
    }

    @Inject(method = "onClick", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/components/AbstractButton;onPress()V"))
    private void onPress(double d, double e, CallbackInfo ci){
        lastTimePressed = Util.getMillis();
    }
    @Inject(method = "keyPressed", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/components/AbstractButton;onPress()V"))
    private void onPress(int i, int j, int k, CallbackInfoReturnable<Boolean> cir){
        lastTimePressed = Util.getMillis();
    }
    @Inject(method = "renderWidget", at = @At("HEAD"), cancellable = true)
    protected void renderWidget(GuiGraphics guiGraphics, int i, int j, float f, CallbackInfo ci) {
        ci.cancel();
        Minecraft minecraft = Minecraft.getInstance();
        guiGraphics.setColor(1.0F, 1.0F, 1.0F, this.alpha);
        RenderSystem.enableBlend();
        RenderSystem.enableDepthTest();
        LegacyGuiGraphics.of(guiGraphics).blitSprite(isHoveredOrFocused() ?LegacySprites.BUTTON_HIGHLIGHTED : LegacySprites.BUTTON , this.getX(), this.getY(), this.getWidth(), this.getHeight());
        guiGraphics.setColor(1.0F, 1.0F, 1.0F, 1.0F);
        int k = ScreenUtil.getDefaultTextColor(!isHoveredOrFocused() || Util.getMillis() - lastTimePressed <= 150);
        this.renderString(guiGraphics, minecraft.font, k | Mth.ceil(this.alpha * 255.0F) << 24);
    }
}
