package wily.legacy.mixin;

import net.minecraft.advancements.AdvancementHolder;
import net.minecraft.advancements.AdvancementType;
import net.minecraft.advancements.DisplayInfo;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.PlayerFaceRenderer;
import net.minecraft.client.gui.components.toasts.AdvancementToast;
import net.minecraft.client.gui.components.toasts.Toast;
import net.minecraft.client.gui.components.toasts.ToastComponent;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.util.Mth;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import wily.legacy.util.ScreenUtil;

import java.util.List;

@Mixin(AdvancementToast.class)
public abstract class AdvancementToastMixin implements Toast {
    @Shadow private boolean playedSound;

    @Shadow @Final private AdvancementHolder advancement;

    @Override
    public Visibility render(GuiGraphics guiGraphics, ToastComponent toastComponent, long l) {
        DisplayInfo displayInfo = this.advancement.value().display().orElse(null);
        ScreenUtil.renderPointerPanel(guiGraphics,0,0,width(),height());
        if (displayInfo != null) {
            List<FormattedCharSequence> list = toastComponent.getMinecraft().font.split(displayInfo.getTitle(), 120);
            int i = displayInfo.getType() == AdvancementType.CHALLENGE ? 0xFF88FF : 0xFFFF00;
            if (list.size() == 1) {
                guiGraphics.drawString(toastComponent.getMinecraft().font, displayInfo.getType().getDisplayName(),(width() - toastComponent.getMinecraft().font.width(displayInfo.getType().getDisplayName())) / 2, height() - 18, i | 0xFF000000);
                guiGraphics.pose().pushPose();
                guiGraphics.pose().translate((width() - toastComponent.getMinecraft().font.width(list.get(0)) * 1.5f) / 2,10,0);
                guiGraphics.pose().scale(1.5f,1.5f,1.5f);
                guiGraphics.drawString(toastComponent.getMinecraft().font, list.get(0), 0, 0, -1);
                guiGraphics.pose().popPose();
            } else {
                if (l < 1500L) {
                    int k = Mth.floor(Mth.clamp((float)(1500L - l) / 300.0f, 0.0f, 1.0f) * 255.0f) << 24 | 0x4000000;
                    guiGraphics.drawString(toastComponent.getMinecraft().font, displayInfo.getType().getDisplayName(),(width() - toastComponent.getMinecraft().font.width(displayInfo.getType().getDisplayName())) / 2, (height() - 8)/ 2, i | k);
                } else {
                    int k = Mth.floor(Mth.clamp((float)(l - 1500L) / 300.0f, 0.0f, 1.0f) * 252.0f) << 24 | 0x4000000;
                    float m = (this.height() - list.size() * toastComponent.getMinecraft().font.lineHeight * 1.5f) / 2f;
                    for (FormattedCharSequence formattedCharSequence : list) {
                        guiGraphics.pose().pushPose();
                        guiGraphics.pose().translate((width() - toastComponent.getMinecraft().font.width(formattedCharSequence) * 1.5f) / 2,m,0);
                        guiGraphics.pose().scale(1.5f,1.5f,1.5f);
                        guiGraphics.drawString(toastComponent.getMinecraft().font, formattedCharSequence, 0, 0, 0xFFFFFF | k);
                        guiGraphics.pose().popPose();
                        m += toastComponent.getMinecraft().font.lineHeight * 1.5f;
                    }
                }
            }
            if (!this.playedSound && l > 0L) {
                this.playedSound = true;
                if (displayInfo.getType() == AdvancementType.CHALLENGE) {
                    toastComponent.getMinecraft().getSoundManager().play(SimpleSoundInstance.forUI(SoundEvents.UI_TOAST_CHALLENGE_COMPLETE, 1.0f, 1.0f));
                }
            }
            if (toastComponent.getMinecraft().player != null) PlayerFaceRenderer.draw(guiGraphics, toastComponent.getMinecraft().player.getSkin(), 7, (height() - 32) / 2, 32);
            ScreenUtil.renderPanel(guiGraphics,width() - 38,(height() - 28) / 2,28,28,2f);
            guiGraphics.renderItem(displayInfo.getIcon(), width() - 32, (height() - 16) / 2);
            return (double)l >= 5000.0 * toastComponent.getNotificationDisplayTimeMultiplier() ? Toast.Visibility.HIDE : Toast.Visibility.SHOW;
        }
        return Toast.Visibility.HIDE;
    }

    @Override
    public int width() {
        return 258;
    }

    @Override
    public int height() {
        return 46;
    }
}
