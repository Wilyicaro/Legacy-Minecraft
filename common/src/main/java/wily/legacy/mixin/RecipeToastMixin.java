package wily.legacy.mixin;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.gui.components.toasts.RecipeToast;
import net.minecraft.client.gui.components.toasts.Toast;
import net.minecraft.client.gui.components.toasts.ToastComponent;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.crafting.Recipe;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import wily.legacy.client.LegacyGuiGraphics;
import wily.legacy.util.LegacySprites;
import wily.legacy.util.Offset;
import wily.legacy.util.ScreenUtil;

import java.util.List;

@Mixin(RecipeToast.class)
public abstract class RecipeToastMixin implements Toast {
    @Shadow private long lastChanged;

    @Shadow @Final private static Component TITLE_TEXT;

    @Shadow @Final private static Component DESCRIPTION_TEXT;

    @Shadow private boolean changed;

    @Shadow @Final private List<Recipe<?>> recipes;
    @Override
    public int width() {
        return Toast.super.width() + 80;
    }
    @Inject(method = "addOrUpdate", at = @At("HEAD"), cancellable = true)
    private static void addOrUpdate(ToastComponent toastComponent, Recipe<?> recipeHolder, CallbackInfo ci) {
        if (!ScreenUtil.hasClassicCrafting()) ci.cancel();
    }
    @Inject(method = "render", at = @At(value = "HEAD"), cancellable = true)
    public void render(PoseStack poseStack, ToastComponent toastComponent, long l, CallbackInfoReturnable<Visibility> cir) {
        if (this.changed) {
            this.lastChanged = l;
            this.changed = false;
        }
        if (this.recipes.isEmpty() || !ScreenUtil.hasClassicCrafting()){
            cir.setReturnValue(Toast.Visibility.HIDE);
            return;
        }

        ScreenUtil.renderPointerPanel(poseStack,0,0,width(),height());
        poseStack.pose().pushPose();
        poseStack.pose().translate((width() - 1.5 * toastComponent.getMinecraft().font.width(TITLE_TEXT)) / 2, 5,0);
        poseStack.pose().scale(1.5f,1.5f,1.5f);
        poseStack.drawString(toastComponent.getMinecraft().font, TITLE_TEXT, 0,0, 0xFFFFFF);
        poseStack.pose().popPose();
        poseStack.drawString(toastComponent.getMinecraft().font, DESCRIPTION_TEXT, (width() - toastComponent.getMinecraft().font.width(DESCRIPTION_TEXT)) / 2 , 18, 0xFFFFFF);
        Recipe<?> recipeHolder = this.recipes.get((int)((double)l / Math.max(1.0, 5000.0 * toastComponent.getNotificationDisplayTimeMultiplier() / (double)this.recipes.size()) % (double)this.recipes.size()));
        ScreenUtil.iconHolderRenderer.itemHolder(8,(height() - 27) / 2,27,27,recipeHolder.getToastSymbol(),false, Offset.ZERO).renderItem(poseStack,0,0,0);
        LegacyGuiGraphics.of(poseStack).blitSprite(LegacySprites.SMALL_PANEL,width() - 36,(height() - 28) / 2,28,28);
        poseStack.renderItem(recipeHolder.getResultItem(toastComponent.getMinecraft().level.registryAccess()) ,width() - 30, (height() - 16) / 2);
        cir.setReturnValue((double)(l - this.lastChanged) >= 5000.0 * toastComponent.getNotificationDisplayTimeMultiplier() ? Toast.Visibility.HIDE : Toast.Visibility.SHOW);
    }
}
