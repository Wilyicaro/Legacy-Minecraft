package wily.legacy.client.screen;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractSliderButton;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.navigation.CommonInputs;
import net.minecraft.network.chat.Component;
import wily.legacy.Legacy4JClient;
import wily.legacy.init.LegacySoundEvents;
import wily.legacy.util.ScreenUtil;

import java.util.List;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;

public class LegacySliderButton<T> extends AbstractSliderButton {
    private final Function<LegacySliderButton<T>,Component> messageGetter;

    private final Function<LegacySliderButton<T>, T> valueGetter;
    private final Function<T, Double> valueSetter;
    private final Consumer<LegacySliderButton<T>> onChange;
    private final Supplier<Tooltip> tooltipSupplier;
    private int slidingTime = 1;
    private int lastSliderInput = -1;
    public T objectValue;
    public LegacySliderButton(int i, int j, int k, int l, Function<LegacySliderButton<T>,Component> messageGetter, Supplier<Tooltip> tooltipSupplier, T initialValue, Function<LegacySliderButton<T>,T> valueGetter, Function<T, Double> valueSetter, Consumer<LegacySliderButton<T>>  onChange) {
        super(i, j, k, l, Component.empty(), valueSetter.apply(initialValue));
        this.messageGetter = messageGetter;
        this.valueGetter = valueGetter;
        this.valueSetter = valueSetter;
        this.onChange = onChange;
        this.tooltipSupplier = tooltipSupplier;
        objectValue = valueGetter.apply(this);
        updateMessage();
    }

    public LegacySliderButton(int i, int j, int k, int l, Function<LegacySliderButton<T>,Component> messageGetter, Supplier<Tooltip> tooltipSupplier, T initialValue, Supplier<List<T>> values, Consumer<LegacySliderButton<T>>  onChange) {
        this(i, j, k, l, messageGetter, tooltipSupplier, initialValue, b-> values.get().get((int) Math.round(b.value * (values.get().size() - 1))),t->Math.max(0d,values.get().indexOf(t))/ (values.get().size() - 1),onChange);
        objectValue = initialValue;
    }
    public Component getDefaultMessage(Component caption, Component visibleValue){
        return caption.copy().append(": ").append(visibleValue);
    }
    public T getObjectValue(){
        return objectValue == null ? valueGetter.apply(this) : objectValue;
    }
    public double getValue(){
        return value;
    }

    @Override
    protected void updateMessage() {
        setTooltip(tooltipSupplier.get());
    }

    @Override
    public void renderWidget(GuiGraphics guiGraphics, int i, int j, float f) {
        setMessage(messageGetter.apply(this));
        super.renderWidget(guiGraphics, i, j, f);
    }

    public void setFocused(boolean bl) {
        super.setFocused(bl);
        if (bl) canChangeValue = Legacy4JClient.controllerManager.canChangeSlidersValue;
    }
    @Override
    public boolean keyPressed(int i, int j, int k) {
        if (!active) return false;
        if (CommonInputs.selected(i)) {
            Legacy4JClient.controllerManager.canChangeSlidersValue = this.canChangeValue = !this.canChangeValue;
            return true;
        }
        if (this.canChangeValue) {
            boolean bl = i == 263;
            if ((bl && value > 0) || (i == 262 && value < 1.0)) {
                double part = 1d / (width - 8) * slidingTime;
                T v = getObjectValue();
                while (v.equals(getObjectValue())) {
                    setValue(this.value + (bl ? -part : part));
                    if (part >= 1) break;
                    part= Math.min(part * 2,1);
                }
                if (slidingTime > 0 && i != lastSliderInput) slidingTime = 0;
                lastSliderInput = i;
                slidingTime++;
                return true;
            }
        }
        return false;
    }
    public boolean keyReleased(int i, int j, int k) {
        if (this.canChangeValue && (i == 263 || i== 262)) slidingTime = 1;
        return false;
    }
    @Override
    protected void applyValue() {
        T oldValue = objectValue;
        objectValue = valueGetter.apply(this);
        value = valueSetter.apply(objectValue);
        if (!oldValue.equals(objectValue)){
            ScreenUtil.playSimpleUISound(LegacySoundEvents.SCROLL.get(),1.0f);
            onChange.accept(this);
        }
    }

}
