package wily.legacy.client.screen;

import net.minecraft.Util;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.network.chat.Component;
import wily.factoryapi.base.Bearer;
import wily.factoryapi.base.client.AdvancedTextWidget;
import wily.factoryapi.base.client.UIAccessor;
import wily.legacy.client.CommonColor;
import wily.legacy.client.KnownListing;
import wily.legacy.client.LegacyOptions;
import wily.legacy.util.LegacyComponents;
import wily.legacy.util.LegacySprites;
import wily.legacy.util.client.LegacyFontUtil;
import wily.legacy.util.client.LegacyRenderUtil;

import java.util.Collections;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;

public class ConfirmationScreen extends OverlayPanelScreen implements RenderableVList.Access {
    protected final AdvancedTextWidget messageLabel;
    protected final RenderableVList renderableVList = new RenderableVList(accessor).layoutSpacing(l -> LegacyOptions.getUIMode().isSD() ? 1 : 2);
    private final List<RenderableVList> renderableVLists = Collections.singletonList(renderableVList);
    private final Consumer<ConfirmationScreen> textWidgetConsumer;
    public Button okButton;
    protected Consumer<ConfirmationScreen> okAction;
    protected Bearer<Integer> messageYOffset = Bearer.of(0);
    protected boolean initialized = false;

    public ConfirmationScreen(Screen parent, Function<ConfirmationScreen, Panel> panelConstructor, Component title, Consumer<ConfirmationScreen> textWidgetConsumer, Consumer<ConfirmationScreen> okAction) {
        super(parent, s -> panelConstructor.apply((ConfirmationScreen) s), title);
        this.textWidgetConsumer = textWidgetConsumer;
        this.messageLabel = new AdvancedTextWidget(UIAccessor.of(this));
        this.okAction = okAction;
        this.parent = parent;
    }

    public ConfirmationScreen(Screen parent, Supplier<Integer> imageWidth, Supplier<Integer> baseHeight, Supplier<Integer> xOffset, Supplier<Integer> yOffset, Component title, Consumer<ConfirmationScreen> textWidgetConsumer, Consumer<ConfirmationScreen> okAction) {
        this(parent, s -> Panel.createPanel(s, p -> p.appearance(LegacyOptions.getUIMode().isSD() ? LegacySprites.PANEL : LegacySprites.SMALL_PANEL, imageWidth.get(), baseHeight.get() + s.messageLabel.height), p -> p.pos(p.centeredLeftPos(s) + xOffset.get(), p.centeredTopPos(s) + yOffset.get())), title, textWidgetConsumer, okAction);
    }

    public ConfirmationScreen(Screen parent, Supplier<Integer> imageWidth, Supplier<Integer> baseHeight, Supplier<Integer> xOffset, Supplier<Integer> yOffset, Component title, Component message, Consumer<ConfirmationScreen> okAction) {
        this(parent, imageWidth, baseHeight, xOffset, yOffset, title, w -> w.messageLabel.lineSpacing(LegacyOptions.getUIMode().isSD() ? 8 : 12).withLines(message, imageWidth.get() - (LegacyOptions.getUIMode().isSD() ? 12 : 30)), okAction);
    }

    public ConfirmationScreen(Screen parent, Supplier<Integer> imageWidth, Supplier<Integer> baseHeight, Component title, Component message, Consumer<ConfirmationScreen> okAction) {
        this(parent, imageWidth, baseHeight, () -> 0, () -> 0, title, message, okAction);
    }

    public ConfirmationScreen(Screen parent, Component title, Component message, Consumer<ConfirmationScreen> okAction) {
        this(parent, ConfirmationScreen::getPanelWidth, ConfirmationScreen::getBaseHeight, title, message, okAction);
    }

    public ConfirmationScreen(Screen parent, Supplier<Integer> imageWidth, Supplier<Integer> baseHeight, Component title, Component message) {
        this(parent, imageWidth, baseHeight, title, message, LegacyScreen::onClose);
    }

    public ConfirmationScreen(Screen parent, Component title, Component message) {
        this(parent, title, message, LegacyScreen::onClose);
    }

    public static ConfirmationScreen createInfoScreen(Screen parent, Component title, Component message) {
        return new ConfirmationScreen(parent, ConfirmationScreen::getPanelWidth, () -> LegacyOptions.getUIMode().isSD() ? 50 : 75, title, message) {
            protected void addButtons() {
                renderableVList.addRenderable(okButton = Button.builder(Component.translatable("gui.ok"), b -> okAction.accept(this)).build());
            }
        };
    }

    public static ConfirmationScreen createLinkScreen(Screen parent, String link) {
        return createLinkScreen(parent, LegacyComponents.OPEN_LINK_TITLE, LegacyComponents.OPEN_LINK_MESSAGE, link);
    }

    public static ConfirmationScreen createLinkScreen(Screen parent, Component title, Component message, String link) {
        return new ConfirmationScreen(parent, ConfirmationScreen::getPanelWidth, ConfirmationScreen::getBaseHeight, title, message, s -> {
            Util.getPlatform().openUri(link);
            s.onClose();
        });
    }

    public static ConfirmationScreen createResetKnownListingScreen(Screen parent, Component title, Component message, KnownListing<?> knownListing) {
        return new ConfirmationScreen(parent, ConfirmationScreen::getPanelWidth, ConfirmationScreen::getBaseHeight, title, message, s -> {
            knownListing.list.clear();
            knownListing.save();
            s.onClose();
        });
    }

    public static ConfirmationScreen createSaveInfoScreen(Screen parent) {
        Supplier<Integer> imageWidth = () -> LegacyOptions.getUIMode().isSD() ? 200 : 275;
        return new ConfirmationScreen(parent, imageWidth, () -> LegacyOptions.getUIMode().isSD() ? 92 : 130, () -> 0, () -> LegacyOptions.getUIMode().isSD() ? 0 : 25, Component.empty(), w -> w.messageLabel.lineSpacing(LegacyOptions.getUIMode().isSD() ? 8 : 12).withLines(LegacyComponents.AUTOSAVE_MESSAGE, imageWidth.get() - (LegacyOptions.getUIMode().isSD() ? 24 : 55)), LegacyScreen::onClose) {
            protected void addButtons() {
                darkBackground = false;
                renderableVList.addRenderable(okButton = Button.builder(Component.translatable("gui.ok"), b -> okAction.accept(this)).build());
            }

            @Override
            public void renderableVListInit() {
                boolean sd = LegacyOptions.getUIMode().isSD();
                messageYOffset.set(sd ? 57 : 68);
                okButton.setWidth(sd ? 150 : 200);
                okButton.setHeight(sd ? 18 : 20);
                int listWidth = LegacyOptions.getUIMode().isSD() ? panel.width - 24 : panel.width - 55;
                renderableVList.init(panel.x + (panel.width - listWidth) / 2, panel.y + panel.height - (sd ? 28 : 40), listWidth, 0);
            }

            @Override
            public boolean shouldCloseOnEsc() {
                return false;
            }

            public void render(GuiGraphics guiGraphics, int i, int j, float f) {
                super.render(guiGraphics, i, j, f);
                LegacyRenderUtil.drawAutoSavingIcon(guiGraphics, panel.x + (panel.width - 24) / 2, panel.y + (LegacyOptions.getUIMode().isSD() ? 28 : 36));
            }
        };
    }

    public static int getPanelWidth() {
        return LegacyOptions.getUIMode().isSD() ? 144 : 230;
    }

    public static int getBaseHeight() {
        return LegacyOptions.getUIMode().isSD() ? 68 : 97;
    }

    @Override
    protected void init() {
        if (!initialized) {
            addButtons();
            initialized = true;
        }
        LegacyFontUtil.applySDFont(b -> textWidgetConsumer.accept(this));
        super.init();
        renderableVListInit();
        accessor.putIntegerBearer("messageYOffset", messageYOffset);
    }

    @Override
    public List<RenderableVList> getRenderableVLists() {
        return renderableVLists;
    }

    @Override
    public void renderableVListInit() {
        messageYOffset.set(title.getString().isEmpty() ? LegacyOptions.getUIMode().isSD() ? 6 : 15 : LegacyOptions.getUIMode().isSD() ? 18 : 35);
        int listWidth = LegacyOptions.getUIMode().isSD() ? panel.width - 12 : panel.width - 30;
        initRenderableVListHeight(LegacyOptions.getUIMode().isSD() ? 18 : 20);
        renderableVList.init(panel.x + (panel.width - listWidth) / 2, panel.y + panel.height - renderableVList.renderables.size() * (LegacyOptions.getUIMode().isSD() ? 19 : 22) - (LegacyOptions.getUIMode().isSD() ? 6 : 8), listWidth, 0);
    }

    @Override
    public boolean keyPressed(KeyEvent keyEvent) {
        if (renderableVList.keyPressed(keyEvent.key())) return true;
        return super.keyPressed(keyEvent);
    }

    protected void addButtons() {
        renderableVList.addRenderable(Button.builder(Component.translatable("gui.cancel"), b -> this.onClose()).build());
        renderableVList.addRenderable(okButton = Button.builder(Component.translatable("gui.ok"), b -> okAction.accept(this)).build());
    }

    @Override
    public void render(GuiGraphics guiGraphics, int i, int j, float f) {
        super.render(guiGraphics, i, j, f);
        int textX = panel.x + (panel.width - messageLabel.width) / 2;
        LegacyFontUtil.applySDFont(b -> {
            LegacyRenderUtil.renderScrollingString(guiGraphics, font, title, textX, panel.y + (b ? 6 : 15), textX + messageLabel.width, panel.y + (b ? 6 : 15) + 11, CommonColor.INVENTORY_GRAY_TEXT.get(), false);
            messageLabel.withPos(textX, panel.y + messageYOffset.get()).withColor(CommonColor.INVENTORY_GRAY_TEXT.get()).withShadow(false).render(guiGraphics, i, j, f);
        });
    }
}
