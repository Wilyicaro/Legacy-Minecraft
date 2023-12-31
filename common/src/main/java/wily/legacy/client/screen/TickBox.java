package wily.legacy.client.screen;

import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.Util;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.*;
import net.minecraft.client.gui.narration.NarratedElementType;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import wily.legacy.LegacyMinecraft;

import java.util.function.Consumer;
import java.util.function.Function;

public class TickBox extends AbstractButton {
    public static final ResourceLocation[] SPRITES = new ResourceLocation[]{new ResourceLocation(LegacyMinecraft.MOD_ID, "widget/tickbox"), new ResourceLocation(LegacyMinecraft.MOD_ID, "widget/tickbox_hovered")};
    public static final ResourceLocation TICK_SPRITE = new ResourceLocation(LegacyMinecraft.MOD_ID, "widget/tick");
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
    }

    @Override
    protected void renderWidget(GuiGraphics guiGraphics, int i, int j, float f) {
        setAlpha(active ? 1.0F : 0.5F);
        setTooltip(tooltip.apply(selected));
        Minecraft minecraft = Minecraft.getInstance();
        guiGraphics.setColor(1.0f, 1.0f, 1.0f, this.alpha);
        RenderSystem.enableBlend();
        RenderSystem.enableDepthTest();
        guiGraphics.blitSprite(SPRITES[isHoveredOrFocused() ? 1 : 0], this.getX(), this.getY(), 12, 12);
        if (selected) guiGraphics.blitSprite(TICK_SPRITE, this.getX(), this.getY(), 14, 12);
        int k = isHoveredOrFocused() ? 0xFFFFFF : 0x404040;
        guiGraphics.setColor(1.0f, 1.0f, 1.0f, 1.0F);
        this.renderString(guiGraphics, minecraft.font, k);
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
    public static void renderScrollingString(GuiGraphics guiGraphics, Font font, Component component, int j, int k, int l, int m, int n, boolean shadow) {
        int o = font.width(component);
        int p = (k + m - font.lineHeight) / 2 + 1;
        int q = l - j;
        if (o > q) {
            int r = o - q;
            double d = (double) Util.getMillis() / 1000.0;
            double e = Math.max((double)r * 0.5, 3.0);
            double f = Math.sin(1.5707963267948966 * Math.cos(Math.PI * 2 * d / e)) / 2.0 + 0.5;
            double g = Mth.lerp(f, 0.0, r);
            guiGraphics.enableScissor(j, k, l, m);
            guiGraphics.drawString(font, component, j - (int)g, p, n,shadow);
            guiGraphics.disableScissor();
        } else {
            guiGraphics.drawString(font, component, j, p, n,shadow);
        }
    }
    @Override
    public void renderString(GuiGraphics guiGraphics, Font font, int i) {
        int k = this.getX() + 14;
        int l = this.getX() + this.getWidth();
        renderScrollingString(guiGraphics, font, this.getMessage(), k, this.getY(), l, this.getY() + this.getHeight(), i,isHoveredOrFocused());
    }
}
