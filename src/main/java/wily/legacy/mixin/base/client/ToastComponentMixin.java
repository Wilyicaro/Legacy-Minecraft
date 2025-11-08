package wily.legacy.mixin.base.client;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.toasts.Toast;
import org.joml.Matrix3x2f;
import org.joml.Matrix3x2fStack;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(targets = /*? if <1.21.2 {*//*"net.minecraft.client.gui.components.toasts.ToastComponent$ToastInstance"*//*?} else {*/ "net.minecraft.client.gui.components.toasts.ToastManager$ToastInstance"/*?}*/)
public abstract class ToastComponentMixin {

    @Shadow
    @Final
    int /*? if >=1.21.2 {*/firstSlotIndex/*?} else {*//*index*//*?}*/;

    @Shadow
    private float visiblePortion;
    @Shadow
    @Final
    private Toast toast;

    //? if <=1.20.1 {
    /*@Inject(method = "render", at = @At("HEAD"), cancellable = true)
    private void render(int i, GuiGraphics guiGraphics, CallbackInfoReturnable<Boolean> cir){
        if (!MinecraftAccessor.getInstance().hasGameLoaded()) cir.setReturnValue(false);
    }
    *///?}

    @Redirect(method = "render", at = @At(value = "INVOKE", target = "Lorg/joml/Matrix3x2fStack;translate(FF)Lorg/joml/Matrix3x2f;", remap = false))
    private Matrix3x2f render(Matrix3x2fStack instance, float f, float g, /*? if >=1.21.2 {*/GuiGraphics guiGraphics, /*?}*/int i) {
        return instance.translate((i - this.toast.width()) / 2f, -toast.height() + (50 + toast.height() + this./*? if >=1.21.2 {*/firstSlotIndex/*?} else {*//*index*//*?}*/ * 32f) * /*? if <1.21.2 {*//*this.getVisibility(Util.getMillis())*//*?} else {*/visiblePortion/*?}*/);
    }
}
