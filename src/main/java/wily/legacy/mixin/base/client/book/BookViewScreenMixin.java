package wily.legacy.mixin.base.client.book;

import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.BookViewScreen;
import net.minecraft.client.gui.screens.inventory.PageButton;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.FormattedText;
import net.minecraft.network.chat.Style;
import net.minecraft.util.FormattedCharSequence;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
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
    private Component pageMsg;
    @Shadow
    private int currentPage;
    @Shadow
    private int cachedPage;
    @Shadow
    private List<FormattedCharSequence> cachedPageComponents;
    @Shadow
    private BookViewScreen.BookAccess bookAccess;
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

    @Shadow
    public abstract @Nullable Style getClickedComponentStyleAt(double d, double e);

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


    @Inject(method = "render", at = @At("HEAD"), cancellable = true)
    public void render(GuiGraphics guiGraphics, int i, int j, float f, CallbackInfo ci) {
        ci.cancel();
        super.render(guiGraphics, i, j, f);

        if (this.cachedPage != this.currentPage) {
            FormattedText formattedText = this.bookAccess.getPage(this.currentPage);
            this.cachedPageComponents = this.font.split(formattedText, 159);
            this.pageMsg = Component.translatable("book.pageIndicator", this.currentPage + 1, Math.max(this.getNumPages(), 1));
        }
        this.cachedPage = this.currentPage;
        guiGraphics.drawString(this.font, this.pageMsg, panel.x + panel.width - 24 - font.width(pageMsg), panel.y + 22, 0xFF000000, false);
        int n = Math.min(176 / this.font.lineHeight, this.cachedPageComponents.size());
        for (int o = 0; o < n; ++o) {
            FormattedCharSequence formattedCharSequence = this.cachedPageComponents.get(o);
            guiGraphics.drawString(this.font, formattedCharSequence, panel.x + 20, panel.y + 37 + o * this.font.lineHeight, 0xFF000000, false);
        }
        Style style = this.getClickedComponentStyleAt(i, j);
        if (style != null) {
            guiGraphics.renderComponentHoverEffect(this.font, style, i, j);
        }
    }

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

    @Inject(method = "getClickedComponentStyleAt", at = @At("HEAD"), cancellable = true)
    public void getClickedComponentStyleAt(double d, double e, CallbackInfoReturnable<Style> cir) {
        if (this.cachedPageComponents.isEmpty()) {
            cir.setReturnValue(null);
            return;
        }
        int i = (int) Math.floor(d - panel.x - 20);
        int j = (int) Math.floor(e - panel.y - 37);
        if (i < 0 || j < 0) {
            cir.setReturnValue(null);
            return;
        }
        int k = Math.min(176 / this.font.lineHeight, this.cachedPageComponents.size());
        if (i <= 159 && j < this.minecraft.font.lineHeight * k + k) {
            int l = j / this.minecraft.font.lineHeight;
            if (l < this.cachedPageComponents.size()) {
                FormattedCharSequence formattedCharSequence = this.cachedPageComponents.get(l);
                cir.setReturnValue(this.minecraft.font.getSplitter().componentStyleAtWidth(formattedCharSequence, i));
                return;
            }
        }
        cir.setReturnValue(null);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}
