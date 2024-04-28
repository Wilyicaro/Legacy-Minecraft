package wily.legacy.mixin;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.toasts.RecipeToast;
import net.minecraft.client.gui.components.toasts.Toast;
import net.minecraft.client.gui.components.toasts.ToastComponent;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeHolder;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import wily.legacy.client.Offset;
import wily.legacy.util.ScreenUtil;

import java.util.List;

@Mixin(RecipeToast.class)
public abstract class RecipeToastMixin implements Toast {
    @Shadow private long lastChanged;

    @Shadow @Final private static Component TITLE_TEXT;

    @Shadow @Final private static Component DESCRIPTION_TEXT;

    @Shadow private boolean changed;

    @Shadow @Final private List<RecipeHolder<?>> recipes;
    @Override
    public int width() {
        return Toast.super.width() + 80;
    }
    @Inject(method = "addOrUpdate", at = @At("HEAD"), cancellable = true)
    private static void addOrUpdate(ToastComponent toastComponent, RecipeHolder<?> recipeHolder, CallbackInfo ci) {
        if (!ScreenUtil.hasClassicCrafting()) ci.cancel();
    }
    @Inject(method = "render", at = @At(value = "HEAD"), cancellable = true)
    public void render(GuiGraphics guiGraphics, ToastComponent toastComponent, long l, CallbackInfoReturnable<Visibility> cir) {
        if (this.changed) {
            this.lastChanged = l;
            this.changed = false;
        }
        if (this.recipes.isEmpty() || !ScreenUtil.hasClassicCrafting()){
            cir.setReturnValue(Toast.Visibility.HIDE);
            return;
        }

        ScreenUtil.renderPointerPanel(guiGraphics,0,0,width(),height());
        guiGraphics.pose().pushPose();
        guiGraphics.pose().translate((width() - 1.5 * toastComponent.getMinecraft().font.width(TITLE_TEXT)) / 2, 5,0);
        guiGraphics.pose().scale(1.5f,1.5f,1.5f);
        guiGraphics.drawString(toastComponent.getMinecraft().font, TITLE_TEXT, 0,0, 0xFFFFFF);
        guiGraphics.pose().popPose();
        guiGraphics.drawString(toastComponent.getMinecraft().font, DESCRIPTION_TEXT, (width() - toastComponent.getMinecraft().font.width(DESCRIPTION_TEXT)) / 2 , 18, 0xFFFFFF);
        RecipeHolder<?> recipeHolder = this.recipes.get((int)((double)l / Math.max(1.0, 5000.0 * toastComponent.getNotificationDisplayTimeMultiplier() / (double)this.recipes.size()) % (double)this.recipes.size()));
        ScreenUtil.iconHolderRenderer.itemHolder(8,(height() - 27) / 2,27,27,recipeHolder.value().getToastSymbol(),false, Offset.ZERO).renderItem(guiGraphics,0,0,0);
        ScreenUtil.renderPanel(guiGraphics,width() - 36,(height() - 28) / 2,28,28,2f);
        guiGraphics.renderItem(recipeHolder.value().getResultItem(toastComponent.getMinecraft().level.registryAccess()) ,width() - 30, (height() - 16) / 2);
        cir.setReturnValue((double)(l - this.lastChanged) >= 5000.0 * toastComponent.getNotificationDisplayTimeMultiplier() ? Toast.Visibility.HIDE : Toast.Visibility.SHOW);
    }
}
