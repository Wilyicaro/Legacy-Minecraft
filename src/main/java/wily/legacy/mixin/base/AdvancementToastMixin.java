package wily.legacy.mixin.base;

import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.advancements.*;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.toasts.AdvancementToast;
import net.minecraft.client.gui.components.toasts.Toast;
//? if <1.21.2 {
/*import net.minecraft.client.gui.components.toasts.ToastComponent;
*///?}
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.util.Mth;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import wily.factoryapi.base.client.FactoryGuiGraphics;
import wily.legacy.client.AdvancementToastAccessor;
import wily.legacy.client.ControlType;
import wily.legacy.client.controller.ControllerBinding;
import wily.legacy.client.screen.ControlTooltip;
import wily.legacy.util.LegacySprites;
import wily.legacy.util.ScreenUtil;

@Mixin(AdvancementToast.class)
public abstract class AdvancementToastMixin implements Toast, AdvancementToastAccessor {
    int width = 82;
    @Shadow private boolean playedSound;

    @Shadow @Final private /*? if >1.20.1 {*/AdvancementHolder/*?} else {*//*Advancement*//*?}*/ advancement;
    @Inject(method = "render", at = @At("HEAD"), cancellable = true)
    public void render(GuiGraphics guiGraphics, /*? if <1.21.2 {*//*ToastComponent toastComponent*//*?} else {*/Font font/*?}*/, long l, /*? if <1.21.2 {*/ /*CallbackInfoReturnable<Visibility> cir*//*?} else {*/CallbackInfo ci /*?}*/) {
        //? if <1.21.2 {
        /*Font font = Minecraft.getInstance().font;
        *///?} else {
        ci.cancel();
        //?}
        Component holdToView = Component.translatable("legacy.menu.advancements.toast",(ControlType.getActiveType().isKbm() ? ControlTooltip.getKeyIcon(InputConstants.KEY_I) : ControllerBinding.UP_BUTTON.bindingState.getIcon()).getComponent());
        DisplayInfo displayInfo = this.advancement./*? if >1.20.1 {*/value().display().orElse(null)/*?} else {*//*getDisplay()*//*?}*/;
        width = 82 + (displayInfo == null ? 0 : Math.max(font.width(holdToView), Math.max(font.width(displayInfo.getTitle()) * 3/2,font.width(displayInfo./*? if >1.20.1 {*/getType/*?} else {*//*getFrame*//*?}*/().getDisplayName()))));
        ScreenUtil.renderPointerPanel(guiGraphics,0,0,width(),height());
        if (displayInfo != null) {
            int i = displayInfo./*? if >1.20.1 {*/getType/*?} else {*//*getFrame*//*?}*/() == /*? if >1.20.1 {*/AdvancementType/*?} else {*//*FrameType*//*?}*/.CHALLENGE ? 0xFF88FF : 0xFFFF00;

            if (l < 1500L) guiGraphics.drawString(font, displayInfo./*? if >1.20.1 {*/getType/*?} else {*//*getFrame*//*?}*/().getDisplayName(),(width() - font.width(displayInfo./*? if >1.20.1 {*/getType/*?} else {*//*getFrame*//*?}*/().getDisplayName())) / 2, height() - 18, i | Mth.floor(Mth.clamp((float)(1500L - l) / 300.0f, 0.0f, 1.0f) * 255.0f) << 24 | 0x4000000);
            else guiGraphics.drawString(font, holdToView,(width() - font.width(holdToView)) / 2, height() - 18, 0xFFFFFF | Mth.floor(Mth.clamp((float)(l - 1500L) / 300.0f, 0.0f, 1.0f) * 252.0f) << 24 | 0x4000000);
            guiGraphics.pose().pushPose();
            guiGraphics.pose().translate((width() - font.width(displayInfo.getTitle()) * 1.5f) / 2,10,0);
            guiGraphics.pose().scale(1.5f,1.5f,1.5f);
            guiGraphics.drawString(font, displayInfo.getTitle(), 0, 0, -1);
            guiGraphics.pose().popPose();

            if (!this.playedSound && l > 0L) {
                this.playedSound = true;
                if (displayInfo./*? if >1.20.1 {*/getType/*?} else {*//*getFrame*//*?}*/() == /*? if >1.20.1 {*/AdvancementType/*?} else {*//*FrameType*//*?}*/.CHALLENGE) {
                    Minecraft.getInstance().getSoundManager().play(SimpleSoundInstance.forUI(SoundEvents.UI_TOAST_CHALLENGE_COMPLETE, 1.0f, 1.0f));
                }
            }
            ScreenUtil.renderLocalPlayerHead(guiGraphics,7, (height() - 32) / 2, 32);
            FactoryGuiGraphics.of(guiGraphics).blitSprite(LegacySprites.SMALL_PANEL,width() - 38,(height() - 28) / 2,28,28);
            guiGraphics.renderItem(displayInfo.getIcon(), width() - 32, (height() - 16) / 2);
            //? if <1.21.2 {
            /*cir.setReturnValue((double)l >= 5000.0 * toastComponent.getNotificationDisplayTimeMultiplier() ? Toast.Visibility.HIDE : Toast.Visibility.SHOW);
            *///?}
            return;
        }
        //? if <1.21.2 {
        /*cir.setReturnValue(Toast.Visibility.HIDE);
        *///?}
    }

    @Override
    public ResourceLocation getAdvancementId() {
        return advancement./*? if >1.20.1 {*/id/*?} else {*//*getId*//*?}*/();
    }

    @Override
    public int width() {
        return width;
    }

    @Override
    public int height() {
        return 46;
    }

    @Override
    public int /*? if <1.21.2 {*//*slotCount*//*?} else {*/occcupiedSlotCount/*?}*/() {
        return 5;
    }
}
