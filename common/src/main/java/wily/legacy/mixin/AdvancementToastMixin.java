package wily.legacy.mixin;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.advancements.Advancement;
import net.minecraft.advancements.DisplayInfo;
import net.minecraft.advancements.FrameType;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.components.PlayerFaceRenderer;
import net.minecraft.client.gui.components.toasts.AdvancementToast;
import net.minecraft.client.gui.components.toasts.Toast;
import net.minecraft.client.gui.components.toasts.ToastComponent;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.util.Mth;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import wily.legacy.client.ControlType;
import wily.legacy.client.LegacyGuiGraphics;
import wily.legacy.client.controller.ControllerBinding;
import wily.legacy.client.screen.ControlTooltip;
import wily.legacy.util.LegacySprites;
import wily.legacy.util.ScreenUtil;

@Mixin(AdvancementToast.class)
public abstract class AdvancementToastMixin implements Toast {
    int width = 82;
    @Shadow private boolean playedSound;

    @Shadow @Final private Advancement advancement;

    @Inject(method = "render", at = @At("HEAD"), cancellable = true)
    public void render(PoseStack poseStack, ToastComponent toastComponent, long l, CallbackInfoReturnable<Visibility> cir) {
        Component holdToView = Component.translatable("legacy.menu.advancements.toast",(ControlType.getActiveType().isKbm() ? ControlTooltip.getKeyIcon(InputConstants.KEY_I) : ControllerBinding.UP_BUTTON.bindingState.getIcon()).getComponent());
        Font font= Minecraft.getInstance().font;
        DisplayInfo displayInfo = this.advancement.getDisplay();
        width = 82 + (displayInfo == null ? 0 : Math.max(font.width(holdToView), Math.max(font.width(displayInfo.getTitle()) * 3/2,font.width(displayInfo.getFrame().getDisplayName()))));
        ScreenUtil.renderPointerPanel(poseStack,0,0,width(),height());
        if (displayInfo != null) {
            int i = displayInfo.getFrame() == FrameType.CHALLENGE ? 0xFF88FF : 0xFFFF00;

            if (l < 1500L) poseStack.drawString(font, displayInfo.getFrame().getDisplayName(),(width() - font.width(displayInfo.getFrame().getDisplayName())) / 2, height() - 18, i | Mth.floor(Mth.clamp((float)(1500L - l) / 300.0f, 0.0f, 1.0f) * 255.0f) << 24 | 0x4000000);
            else poseStack.drawString(font, holdToView,(width() - font.width(holdToView)) / 2, height() - 18, 0xFFFFFF | Mth.floor(Mth.clamp((float)(l - 1500L) / 300.0f, 0.0f, 1.0f) * 252.0f) << 24 | 0x4000000);
            poseStack.pose().pushPose();
            poseStack.pose().translate((width() - font.width(displayInfo.getTitle()) * 1.5f) / 2,10,0);
            poseStack.pose().scale(1.5f,1.5f,1.5f);
            poseStack.drawString(font, displayInfo.getTitle(), 0, 0, -1);
            poseStack.pose().popPose();

            if (!this.playedSound && l > 0L) {
                this.playedSound = true;
                if (displayInfo.getFrame() == FrameType.CHALLENGE) {
                    toastComponent.getMinecraft().getSoundManager().play(SimpleSoundInstance.forUI(SoundEvents.UI_TOAST_CHALLENGE_COMPLETE, 1.0f, 1.0f));
                }
            }
            if (toastComponent.getMinecraft().player != null) PlayerFaceRenderer.draw(poseStack, toastComponent.getMinecraft().player.getSkinTextureLocation(), 7, (height() - 32) / 2, 32);
            LegacyGuiGraphics.of(poseStack).blitSprite(LegacySprites.SMALL_PANEL,width() - 38,(height() - 28) / 2,28,28);
            poseStack.renderItem(displayInfo.getIcon(), width() - 32, (height() - 16) / 2);
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
