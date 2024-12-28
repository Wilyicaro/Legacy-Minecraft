package wily.legacy.mixin.base;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.Util;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.toasts.Toast;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import wily.factoryapi.base.client.MinecraftAccessor;
import wily.legacy.Legacy4JClient;

@Mixin(targets = /*? if <1.21.2 {*//*"net.minecraft.client.gui.components.toasts.ToastComponent$ToastInstance"*//*?} else {*/ "net.minecraft.client.gui.components.toasts.ToastManager$ToastInstance"/*?}*/)
public abstract class ToastComponentMixin {

    //? if <1.21.2 {
    /*@Shadow protected abstract float getVisibility(long l);
    *///?} else {
    @Shadow private float visiblePortion;
    //?}

    @Shadow @Final private Toast toast;
    @Shadow @Final int /*? if >=1.21.2 {*/firstSlotIndex/*?} else {*//*index*//*?}*/;

    //? if <=1.20.1 {
    /*@Inject(method = "render", at = @At("HEAD"), cancellable = true)
    private void render(int i, GuiGraphics guiGraphics, CallbackInfoReturnable<Boolean> cir){
        if (!MinecraftAccessor.getInstance().hasGameLoaded()) cir.setReturnValue(false);
    }
    *///?}

    @Redirect(method = "render", at = @At(value = "INVOKE",target = "Lcom/mojang/blaze3d/vertex/PoseStack;translate(FFF)V"))
    private void render(PoseStack instance, float f, float g, float h, /*? if >=1.21.2 {*/GuiGraphics guiGraphics, /*?}*/int i){
        instance.translate((i - this.toast.width()) / 2f,-toast.height() + (50 + toast.height() + this./*? if >=1.21.2 {*/firstSlotIndex/*?} else {*//*index*//*?}*/ * 32f) * /*? if <1.21.2 {*//*this.getVisibility(Util.getMillis())*//*?} else {*/visiblePortion/*?}*/, h);
    }
}
