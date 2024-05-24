package wily.legacy.mixin;

import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.advancements.AdvancementHolder;
import net.minecraft.advancements.AdvancementType;
import net.minecraft.advancements.DisplayInfo;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.PlayerFaceRenderer;
import net.minecraft.client.gui.components.toasts.AdvancementToast;
import net.minecraft.client.gui.components.toasts.Toast;
import net.minecraft.client.gui.components.toasts.ToastComponent;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.util.Mth;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import wily.legacy.client.controller.ControllerBinding;
import wily.legacy.client.screen.ControlTooltip;
import wily.legacy.util.ScreenUtil;

import java.util.List;

@Mixin(AdvancementToast.class)
public abstract class AdvancementToastMixin implements Toast {
    int width = 82;
    @Shadow private boolean playedSound;

    @Shadow @Final private AdvancementHolder advancement;

    @Inject(method = "render", at = @At("HEAD"), cancellable = true)
    public void render(GuiGraphics guiGraphics, ToastComponent toastComponent, long l, CallbackInfoReturnable<Visibility> cir) {
        Component holdToView = Component.translatable("legacy.menu.advancements.toast", ControlTooltip.getActiveType().isKeyboard() ? ControlTooltip.getKeyIcon(InputConstants.KEY_E,true) : ControllerBinding.LEFT_BUTTON.bindingState.getIcon(true));
        Font font= Minecraft.getInstance().font;
        DisplayInfo displayInfo = this.advancement.value().display().orElse(null);
        width = 82 + (displayInfo == null ? 0 : Math.max(font.width(holdToView), Math.max(font.width(displayInfo.getTitle()) * 3/2,font.width(displayInfo.getType().getDisplayName()))));
        ScreenUtil.renderPointerPanel(guiGraphics,0,0,width(),height());
        if (displayInfo != null) {
            int i = displayInfo.getType() == AdvancementType.CHALLENGE ? 0xFF88FF : 0xFFFF00;

            if (l < 1500L) guiGraphics.drawString(font, displayInfo.getType().getDisplayName(),(width() - font.width(displayInfo.getType().getDisplayName())) / 2, height() - 18, i | Mth.floor(Mth.clamp((float)(1500L - l) / 300.0f, 0.0f, 1.0f) * 255.0f) << 24 | 0x4000000);
            else guiGraphics.drawString(font, holdToView,(width() - font.width(holdToView)) / 2, height() - 18, 0xFFFFFF | Mth.floor(Mth.clamp((float)(l - 1500L) / 300.0f, 0.0f, 1.0f) * 252.0f) << 24 | 0x4000000);
            guiGraphics.pose().pushPose();
            guiGraphics.pose().translate((width() - font.width(displayInfo.getTitle()) * 1.5f) / 2,10,0);
            guiGraphics.pose().scale(1.5f,1.5f,1.5f);
            guiGraphics.drawString(font, displayInfo.getTitle(), 0, 0, -1);
            guiGraphics.pose().popPose();

            if (!this.playedSound && l > 0L) {
                this.playedSound = true;
                if (displayInfo.getType() == AdvancementType.CHALLENGE) {
                    toastComponent.getMinecraft().getSoundManager().play(SimpleSoundInstance.forUI(SoundEvents.UI_TOAST_CHALLENGE_COMPLETE, 1.0f, 1.0f));
                }
            }
            if (toastComponent.getMinecraft().player != null) PlayerFaceRenderer.draw(guiGraphics, toastComponent.getMinecraft().player.getSkin(), 7, (height() - 32) / 2, 32);
            ScreenUtil.renderPanel(guiGraphics,width() - 38,(height() - 28) / 2,28,28,2f);
            guiGraphics.renderItem(displayInfo.getIcon(), width() - 32, (height() - 16) / 2);
            cir.setReturnValue((double)l >= 5000.0 * toastComponent.getNotificationDisplayTimeMultiplier() ? Toast.Visibility.HIDE : Toast.Visibility.SHOW);
            return;
        }
        cir.setReturnValue(Toast.Visibility.HIDE);
    }

    @Override
    public int width() {
        return width;
    }

    @Override
    public int height() {
        return 46;
    }
}
