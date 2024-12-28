package wily.legacy.client.screen;

import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.*;
import net.minecraft.client.gui.narration.NarratedElementType;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import wily.factoryapi.FactoryAPI;
import wily.factoryapi.base.client.FactoryGuiGraphics;
import wily.legacy.Legacy4J;
import wily.legacy.client.CommonColor;
import wily.legacy.util.ScreenUtil;

import java.util.function.Consumer;
import java.util.function.Function;

public class TickBox extends AbstractButton {
    public static final ResourceLocation[] SPRITES = new ResourceLocation[]{Legacy4J.createModLocation( "widget/tickbox"), Legacy4J.createModLocation( "widget/tickbox_hovered")};
    public static final ResourceLocation TICK = Legacy4J.createModLocation( "widget/tick");
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
    protected void renderWidget(GuiGraphics guiGraphics, int i, int j, float f) {
        setAlpha(active ? 1.0F : 0.5F);
        Minecraft minecraft = Minecraft.getInstance();
        FactoryGuiGraphics.of(guiGraphics).setColor(1.0f, 1.0f, 1.0f, this.alpha);
        RenderSystem.enableBlend();
        RenderSystem.enableDepthTest();
        FactoryGuiGraphics.of(guiGraphics).blitSprite(SPRITES[isHoveredOrFocused() ? 1 : 0], this.getX(), this.getY(), 12, 12);
        if (selected) FactoryGuiGraphics.of(guiGraphics).blitSprite(TICK, this.getX(), this.getY(), 14, 12);
        FactoryGuiGraphics.of(guiGraphics).setColor(1.0f, 1.0f, 1.0f, 1.0F);
        guiGraphics.pose().pushPose();
        if (!isHoveredOrFocused()) guiGraphics.pose().translate(0.5f,0.5f,0f);
        this.renderString(guiGraphics, minecraft.font, isHoveredOrFocused() ? ScreenUtil.getDefaultTextColor() : CommonColor.INVENTORY_GRAY_TEXT.get());
        guiGraphics.pose().popPose();
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
    public void renderString(GuiGraphics guiGraphics, Font font, int i) {
        ScreenUtil.renderScrollingString(guiGraphics, font, this.getMessage(), this.getX() + 13, this.getY(), this.getX() + this.getWidth(), this.getY() + this.getHeight(), i,isHoveredOrFocused());
    }
}
