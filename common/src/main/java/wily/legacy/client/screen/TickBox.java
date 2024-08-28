package wily.legacy.client.screen;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.components.*;
import net.minecraft.client.gui.narration.NarratedElementType;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import wily.legacy.Legacy4J;
import wily.legacy.client.CommonColor;
import wily.legacy.client.LegacyGuiGraphics;
import wily.legacy.util.ScreenUtil;

import java.util.function.Consumer;
import java.util.function.Function;

public class TickBox extends AbstractButton {
    public static final ResourceLocation[] SPRITES = new ResourceLocation[]{new ResourceLocation(Legacy4J.MOD_ID, "widget/tickbox"), new ResourceLocation(Legacy4J.MOD_ID, "widget/tickbox_hovered")};
    public static final ResourceLocation TICK = new ResourceLocation(Legacy4J.MOD_ID, "widget/tick");
    protected final Function<Boolean,Component> message;
    protected Function<Boolean,Tooltip> tooltip;
    private final Consumer<TickBox> onPress;

    public boolean selected;

    public TickBox(int i, int j,int width, int height, boolean initialState,Function<Boolean, Component> message,Function<Boolean, Tooltip> tooltip, Consumer<TickBox> onPress) {
        super(i, j, width, height, message.apply(false));
        this.selected = initialState;
        this.message = message;
        this.tooltip = tooltip;
        this.onPress = onPress;
        setTooltip(tooltip.apply(selected));
    }
    public TickBox(int i, int j,int width, boolean initialState,Function<Boolean, Component> message,Function<Boolean, Tooltip> tooltip, Consumer<TickBox> onPress){
        this(i,j,width,12,initialState,message,tooltip,onPress);
    }
    public TickBox(int i, int j, boolean initialState,Function<Boolean, Component> message,Function<Boolean, Tooltip> tooltip, Consumer<TickBox> onPress){
        this(i,j,200,initialState,message,tooltip,onPress);
    }
    @Override
    public void onPress() {
        selected = !selected;
        onPress.accept(this);
        setTooltip(tooltip.apply(selected));
    }

    @Override
    protected void renderWidget(PoseStack poseStack, int i, int j, float f) {
        setAlpha(active ? 1.0F : 0.5F);
        Minecraft minecraft = Minecraft.getInstance();
        poseStack.setColor(1.0f, 1.0f, 1.0f, this.alpha);
        RenderSystem.enableBlend();
        RenderSystem.enableDepthTest();
        LegacyGuiGraphics.of(poseStack).blitSprite(SPRITES[isHoveredOrFocused() ? 1 : 0], this.getX(), this.getY(), 12, 12);
        if (selected) LegacyGuiGraphics.of(poseStack).blitSprite(TICK, this.getX(), this.getY(), 14, 12);
        int k = isHoveredOrFocused() ? ScreenUtil.getDefaultTextColor() : CommonColor.INVENTORY_GRAY_TEXT.get();
        poseStack.setColor(1.0f, 1.0f, 1.0f, 1.0F);
        this.renderString(poseStack, minecraft.font, k);
    }


    @Override
    public Component getMessage() {
        return message.apply(selected);
    }

    @Override
    protected void updateWidgetNarration(NarrationElementOutput narrationElementOutput) {
        narrationElementOutput.add(NarratedElementType.TITLE, this.createNarrationMessage());
        if (this.active) {
            Component component = createNarrationMessage();
            if (this.isFocused()) {
                narrationElementOutput.add(NarratedElementType.USAGE, Component.translatable("narration.cycle_button.usage.focused", component));
            } else {
                narrationElementOutput.add(NarratedElementType.USAGE, Component.translatable("narration.cycle_button.usage.hovered", component));
            }
        }
    }

    @Override
    public void renderString(PoseStack poseStack, Font font, int i) {
        ScreenUtil.renderScrollingString(poseStack, font, this.getMessage(), this.getX() + 14, this.getY(), this.getX() + this.getWidth(), this.getY() + this.getHeight(), i,isHoveredOrFocused());
    }
}
