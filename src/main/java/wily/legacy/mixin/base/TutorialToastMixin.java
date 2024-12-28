package wily.legacy.mixin.base;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.toasts.Toast;
import net.minecraft.client.gui.components.toasts.TutorialToast;
import net.minecraft.resources.ResourceLocation;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;
import wily.legacy.util.ScreenUtil;

import java.util.function.Function;

@Mixin(TutorialToast.class)
public abstract class TutorialToastMixin implements Toast {
    @Redirect(method = "render", at = @At(value = "INVOKE", target = /*? if <1.20.2 {*//*"Lnet/minecraft/client/gui/GuiGraphics;blit(Lnet/minecraft/resources/ResourceLocation;IIIIII)V"*//*?} else if <1.21.2 {*//*"Lnet/minecraft/client/gui/GuiGraphics;blitSprite(Lnet/minecraft/resources/ResourceLocation;IIII)V"*//*?} else {*/ "Lnet/minecraft/client/gui/GuiGraphics;blitSprite(Ljava/util/function/Function;Lnet/minecraft/resources/ResourceLocation;IIII)V"/*?}*/))
    public void render(GuiGraphics instance,/*? if >=1.21.2 {*/ Function function, /*?}*/  ResourceLocation resourceLocation, int i, int j, int k, int l/*? if <=1.20.1 {*//*, int m, int n*//*?}*/) {
        ScreenUtil.renderPointerPanel(instance,0,0,width(),height());
    }
}
