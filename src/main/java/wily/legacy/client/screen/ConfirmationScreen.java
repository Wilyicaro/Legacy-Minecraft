package wily.legacy.client.screen;

import net.minecraft.Util;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.Renderable;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.util.FormattedCharSequence;
import wily.factoryapi.base.Bearer;
import wily.factoryapi.base.client.AdvancedTextWidget;
import wily.factoryapi.base.client.UIAccessor;
import wily.factoryapi.base.client.WidgetAccessor;
import wily.legacy.client.CommonColor;
import wily.legacy.client.KnownListing;
import wily.legacy.client.LegacyOptions;
import wily.legacy.util.LegacyComponents;
import wily.legacy.util.LegacySprites;
import wily.legacy.util.ScreenUtil;

import java.util.Collections;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.IntSupplier;

public class ConfirmationScreen extends OverlayPanelScreen implements RenderableVList.Access {
    public static int getPanelWidth() {
        return LegacyOptions.getUIMode().isSD() ? 144 : 230;
    }

    public static int getBaseHeight() {
        return LegacyOptions.getUIMode().isSD() ? 68 : 97;
    }

    protected final AdvancedTextWidget messageLabel;
    protected Consumer<ConfirmationScreen> okAction;
    public Button okButton;
    protected Bearer<Integer> messageYOffset = Bearer.of(0);
    protected final RenderableVList renderableVList = new RenderableVList(accessor).layoutSpacing(l -> LegacyOptions.getUIMode().isSD() ? 1 : 2);
    private final List<RenderableVList> renderableVLists = Collections.singletonList(renderableVList);
    protected boolean initialized = false;
    private final Consumer<AdvancedTextWidget> textWidgetConsumer;

    public ConfirmationScreen(Screen parent, Function<ConfirmationScreen, Panel> panelConstructor, Component title, Consumer<AdvancedTextWidget> textWidgetConsumer, Consumer<ConfirmationScreen> okAction) {
        super(parent, s-> panelConstructor.apply((ConfirmationScreen)s), title);
        this.textWidgetConsumer = textWidgetConsumer;
        this.messageLabel = new AdvancedTextWidget(UIAccessor.of(this));
        this.okAction = okAction;
        this.parent = parent;
    }

    public ConfirmationScreen(Screen parent, int imageWidth, int baseHeight, int xOffset, int yOffset, Component title, Consumer<AdvancedTextWidget> textWidgetConsumer, Consumer<ConfirmationScreen> okAction) {
        this(parent, () -> imageWidth, () -> baseHeight, () -> xOffset, () -> yOffset, title, textWidgetConsumer, okAction);
    }

    public ConfirmationScreen(Screen parent, IntSupplier imageWidth, IntSupplier baseHeight, Component title, Consumer<AdvancedTextWidget> textWidgetConsumer, Consumer<ConfirmationScreen> okAction) {
        this(parent, imageWidth, baseHeight, () -> 0, () -> 0, title, textWidgetConsumer, okAction);
    }

    public ConfirmationScreen(Screen parent, IntSupplier imageWidth, IntSupplier baseHeight, IntSupplier xOffset, IntSupplier yOffset, Component title, Consumer<AdvancedTextWidget> textWidgetConsumer, Consumer<ConfirmationScreen> okAction) {
        this(parent,s-> Panel.createPanel(s, p-> {
            int width = imageWidth.getAsInt();
            p.appearance(LegacyOptions.getUIMode().isSD() ? LegacySprites.PANEL : LegacySprites.SMALL_PANEL, width, baseHeight.getAsInt() + s.messageLabel.height + s.titleExtraHeight(width));
        }, p-> p.pos(p.centeredLeftPos(s) + xOffset.getAsInt(), p.centeredTopPos(s) + yOffset.getAsInt())), title, textWidgetConsumer, okAction);
    }

    public ConfirmationScreen(Screen parent, IntSupplier imageWidth, IntSupplier baseHeight, Component title, Component message, Consumer<ConfirmationScreen> okAction) {
        this(parent, imageWidth, baseHeight, title, w -> w.lineSpacing(LegacyOptions.getUIMode().isSD() ? 8 : 12).withLines(message, imageWidth.getAsInt() - (LegacyOptions.getUIMode().isSD() ? 12 : 30)), okAction);
    }

    public ConfirmationScreen(Screen parent, IntSupplier imageWidth, IntSupplier baseHeight, IntSupplier xOffset, IntSupplier yOffset, Component title, Component message, Consumer<ConfirmationScreen> okAction) {
        this(parent, imageWidth, baseHeight, xOffset, yOffset, title, w -> w.lineSpacing(LegacyOptions.getUIMode().isSD() ? 8 : 12).withLines(message, imageWidth.getAsInt() - (LegacyOptions.getUIMode().isSD() ? 12 : 30)), okAction);
    }

    public ConfirmationScreen(Screen parent, int imageWidth, int baseHeight, int xOffset, int yOffset, Component title, Component message, Consumer<ConfirmationScreen> okAction) {
        this(parent, () -> imageWidth, () -> baseHeight, () -> xOffset, () -> yOffset, title, w -> w.lineSpacing(LegacyOptions.getUIMode().isSD() ? 8 : 12).withLines(message, imageWidth - (LegacyOptions.getUIMode().isSD() ? 12 : 30)), okAction);
    }

    public ConfirmationScreen(Screen parent, int imageWidth, int baseHeight, Component title, Component message, Consumer<ConfirmationScreen> okAction) {
        this(parent, imageWidth, baseHeight, 0, 0, title, message, okAction);
    }

    public ConfirmationScreen(Screen parent, IntSupplier imageWidth, IntSupplier baseHeight, Component title, Component message) {
        this(parent, imageWidth, baseHeight, title, message, LegacyScreen::onClose);
    }

    public ConfirmationScreen(Screen parent, Component title, Component message, Consumer<ConfirmationScreen> okAction) {
        this(parent, ConfirmationScreen::getPanelWidth, ConfirmationScreen::getBaseHeight, title, message, okAction);
    }

    public ConfirmationScreen(Screen parent, int imageWidth, int baseHeight, Component title, Component message) {
        this(parent, imageWidth, baseHeight, title, message, LegacyScreen::onClose);
    }

    public ConfirmationScreen(Screen parent, Component title, Component message) {
        this(parent, title, message, LegacyScreen::onClose);
    }

    public static ConfirmationScreen createInfoScreen(Screen parent, Component title,Component message) {
        return new ConfirmationScreen(parent, ConfirmationScreen::getPanelWidth, () -> LegacyOptions.getUIMode().isSD() ? 50 : 75, title, message){
            protected void addButtons() {
                renderableVList.addRenderable(okButton = Button.builder(Component.translatable("gui.ok"),b-> okAction.accept(this)).build());
            }
        };
    }

    public static ConfirmationScreen createLinkScreen(Screen parent, String link) {
        return createLinkScreen(parent, LegacyComponents.OPEN_LINK_TITLE, LegacyComponents.OPEN_LINK_MESSAGE, link);
    }

    public static ConfirmationScreen createLinkScreen(Screen parent, Component title, Component message, String link) {
        return new ConfirmationScreen(parent, ConfirmationScreen::getPanelWidth, ConfirmationScreen::getBaseHeight, title,message, s-> {
            Util.getPlatform().openUri(link);
            s.onClose();
        });
    }

    public static ConfirmationScreen createResetKnownListingScreen(Screen parent, Component title, Component message, KnownListing<?> knownListing) {
        return new ConfirmationScreen(parent, ConfirmationScreen::getPanelWidth, ConfirmationScreen::getBaseHeight, title,message, s-> {
            knownListing.list.clear();
            knownListing.save();
            s.onClose();
        });
    }

    public static ConfirmationScreen createSaveInfoScreen(Screen parent){
        return new ConfirmationScreen(parent, () -> LegacyOptions.getUIMode().isSD() ? 184 : 275, () -> LegacyOptions.getUIMode().isSD() ? 92 : 130, () -> 0, () -> LegacyOptions.getUIMode().isSD() ? 12 : 25, Component.empty(), LegacyComponents.AUTOSAVE_MESSAGE, LegacyScreen::onClose){
            protected void addButtons() {
                transparentBackground = false;
                renderableVList.addRenderable(okButton = Button.builder(Component.translatable("gui.ok"), b-> okAction.accept(this)).build());
            }
            @Override
            public void renderableVListInit(){
                boolean sd = LegacyOptions.getUIMode().isSD();
                int listWidth = sd ? panel.width - 30 : 220;
                messageYOffset.set(sd ? 48 : 68);
                renderableVList.init(panel.x + (panel.width - listWidth) / 2, panel.y + panel.height - (sd ? 29 : 40), listWidth, 0);
            }

            @Override
            public boolean shouldCloseOnEsc() {
                return false;
            }

            public void render(GuiGraphics guiGraphics, int i, int j, float f) {
                super.render(guiGraphics, i, j, f);
                ScreenUtil.drawAutoSavingIcon(guiGraphics,panel.x + panel.width / 2 - 10, panel.y + (LegacyOptions.getUIMode().isSD() ? 25 : 36));
            }
        };
    }

    @Override
    protected void init() {
        if (!initialized){
            addButtons();
            initialized = true;
        }
        ScreenUtil.applySDFont(ignored -> textWidgetConsumer.accept(messageLabel));
        super.init();
        renderableVListInit();
        accessor.putIntegerBearer("messageYOffset", messageYOffset);
    }

    @Override
    public List<RenderableVList> getRenderableVLists() {
        return renderableVLists;
    }

    @Override
    public void initRenderableVListEntry(RenderableVList renderableVList, Renderable renderable) {
        if (renderable instanceof AbstractWidget widget) {
            //? if <=1.20.1 {
            /*((WidgetAccessor) widget).setHeight(accessor.getInteger("buttonsHeight", LegacyOptions.getUIMode().isSD() ? 18 : 20));
            *///?} else {
            widget.setHeight(accessor.getInteger("buttonsHeight", LegacyOptions.getUIMode().isSD() ? 18 : 20));
            //?}
        }
    }

    public void renderableVListInit(){
        boolean sd = LegacyOptions.getUIMode().isSD();
        int titleY = sd ? 6 : 15;
        messageYOffset.set(title.getString().isEmpty() ? titleY : titleY + titleLines(panel.width).size() * font.lineHeight + (sd ? 4 : 11));
        int listWidth = sd ? panel.width - 12 : panel.width - 30;
        int itemHeight = sd ? 19 : 22;
        int bottomPadding = sd ? 6 : 8;
        renderableVList.init(panel.x + (panel.width - listWidth) / 2, panel.y + panel.height - renderableVList.renderables.size() * itemHeight - bottomPadding,listWidth,0);
    }

    protected List<FormattedCharSequence> titleLines(int panelWidth) {
        return font.split(title, Math.max(1, panelWidth - (LegacyOptions.getUIMode().isSD() ? 12 : 30)));
    }

    protected int titleExtraHeight(int panelWidth) {
        return title.getString().isEmpty() ? 0 : Math.max(0, titleLines(panelWidth).size() - 1) * font.lineHeight;
    }

    @Override
    public boolean keyPressed(int i, int j, int k) {
        if (renderableVList.keyPressed(i)) return true;
        return super.keyPressed(i, j, k);
    }

    protected void addButtons(){
        renderableVList.addRenderable(Button.builder(Component.translatable("gui.cancel"), b-> this.onClose()).build());
        renderableVList.addRenderable(okButton = Button.builder(Component.translatable("gui.ok"),b-> okAction.accept(this)).build());
    }

    @Override
    public void render(GuiGraphics guiGraphics, int i, int j, float f) {
        //? if <=1.20.1 {
        if (messageLabel.getLines().isEmpty()) {
            textWidgetConsumer.accept(messageLabel);
            repositionElements();
        }
        //?}
        super.render(guiGraphics, i, j, f);
        int textX = panel.x + (LegacyOptions.getUIMode().isSD() ? 6 : 15);
        ScreenUtil.applySDFont(ignored -> {
            List<FormattedCharSequence> titleLines = titleLines(panel.width);
            for (int line = 0; line < titleLines.size(); line++) {
                guiGraphics.drawString(font, titleLines.get(line), textX, panel.y + (LegacyOptions.getUIMode().isSD() ? 6 : 15) + line * font.lineHeight, CommonColor.INVENTORY_GRAY_TEXT.get(), false);
            }
            messageLabel.withPos(textX, panel.y + messageYOffset.get()).withColor(CommonColor.INVENTORY_GRAY_TEXT.get()).withShadow(false).render(guiGraphics, i, j, f);
        });
    }
}
