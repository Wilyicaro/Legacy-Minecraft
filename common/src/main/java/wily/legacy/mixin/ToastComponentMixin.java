package wily.legacy.mixin;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.Util;
import net.minecraft.client.gui.components.toasts.Toast;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import wily.legacy.Legacy4JClient;

@Mixin(targets = "net.minecraft.client.gui.components.toasts.ToastComponent$ToastInstance")
public abstract class ToastComponentMixin {


    @Shadow protected abstract float getVisibility(long l);

    @Shadow @Final private Toast toast;
    @Shadow @Final private int index;

    @Inject(method = "render", at = @At("HEAD"), cancellable = true)
    private void render(int i, PoseStack poseStack, CallbackInfoReturnable<Boolean> cir){
        if (!Legacy4JClient.isGameLoadFinished) cir.setReturnValue(false);
    }

    @Redirect(method = "render", at = @At(value = "INVOKE",target = "Lcom/mojang/blaze3d/vertex/PoseStack;translate(FFF)V"))
    private void render(PoseStack instance, float f, float g, float h, int i){
        instance.translate((i - this.toast.width()) / 2f,-toast.height() + (50 + toast.height() + this.index * 32f) * this.getVisibility(Util.getMillis()), h);
    }
}
