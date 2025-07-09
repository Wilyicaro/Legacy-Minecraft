package wily.legacy.mixin.base.client.book;

import com.llamalad7.mixinextras.sugar.Local;
import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.SharedConstants;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.MultiLineEditBox;
import net.minecraft.client.gui.font.TextFieldHelper;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.BookEditScreen;
import net.minecraft.client.gui.screens.inventory.BookSignScreen;
import net.minecraft.client.gui.screens.inventory.PageButton;
import net.minecraft.client.renderer.Rect2i;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.Style;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.util.StringUtil;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.WritableBookContent;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Final;
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
import wily.legacy.client.screen.ConfirmationScreen;
import wily.legacy.client.screen.ControlTooltip;
import wily.legacy.client.screen.KeyboardScreen;
import wily.legacy.util.LegacyComponents;

import java.util.List;
import java.util.Objects;
import java.util.function.Predicate;

@Mixin(BookEditScreen.class)
public abstract class BookEditScreenMixin extends Screen implements Controller.Event,ControlTooltip.Event {

    @Shadow private PageButton forwardButton;
    @Shadow private PageButton backButton;

    @Shadow protected abstract void pageBack();

    @Shadow protected abstract void pageForward();
    private static final Component EXIT_BOOK = Component.translatable("legacy.menu.exit_book");
    private static final Component EXIT_BOOK_MESSAGE = Component.translatable("legacy.menu.exit_book_message");

    @Shadow protected abstract int getNumPages();

    @Shadow private int currentPage;

    @Shadow protected abstract void saveChanges();

    @Shadow private MultiLineEditBox page;

    @Shadow protected abstract void updatePageContent();

    @Shadow private Component numberOfPages;

    @Shadow protected abstract Component getPageNumberMessage();

    @Unique
    private List<String> initialPages;
    @Shadow @Final private List<String> pages;
    @Shadow @Final private BookSignScreen signScreen;
    @Unique
    private BookPanel panel = new BookPanel(this){
        @Override
        public @Nullable Component getAction(Context context) {
            return page.isFocused() ? context.actionOfContext(KeyContext.class, ControlTooltip::getKeyboardAction) : null;
        }
    };


    @Inject(method = "<init>", at = @At("RETURN"))
    private void initReturn(Player player, ItemStack itemStack, InteractionHand interactionHand, WritableBookContent writableBookContent, CallbackInfo ci) {
        initialPages = List.copyOf(pages);
    }

    @Override
    public void added() {
        super.added();
        ControlTooltip.Renderer.of(this)
                .add(()-> ControlType.getActiveType().isKbm() ? getFocused() == panel ? null : ControlTooltip.COMPOUND_ICON_FUNCTION.apply(new ControlTooltip.Icon[]{ControlTooltip.getKeyIcon(InputConstants.KEY_LSHIFT),ControlTooltip.PLUS_ICON,ControlTooltip.getKeyIcon(InputConstants.KEY_LEFT)}) : ControllerBinding.LEFT_BUMPER.getIcon(), ()-> currentPage != 0 ? LegacyComponents.PREVIOUS_PAGE : null)
                .add(()-> ControlType.getActiveType().isKbm() ? getFocused() == panel ? null : ControlTooltip.COMPOUND_ICON_FUNCTION.apply(new ControlTooltip.Icon[]{ControlTooltip.getKeyIcon(InputConstants.KEY_LSHIFT),ControlTooltip.PLUS_ICON,ControlTooltip.getKeyIcon(InputConstants.KEY_RIGHT)}) : ControllerBinding.RIGHT_BUMPER.getIcon(), ()-> this.currentPage < this.getNumPages() - 1 ? LegacyComponents.NEXT_PAGE : LegacyComponents.ADD_PAGE);
    }

    protected BookEditScreenMixin(Component component) {
        super(component);
    }

    @Override
    public void onClose() {
        if (!pages.equals(initialPages)) {
            minecraft.setScreen(new ConfirmationScreen(this, EXIT_BOOK, EXIT_BOOK_MESSAGE,b-> minecraft.setScreen(null)));
        }
        else super.onClose();
    }

    @Inject(method = "init",at = @At("HEAD"), cancellable = true)
    public void init(CallbackInfo ci) {
        ci.cancel();
        panel.init();
        addRenderableOnly(panel);

        this.page = MultiLineEditBox.builder().setShowDecorations(false).setTextColor(-16777216).setCursorColor(-16777216).setShowBackground(false).setTextShadow(false).setX(panel.x + 20).setY(panel.y + 37).build(this.font, panel.getWidth() - 40, panel.getHeight() - 74, CommonComponents.EMPTY);
        this.page.setCharacterLimit(1024);
        MultiLineEditBox var10000 = this.page;
        Objects.requireNonNull(this.font);
        var10000.setLineLimit(126 / 9);
        this.page.setValueListener((string) -> this.pages.set(this.currentPage, string));
        this.addRenderableWidget(this.page);
        this.updatePageContent();
        this.numberOfPages = this.getPageNumberMessage();
        this.addRenderableWidget(Button.builder(Component.translatable("book.signButton"), button -> this.minecraft.setScreen(this.signScreen)).bounds(this.width / 2 - 108, panel.y + panel.height + 5, 100, 20).build());
        this.addRenderableWidget(Button.builder(CommonComponents.GUI_DONE, (button) -> {
            this.minecraft.setScreen(null);
            this.saveChanges();
        }).bounds(this.width / 2 + 8, panel.y + panel.height + 5, 100, 20).build());

        this.forwardButton = this.addRenderableWidget(panel.createLegacyPageButton(panel.x + panel.width - 62, panel.y + panel.height - 34, true, (button) -> this.pageForward(), true));
        this.backButton = this.addRenderableWidget(panel.createLegacyPageButton(panel.x + 26, panel.y + panel.height - 34, false, (button) -> this.pageBack(), true));
    }

    @Inject(method = "renderBackground", at = @At("HEAD"), cancellable = true)
    public void renderBackground(CallbackInfo ci) {
        ci.cancel();
    }

    @Override
    public void bindingStateTick(BindingState state) {
        if ((state.is(ControllerBinding.RIGHT_BUMPER) || state.is(ControllerBinding.LEFT_BUMPER)) && state.canClick()){
            (state.is(ControllerBinding.RIGHT_BUMPER) ? forwardButton : backButton).keyPressed(InputConstants.KEY_RETURN,0,0);
        }
    }
    @Inject(method = "keyPressed",at = @At("HEAD"), cancellable = true)
    public void keyPressed(int i, int j, int k, CallbackInfoReturnable<Boolean> cir) {
        if ((hasShiftDown() && (i == InputConstants.KEY_RIGHT || i == InputConstants.KEY_LEFT)) && getFocused() != panel){
            (i == InputConstants.KEY_RIGHT ? forwardButton : backButton).keyPressed(InputConstants.KEY_RETURN,0,0);
            cir.setReturnValue(true);
            return;
        }
        cir.setReturnValue(super.keyPressed(i,j,k));
    }

    @ModifyArg(method = "render", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/GuiGraphics;drawString(Lnet/minecraft/client/gui/Font;Lnet/minecraft/network/chat/Component;IIIZ)V"), index = 2)
    public int pageX(int x, @Local(ordinal = 4) int pageWidth) {
        return panel.x + panel.width - 24 - pageWidth;
    }

    @ModifyArg(method = "render", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/GuiGraphics;drawString(Lnet/minecraft/client/gui/Font;Lnet/minecraft/network/chat/Component;IIIZ)V"), index = 3)
    public int pageY(int x) {
        return panel.y + 22;
    }


    @Override
    public boolean isPauseScreen() {
        return false;
    }
}
