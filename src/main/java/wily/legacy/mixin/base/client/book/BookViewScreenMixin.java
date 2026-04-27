package wily.legacy.mixin.base.client.book;

import com.llamalad7.mixinextras.sugar.Local;
import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.BookViewScreen;
import net.minecraft.client.gui.screens.inventory.PageButton;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.util.FormattedCharSequence;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyArg;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import wily.legacy.client.ControlType;
import wily.legacy.client.controller.BindingState;
import wily.legacy.client.controller.Controller;
import wily.legacy.client.controller.ControllerBinding;
import wily.legacy.client.screen.BookPanel;
import wily.legacy.client.screen.ControlTooltip;
import wily.legacy.util.LegacyComponents;

import java.util.List;

@Mixin(BookViewScreen.class)
public abstract class BookViewScreenMixin extends Screen implements Controller.Event, ControlTooltip.Event {

    @Shadow
    private PageButton forwardButton;
    @Shadow
    private PageButton backButton;
    @Shadow
    private int currentPage;

    @Unique
    private final BookPanel panel = new BookPanel(this);
    protected BookViewScreenMixin(Component component) {
        super(component);
    }

    @Shadow
    protected abstract void updateButtonVisibility();

    @Shadow
    protected abstract void pageBack();

    @Shadow
    protected abstract void pageForward();

    @Shadow
    protected abstract int getNumPages();

    @Override
    public void added() {
        super.added();
        ControlTooltip.Renderer.of(this)
                .add(() -> ControlType.getActiveType().isKbm() ? ControlTooltip.getKeyIcon(InputConstants.KEY_LEFT) : ControllerBinding.LEFT_BUMPER.getIcon(), () -> currentPage != 0 ? LegacyComponents.PREVIOUS_PAGE : null)
                .add(() -> ControlType.getActiveType().isKbm() ? ControlTooltip.getKeyIcon(InputConstants.KEY_RIGHT) : ControllerBinding.RIGHT_BUMPER.getIcon(), () -> this.currentPage < this.getNumPages() - 1 ? LegacyComponents.NEXT_PAGE : null);
    }

    @Inject(method = "init", at = @At("HEAD"), cancellable = true)
    public void init(CallbackInfo ci) {
        ci.cancel();
        panel.init();
        addRenderableWidget(panel);
        this.forwardButton = this.addRenderableWidget(panel.createLegacyPageButton(panel.x + panel.width - 62, panel.y + panel.height - 34, true, (button) -> this.pageForward(), true));
        this.backButton = this.addRenderableWidget(panel.createLegacyPageButton(panel.x + 26, panel.y + panel.height - 34, false, (button) -> this.pageBack(), true));
        setFocused(panel);
        this.updateButtonVisibility();
    }

    //? if >1.20.1 {
    @Inject(method = "renderBackground", at = @At("HEAD"), cancellable = true)
    public void renderBackground(CallbackInfo ci) {
        ci.cancel();
    }
    //?}

    @Override
    public void bindingStateTick(BindingState state) {
        if ((state.is(ControllerBinding.RIGHT_BUMPER) || state.is(ControllerBinding.LEFT_BUMPER)) && state.canClick()) {
            (state.is(ControllerBinding.RIGHT_BUMPER) ? forwardButton : backButton).keyPressed(new KeyEvent(InputConstants.KEY_RETURN, 0, 0));
        }
    }

    @Inject(method = "keyPressed", at = @At("HEAD"), cancellable = true)
    public void keyPressed(KeyEvent keyEvent, CallbackInfoReturnable<Boolean> cir) {
        if (ControlType.getActiveType().isKbm() && (keyEvent.isRight() || keyEvent.isLeft())) {
            (keyEvent.isRight() ? forwardButton : backButton).keyPressed(new KeyEvent(InputConstants.KEY_RETURN, 0, 0));
            cir.setReturnValue(true);
            return;
        }
        cir.setReturnValue(super.keyPressed(keyEvent));
    }

    @ModifyArg(method = "visitText", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/Font;split(Lnet/minecraft/network/chat/FormattedText;I)Ljava/util/List;"), index = 1)
    public int changeSplitWidth(int i) {
        return 159;
    }

    @ModifyArg(method = "visitText", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/ActiveTextCollector;accept(Lnet/minecraft/client/gui/TextAlignment;IILnet/minecraft/network/chat/Component;)V"), index = 1)
    public int changePageX(int i) {
        return panel.x + panel.width - 24;
    }

    @ModifyArg(method = "visitText", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/ActiveTextCollector;accept(Lnet/minecraft/client/gui/TextAlignment;IILnet/minecraft/network/chat/Component;)V"), index = 2)
    public int changePageY(int i) {
        return panel.y + 22;
    }

    @ModifyArg(method = "visitText", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/ActiveTextCollector;accept(IILnet/minecraft/util/FormattedCharSequence;)V"), index = 0)
    public int changeTextX(int i) {
        return panel.x + 20;
    }

    @ModifyArg(method = "visitText", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/ActiveTextCollector;accept(IILnet/minecraft/util/FormattedCharSequence;)V"), index = 1)
    public int changeTextY(int i, @Local(ordinal = 3) int o) {
        return panel.y + 37 + o * this.font.lineHeight;
    }

    @ModifyArg(method = "visitText", at = @At(value = "INVOKE", target = "Ljava/lang/Math;min(II)I"), index = 0)
    public int changeMaxLines(int i) {
        return 176 / this.font.lineHeight;
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}