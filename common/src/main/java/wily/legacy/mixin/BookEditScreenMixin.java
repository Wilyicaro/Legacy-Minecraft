package wily.legacy.mixin;

import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.BookEditScreen;
import net.minecraft.client.gui.screens.inventory.PageButton;
import net.minecraft.client.renderer.Rect2i;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.Style;
import net.minecraft.util.FormattedCharSequence;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyArg;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import wily.legacy.client.controller.BindingState;
import wily.legacy.client.controller.Controller;
import wily.legacy.client.controller.ControllerBinding;
import wily.legacy.client.screen.BookPanel;
import wily.legacy.client.screen.ConfirmationScreen;
import wily.legacy.client.screen.ControlTooltip;

import java.util.function.Predicate;

@Mixin(BookEditScreen.class)
public abstract class BookEditScreenMixin extends Screen implements Controller.Event {
    @Shadow protected abstract void clearDisplayCache();

    @Shadow private Button signButton;
    @Shadow private boolean isSigning;

    @Shadow protected abstract void updateButtonVisibility();

    @Shadow private Button doneButton;

    @Shadow protected abstract void saveChanges(boolean bl);

    @Shadow private Button finalizeButton;
    @Shadow private Button cancelButton;
    @Shadow private PageButton forwardButton;
    @Shadow private PageButton backButton;

    @Shadow protected abstract void pageBack();

    @Shadow protected abstract void pageForward();

    @Shadow private int frameTick;
    @Shadow @Final private static Component EDIT_TITLE_LABEL;
    @Shadow @Final private Component ownerText;
    @Shadow @Final private static Component FINALIZE_WARNING_LABEL;
    @Shadow private Component pageMsg;
    private static final Component EXIT_BOOK = Component.translatable("legacy.menu.exit_book");
    private static final Component EXIT_BOOK_MESSAGE = Component.translatable("legacy.menu.exit_book_message");
    @Shadow protected abstract BookEditScreen.DisplayCache getDisplayCache();

    @Shadow protected abstract void renderHighlight(GuiGraphics arg, Rect2i[] args);

    @Shadow protected abstract void renderCursor(GuiGraphics arg, BookEditScreen.Pos2i arg2, boolean bl);

    @Shadow @Final private static FormattedCharSequence BLACK_CURSOR;
    @Shadow @Final private static FormattedCharSequence GRAY_CURSOR;
    @Shadow private String title;

    @Shadow protected abstract boolean titleKeyPressed(int i, int j, int k);

    @Shadow protected abstract boolean bookKeyPressed(int i, int j, int k);

    @Shadow protected abstract int getNumPages();

    @Shadow private int currentPage;
    @Shadow private boolean isModified;
    private BookPanel panel = new BookPanel(this);
    private ControlTooltip.Renderer controlTooltipRender = ControlTooltip.defaultScreen(this)
            .add(()->ControlTooltip.getActiveType().isKeyboard() ? getFocused() == panel ? null : ControlTooltip.getKeyIcon(InputConstants.KEY_LEFT,true) : ControllerBinding.LEFT_BUMPER.bindingState.getIcon(true), ()-> currentPage != 0 ? ControlTooltip.CONTROL_ACTION_CACHE.getUnchecked("legacy.action.previous_page") : null)
            .add(()->ControlTooltip.getActiveType().isKeyboard() ? getFocused() == panel ? null : ControlTooltip.getKeyIcon(InputConstants.KEY_RIGHT,true) : ControllerBinding.RIGHT_BUMPER.bindingState.getIcon(true), ()-> ControlTooltip.CONTROL_ACTION_CACHE.getUnchecked(this.currentPage < this.getNumPages() - 1 ? "legacy.action.next_page" : "legacy.action.add_page"));
    protected BookEditScreenMixin(Component component) {
        super(component);
    }
    @ModifyArg(method = "<init>", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/font/TextFieldHelper;<init>(Ljava/util/function/Supplier;Ljava/util/function/Consumer;Ljava/util/function/Supplier;Ljava/util/function/Consumer;Ljava/util/function/Predicate;)V"), index = 4)
    private Predicate<String> changeTextFieldHelperWidth(Predicate<String> predicate){
        return string-> string.length() < 2304 && this.font.wordWrapHeight(string, 159) <= 176;
    }
    @ModifyArg(method = "rebuildDisplayCache", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/StringSplitter;splitLines(Ljava/lang/String;ILnet/minecraft/network/chat/Style;ZLnet/minecraft/client/StringSplitter$LinePosConsumer;)V"), index = 1)
    private int rebuildDisplayCache(int i) {
        return 159;
    }
    @Inject(method = "convertScreenToLocal", at = @At("HEAD"), cancellable = true)
    private void convertScreenToLocal(BookEditScreen.Pos2i pos2i, CallbackInfoReturnable<BookEditScreen.Pos2i> cir) {
        cir.setReturnValue(new BookEditScreen.Pos2i(pos2i.x - panel.x - 20, pos2i.y - panel.y - 37));
    }
    @Inject(method = "convertLocalToScreen", at = @At("HEAD"), cancellable = true)

    private void convertLocalToScreen(BookEditScreen.Pos2i pos2i, CallbackInfoReturnable<BookEditScreen.Pos2i> cir) {
        cir.setReturnValue(new BookEditScreen.Pos2i(pos2i.x + panel.x + 20, pos2i.y + panel.y + 37));
    }

    @Override
    public void onClose() {
        if (isModified) {
            minecraft.setScreen(new ConfirmationScreen(this,EXIT_BOOK,EXIT_BOOK_MESSAGE,b-> minecraft.setScreen(null)));
        }else super.onClose();
    }
    @Inject(method = "init",at = @At("HEAD"), cancellable = true)
    public void init(CallbackInfo ci) {
        ci.cancel();
        this.clearDisplayCache();
        panel.init();
        addRenderableWidget(panel);
        this.signButton = this.addRenderableWidget(Button.builder(Component.translatable("book.signButton"), (button) -> {
            this.isSigning = true;
            this.updateButtonVisibility();
        }).bounds(this.width / 2 - 108, panel.y + panel.height + 5, 100, 20).build());
        this.doneButton = this.addRenderableWidget(Button.builder(CommonComponents.GUI_DONE, (button) -> {
            this.minecraft.setScreen(null);
            this.saveChanges(false);
        }).bounds(this.width / 2 + 8, panel.y + panel.height + 5, 100, 20).build());
        this.finalizeButton = this.addRenderableWidget(Button.builder(Component.translatable("book.finalizeButton"), (button) -> {
            if (this.isSigning) {
                this.saveChanges(true);
                this.minecraft.setScreen(null);
            }
        }).bounds(this.width / 2 - 108, panel.y + panel.height + 5, 100, 20).build());
        this.cancelButton = this.addRenderableWidget(Button.builder(CommonComponents.GUI_CANCEL, (button) -> {
            if (this.isSigning) this.isSigning = false;
            this.updateButtonVisibility();
        }).bounds(this.width / 2 + 8, panel.y + panel.height + 5, 100, 20).build());

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
        int n;
        int o;
        if (this.isSigning) {
            boolean bl = this.frameTick / 6 % 2 == 0;
            FormattedCharSequence formattedCharSequence = FormattedCharSequence.composite(FormattedCharSequence.forward(this.title, Style.EMPTY), bl ? BLACK_CURSOR : GRAY_CURSOR);
            guiGraphics.drawString(this.font, EDIT_TITLE_LABEL, panel.x + 20, panel.y + 37, 0, false);
            guiGraphics.drawString(this.font, formattedCharSequence, panel.x + 20, panel.y + 50, 0, false);
            guiGraphics.drawString(this.font, this.ownerText, panel.x + 20, panel.y + 61, 0, false);
            guiGraphics.drawWordWrap(this.font, FINALIZE_WARNING_LABEL, panel.x + 20, panel.y + 85, 159, 0);
        } else {
            guiGraphics.drawString(this.font, this.pageMsg, panel.x + panel.width - 24 - font.width(pageMsg), panel.y + 22, 0, false);
            BookEditScreen.DisplayCache displayCache = this.getDisplayCache();
            BookEditScreen.LineInfo[] var15 = displayCache.lines;
            n = var15.length;

            for(o = 0; o < n; ++o) {
                BookEditScreen.LineInfo lineInfo = var15[o];
                guiGraphics.drawString(this.font, lineInfo.asComponent, lineInfo.x, lineInfo.y, -16777216, false);
            }
            if (panel.isFocused()) {
                this.renderHighlight(guiGraphics, displayCache.selection);
                this.renderCursor(guiGraphics, displayCache.cursor, displayCache.cursorAtEnd);
            }
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
        if ((ControlTooltip.getActiveType().isKeyboard() && (i == InputConstants.KEY_RIGHT || i == InputConstants.KEY_LEFT)) && getFocused() != panel){
            (i == InputConstants.KEY_RIGHT ? forwardButton : backButton).keyPressed(InputConstants.KEY_RETURN,0,0);
            cir.setReturnValue(true);
            return;
        }
        if (super.keyPressed(i, j, k)) {
            cir.setReturnValue(true);
        } else if (this.isSigning) {
            cir.setReturnValue(titleKeyPressed(i,j,k));
        } else {
            boolean bl = this.bookKeyPressed(i, j, k);
            if (bl) {
                this.clearDisplayCache();
                cir.setReturnValue(true);
            } else {
                cir.setReturnValue(false);
            }
        }
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}
