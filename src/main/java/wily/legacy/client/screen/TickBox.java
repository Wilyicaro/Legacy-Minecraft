package wily.legacy.client.screen;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.*;
import net.minecraft.client.gui.narration.NarratedElementType;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.client.input.InputWithModifiers;
import net.minecraft.network.chat.Component;
import wily.factoryapi.base.client.FactoryGuiGraphics;
import wily.factoryapi.util.FactoryScreenUtil;
import wily.legacy.client.CommonColor;
import wily.legacy.client.LegacyOptions;
import wily.legacy.util.LegacySprites;
import wily.legacy.util.client.LegacyFontUtil;
import wily.legacy.util.client.LegacyRenderUtil;

import java.util.function.BooleanSupplier;
import java.util.function.Consumer;
import java.util.function.Function;

public class TickBox extends AbstractButton {
    protected final Function<Boolean, Component> message;
    private final Consumer<TickBox> onPress;
    public boolean selected;
    protected Function<Boolean, Tooltip> tooltip;
    protected final BooleanSupplier selectedGetter;

    public TickBox(int i, int j, int width, int height, boolean initialState, Function<Boolean, Component> message, Function<Boolean, Tooltip> tooltip, Consumer<TickBox> onPress, BooleanSupplier selectedGetter) {
        super(i, j, width, height, message.apply(false));
        this.selected = initialState;
        this.message = message;
        this.tooltip = tooltip;
        this.onPress = onPress;
        this.selectedGetter = selectedGetter;
        setTooltip(tooltip.apply(selected));
    }

    public TickBox(int i, int j, int width, boolean initialState, Function<Boolean, Component> message, Function<Boolean, Tooltip> tooltip, Consumer<TickBox> onPress, BooleanSupplier selectedGetter) {
        this(i, j, width, getDefaultHeight(), initialState, message, tooltip, onPress, selectedGetter);
    }

    public TickBox(int i, int j, int width, boolean initialState, Function<Boolean, Component> message, Function<Boolean, Tooltip> tooltip, Consumer<TickBox> onPress) {
        this(i, j, width, initialState, message, tooltip, onPress, null);
    }

    public TickBox(int i, int j, boolean initialState, Function<Boolean, Component> message, Function<Boolean, Tooltip> tooltip, Consumer<TickBox> onPress) {
        this(i, j, 200, initialState, message, tooltip, onPress);
    }

    public static int getDefaultHeight() {
        return LegacyOptions.getUIMode().isSD() ? 9 : 12;
    }

    @Override
    public void onPress(InputWithModifiers input) {
        selected = !selected;
        onPress.accept(this);
        updateMessage();
    }

    public void updateMessage() {
        setTooltip(tooltip.apply(selected));
    }

    public void updateHeight() {
        setHeight(getDefaultHeight());
    }

    @Override
    protected void renderWidget(GuiGraphics guiGraphics, int i, int j, float f) {
        setAlpha(active ? 1.0F : 0.5F);
        Minecraft minecraft = Minecraft.getInstance();
        FactoryGuiGraphics.of(guiGraphics).setBlitColor(1.0f, 1.0f, 1.0f, this.alpha);
        FactoryScreenUtil.enableBlend();
        FactoryScreenUtil.enableDepthTest();
        FactoryGuiGraphics.of(guiGraphics).blitSprite(isHoveredOrFocused() ? LegacySprites.TICKBOX_HOVERED : LegacySprites.TICKBOX, this.getX(), this.getY(), getHeight(), getHeight());
        if (selected) {
            if (LegacyOptions.getUIMode().isSD())
                FactoryGuiGraphics.of(guiGraphics).blitSprite(LegacySprites.SMALL_TICK, this.getX(), this.getY(), 11, 9);
            else FactoryGuiGraphics.of(guiGraphics).blitSprite(LegacySprites.TICK, this.getX(), this.getY(), 14, 12);
        }
        FactoryGuiGraphics.of(guiGraphics).setBlitColor(1.0f, 1.0f, 1.0f, 1.0F);
        guiGraphics.pose().pushMatrix();
        if (!isHoveredOrFocused()) guiGraphics.pose().translate(0.4f, 0.4f);
        this.renderString(guiGraphics, minecraft.font, isHoveredOrFocused() ? LegacyRenderUtil.getDefaultTextColor() : CommonColor.INVENTORY_GRAY_TEXT.get());
        guiGraphics.pose().popMatrix();
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
        LegacyFontUtil.applySDFont(b -> LegacyRenderUtil.renderScrollingString(guiGraphics, font, this.getMessage(), this.getX() + getHeight() + (LegacyOptions.getUIMode().isSD() ? 0 : 1), this.getY(), this.getX() + this.getWidth(), this.getY() + this.getHeight(), i, isHoveredOrFocused()));
    }

    public boolean updateValue() {
        if (selectedGetter != null) {
            boolean oldValue = selected;
            selected = selectedGetter.getAsBoolean();
            if (selected != oldValue) {
                updateMessage();
                return true;
            }
        }
        return false;
    }
}
