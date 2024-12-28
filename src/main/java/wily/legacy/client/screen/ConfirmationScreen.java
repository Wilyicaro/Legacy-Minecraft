package wily.legacy.client.screen;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.MultiLineLabel;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.TitleScreen;
import net.minecraft.network.chat.Component;
import wily.factoryapi.base.client.UIDefinition;
import wily.legacy.client.CommonColor;
import wily.legacy.util.LegacyComponents;
import wily.legacy.util.ScreenUtil;

import java.util.Collections;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.Predicate;

public class ConfirmationScreen extends OverlayPanelScreen implements RenderableVList.Access{
    protected final MultiLineLabel messageLabel;
    protected Predicate<Button> okAction;
    public Button okButton;
    protected int messageYOffset;
    protected final RenderableVList renderableVList = new RenderableVList(accessor).layoutSpacing(l->2);
    private final List<RenderableVList> renderableVLists = Collections.singletonList(renderableVList);
    protected boolean initialized = false;

    public ConfirmationScreen(Screen parent, int imageWidth, int imageHeight, Component title, MultiLineLabel messageLabel, Predicate<Button> okAction) {
        super(parent,imageWidth, imageHeight, title);
        this.messageLabel = messageLabel;
        this.okAction = okAction;
        this.parent = parent;
        messageYOffset = title.getString().isEmpty() ? 15 : 35;
    }

    public ConfirmationScreen(Screen parent, int imageWidth, int imageHeight, Component title, Component message, Consumer<Button> okAction) {
        this(parent, imageWidth, imageHeight, title, MultiLineLabel.create(Minecraft.getInstance().font,message,imageWidth - 30), b-> {okAction.accept(b);return false;});
    }
    public ConfirmationScreen(Screen parent, Component title, Component message, Consumer<Button> okAction) {
        this(parent,230, 133,title,message,okAction);
    }
    public ConfirmationScreen(Screen parent, int imageWidth, int baseHeight, Component title, MultiLineLabel messageLines) {
        this(parent, imageWidth, baseHeight + messageLines.getLineCount() * 12,title,messageLines, b-> true);
    }
    public ConfirmationScreen(Screen parent, int imageWidth, int baseHeight, Component title, Component message) {
        this(parent,imageWidth, baseHeight,title,MultiLineLabel.create(Minecraft.getInstance().font,message,imageWidth - 30));
    }
    public ConfirmationScreen(Screen parent, Component title, Component message) {
        this(parent,230,97,title,message);
    }

    public static ConfirmationScreen createInfoScreen(Screen parent, Component title,Component message) {
        return new ConfirmationScreen(parent,title,message){
            protected void addButtons() {
                renderableVList.addRenderable(okButton = Button.builder(Component.translatable("gui.ok"),b-> {if (okAction.test(b)) onClose();}).build());
            }
        };
    }

    public static ConfirmationScreen createSaveInfoScreen(Screen parent){
        return new ConfirmationScreen(parent,275,130,Component.empty(), LegacyComponents.AUTOSAVE_MESSAGE){
            protected void addButtons() {
                transparentBackground = false;
                messageYOffset = 68;
                renderableVList.addRenderable(okButton = Button.builder(Component.translatable("gui.ok"), b-> {if (okAction.test(b)) onClose();}).build());
            }
            @Override
            public void renderableVListInit(){
                panel.y+=25;
                renderableVList.init(panel.x + (panel.width - 220) / 2, panel.y + panel.height - 40,220,0);
            }

            @Override
            public boolean shouldCloseOnEsc() {
                return false;
            }

            public void render(GuiGraphics guiGraphics, int i, int j, float f) {
                super.render(guiGraphics, i, j, f);
                ScreenUtil.drawAutoSavingIcon(guiGraphics,panel.x + 127, panel.y + 36);
            }
        };
    }

    @Override
    protected void init() {
        if (!initialized){
            addButtons();
            initialized = true;
        }
        super.init();
        renderableVListInit();
    }

    @Override
    public List<RenderableVList> getRenderableVLists() {
        return renderableVLists;
    }

    public void renderableVListInit(){
        renderableVList.init(panel.x + (panel.width - 200) / 2, panel.y + panel.height - renderableVList.renderables.size() * 22 - 8,200,0);
    }

    @Override
    public boolean keyPressed(int i, int j, int k) {
        if (renderableVList.keyPressed(i)) return true;
        return super.keyPressed(i, j, k);
    }

    protected void addButtons(){
        renderableVList.addRenderable(Button.builder(Component.translatable("gui.cancel"), b-> this.onClose()).build());
        renderableVList.addRenderable(okButton = Button.builder(Component.translatable("gui.ok"),b-> {if (okAction.test(b)) onClose();}).build());
    }
    @Override
    public void render(GuiGraphics guiGraphics, int i, int j, float f) {
        super.render(guiGraphics, i, j, f);
        ScreenUtil.renderScrollingString(guiGraphics,font,title,panel.x + 15, panel.y + 15,panel.x + panel.width - 15, panel.y + 26, CommonColor.INVENTORY_GRAY_TEXT.get(),false);
        messageLabel.renderLeftAlignedNoShadow(guiGraphics,panel.x + 15, panel.y + messageYOffset, 12, CommonColor.INVENTORY_GRAY_TEXT.get());
    }
}
