package wily.legacy.client.screen;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.MultiLineLabel;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import wily.legacy.client.CommonColor;
import wily.legacy.util.ScreenUtil;

import java.util.function.Consumer;
import java.util.function.Predicate;

public class ConfirmationScreen extends OverlayPanelScreen{
    protected final MultiLineLabel messageLabel;
    protected Predicate<Button> okAction;
    public Button okButton;
    protected int messageYOffset;

    public ConfirmationScreen(Screen parent, int imageWidth, int imageHeight, Component title, MultiLineLabel messageLabel, Predicate<Button> okAction) {
        super(imageWidth, imageHeight, title);
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
    public ConfirmationScreen(Screen parent, int imageWidth, int baseHeight, Component title,Component message) {
        this(parent,imageWidth, baseHeight,title,MultiLineLabel.create(Minecraft.getInstance().font,message,imageWidth - 30));
    }
    public ConfirmationScreen(Screen parent, Component title,Component message) {
        this(parent,230,97,title,message);
    }
    public static ConfirmationScreen createInfoScreen(Screen parent, Component title,Component message) {
        return new ConfirmationScreen(parent,title,message){
            protected void initButtons() {
                okButton = addRenderableWidget(Button.builder(Component.translatable("gui.ok"),b-> {if (okAction.test(b)) onClose();}).bounds(panel.x + 15, panel.y + panel.height - 30,200,20).build());
            }
        };
    }

    @Override
    protected void init() {
        super.init();
        initButtons();
    }
    protected void initButtons(){
        addRenderableWidget(Button.builder(Component.translatable("gui.cancel"), b-> this.onClose()).bounds(panel.x + (panel.width - 200) / 2, panel.y + panel.height - 52,200,20).build());
        okButton = addRenderableWidget(Button.builder(Component.translatable("gui.ok"),b-> {if (okAction.test(b)) onClose();}).bounds(panel.x + (panel.width - 200) / 2, panel.y + panel.height - 30,200,20).build());
    }
    @Override
    public void render(PoseStack poseStack, int i, int j, float f) {
        super.render(poseStack, i, j, f);
        ScreenUtil.renderScrollingString(poseStack,font,title,panel.x + 15, panel.y + 15,panel.x + panel.width - 15, panel.y + 26, CommonColor.INVENTORY_GRAY_TEXT.get(),false);
        messageLabel.renderLeftAlignedNoShadow(poseStack,panel.x + 15, panel.y + messageYOffset, 12, CommonColor.INVENTORY_GRAY_TEXT.get());
    }
}
