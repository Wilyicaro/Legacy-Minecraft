package wily.legacy.client.screen;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.MultiLineLabel;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.FormattedText;

import java.util.List;

public class ConfirmationScreen extends PanelBackgroundScreen{
    protected MultiLineLabel messageLines = MultiLineLabel.EMPTY;
    protected final Component message;
    protected Button.OnPress okAction;
    public Button okButton;

    public ConfirmationScreen(Screen parent, int imageWidth, int imageHeight, Component title, Component message, Button.OnPress okAction) {
        super(imageWidth, imageHeight, title);
        this.message = message;
        this.okAction = okAction;
        this.parent = parent;
    }
    public ConfirmationScreen(Screen parent, Component title, Component message, Button.OnPress okAction) {
        this(parent,230, 133,title,message,okAction);
    }

    @Override
    protected void init() {
        super.init();
        initButtons();
        messageLines = MultiLineLabel.create(font,message,panel.width - 30);
        parent.resize(minecraft,width,height);
    }
    protected void initButtons(){
        addRenderableWidget(Button.builder(Component.translatable("gui.cancel"), b-> this.onClose()).bounds(panel.x + 15, panel.y + panel.height - 52,200,20).build());
        okButton = addRenderableWidget(Button.builder(Component.translatable("gui.ok"),okAction).bounds(panel.x + 15, panel.y + panel.height - 30,200,20).build());
    }

    @Override
    public void renderDefaultBackground(GuiGraphics guiGraphics, int i, int j, float f) {
        guiGraphics.pose().translate(0,0,-800);
        parent.render(guiGraphics,0,0,f);
        guiGraphics.pose().translate(0,0,800);
        renderTransparentBackground(guiGraphics);
    }

    @Override
    public void render(GuiGraphics guiGraphics, int i, int j, float f) {
        super.render(guiGraphics, i, j, f);
        guiGraphics.drawString(font,title,panel.x+ 15,panel.y+ 15, 4210752,false);
        if (!message.getString().isEmpty()) {
            messageLines.renderLeftAlignedNoShadow(guiGraphics,panel.x + 15, panel.y + 35, 12, 4210752);
        }
    }
}
