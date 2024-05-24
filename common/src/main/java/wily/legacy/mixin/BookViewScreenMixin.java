package wily.legacy.mixin;

import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.BookViewScreen;
import net.minecraft.client.gui.screens.inventory.PageButton;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.FormattedText;
import net.minecraft.network.chat.Style;
import net.minecraft.util.FormattedCharSequence;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import wily.legacy.client.controller.BindingState;
import wily.legacy.client.controller.Controller;
import wily.legacy.client.controller.ControllerBinding;
import wily.legacy.client.screen.BookPanel;
import wily.legacy.client.screen.ControlTooltip;

import java.util.List;

@Mixin(BookViewScreen.class)
public abstract class BookViewScreenMixin extends Screen implements Controller.Event {

    @Shadow protected abstract void updateButtonVisibility();


    @Shadow private PageButton forwardButton;
    @Shadow private PageButton backButton;

    @Shadow protected abstract void pageBack();

    @Shadow protected abstract void pageForward();

    @Shadow private Component pageMsg;

    @Shadow protected abstract int getNumPages();

    @Shadow private int currentPage;
    @Shadow private int cachedPage;
    @Shadow private List<FormattedCharSequence> cachedPageComponents;
    @Shadow private BookViewScreen.BookAccess bookAccess;


    @Shadow public abstract @Nullable Style getClickedComponentStyleAt(double d, double e);

    private BookPanel panel = new BookPanel(this);
    private ControlTooltip.Renderer controlTooltipRender = ControlTooltip.defaultScreen(this)
            .add(()->ControlTooltip.getActiveType().isKeyboard() ? ControlTooltip.getKeyIcon(InputConstants.KEY_LEFT,true) : ControllerBinding.LEFT_BUMPER.bindingState.getIcon(true), ()-> currentPage != 0 ? ControlTooltip.CONTROL_ACTION_CACHE.getUnchecked("legacy.action.previous_page") : null)
            .add(()->ControlTooltip.getActiveType().isKeyboard() ? ControlTooltip.getKeyIcon(InputConstants.KEY_RIGHT,true) : ControllerBinding.RIGHT_BUMPER.bindingState.getIcon(true), ()-> this.currentPage < this.getNumPages() - 1 ? ControlTooltip.CONTROL_ACTION_CACHE.getUnchecked( "legacy.action.next_page") : null);
    protected BookViewScreenMixin(Component component) {
        super(component);
    }

    @Inject(method = "init",at = @At("HEAD"), cancellable = true)
    public void init(CallbackInfo ci) {
        ci.cancel();
        panel.init();
        addRenderableWidget(panel);
        this.forwardButton = this.addRenderableWidget(panel.createLegacyPageButton(panel.x + panel.width - 62, panel.y + panel.height - 34, true, (button) -> this.pageForward(), true));
        this.backButton = this.addRenderableWidget(panel.createLegacyPageButton(panel.x + 26, panel.y + panel.height - 34, false, (button) -> this.pageBack(), true));
        setFocused(panel);
        this.updateButtonVisibility();
    }
    public void renderBackground(GuiGraphics guiGraphics, int i, int j, float f) {
    }
    @Inject(method = "render",at = @At("HEAD"), cancellable = true)
    public void render(GuiGraphics guiGraphics, int i, int j, float f, CallbackInfo ci) {
        ci.cancel();
        super.render(guiGraphics, i, j, f);

        if (this.cachedPage != this.currentPage) {
            FormattedText formattedText = this.bookAccess.getPage(this.currentPage);
            this.cachedPageComponents = this.font.split(formattedText, 159);
            this.pageMsg = Component.translatable("book.pageIndicator", this.currentPage + 1, Math.max(this.getNumPages(), 1));
        }
        this.cachedPage = this.currentPage;
        guiGraphics.drawString(this.font, this.pageMsg, panel.x + panel.width - 24 - font.width(pageMsg), panel.y + 22, 0, false);
        int n = Math.min(176 / this.font.lineHeight, this.cachedPageComponents.size());
        for (int o = 0; o < n; ++o) {
            FormattedCharSequence formattedCharSequence = this.cachedPageComponents.get(o);
            guiGraphics.drawString(this.font, formattedCharSequence, panel.x + 20, panel.y + 37 + o * this.font.lineHeight, 0, false);
        }
        Style style = this.getClickedComponentStyleAt(i, j);
        if (style != null) {
            guiGraphics.renderComponentHoverEffect(this.font, style, i, j);
        }
        controlTooltipRender.render(guiGraphics, i, j, f);

    }
    @Override
    public void bindingStateTick(BindingState state) {
        if ((state.is(ControllerBinding.RIGHT_BUMPER) || state.is(ControllerBinding.LEFT_BUMPER)) && state.canClick()){
            (state.is(ControllerBinding.RIGHT_BUMPER) ? forwardButton : backButton).keyPressed(InputConstants.KEY_RETURN,0,0);
        }
    }
    @Inject(method = "keyPressed",at = @At("HEAD"), cancellable = true)
    public void keyPressed(int i, int j, int k, CallbackInfoReturnable<Boolean> cir) {
        if (ControlTooltip.getActiveType().isKeyboard() && (i == InputConstants.KEY_RIGHT || i == InputConstants.KEY_LEFT)){
            (i == InputConstants.KEY_RIGHT ? forwardButton : backButton).keyPressed(InputConstants.KEY_RETURN,0,0);
            cir.setReturnValue(true);
            return;
        }
        cir.setReturnValue(super.keyPressed(i,j,k));
    }
    @Inject(method = "getClickedComponentStyleAt",at = @At("HEAD"), cancellable = true)
    public void getClickedComponentStyleAt(double d, double e, CallbackInfoReturnable<Style> cir) {
        if (this.cachedPageComponents.isEmpty()) {
            cir.setReturnValue(null);
            return;
        }
        int i = (int) Math.floor(d - panel.x - 20);
        int j = (int) Math.floor(d - panel.y - 37);
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
