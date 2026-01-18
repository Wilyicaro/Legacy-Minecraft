package wily.legacy.client.screen;

import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractSliderButton;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;
import wily.legacy.Legacy4JClient;
import wily.legacy.init.LegacyRegistries;
import wily.legacy.util.client.LegacyFontUtil;
import wily.legacy.util.client.LegacySoundUtil;

import java.util.List;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.IntSupplier;
import java.util.function.Supplier;

public class LegacySliderButton<T> extends AbstractSliderButton {
    private final Function<LegacySliderButton<T>, Component> messageGetter;
    private final Function<LegacySliderButton<T>, T> valueGetter;
    private final Function<T, Double> valueSetter;
    private final Consumer<LegacySliderButton<T>> onChange;
    private final Function<LegacySliderButton<T>, Tooltip> tooltipSupplier;
    protected boolean dragging = false;
    protected boolean applyChangesOnRelease = false;
    protected T objectValue;
    protected final Supplier<T> objectValueSupplier;
    private int slidingMul = 1;
    private int lastSliderInput = -1;
    private double rangeMul = 1;

    public LegacySliderButton(int i, int j, int k, int l, Function<LegacySliderButton<T>, Component> messageGetter, Function<LegacySliderButton<T>, Tooltip> tooltipSupplier, T initialValue, Function<LegacySliderButton<T>, T> valueGetter, Function<T, Double> valueSetter, Consumer<LegacySliderButton<T>> onChange, Supplier<T> objectValueSupplier) {
        super(i, j, k, l, Component.empty(), valueSetter.apply(initialValue));
        this.messageGetter = messageGetter;
        this.valueGetter = valueGetter;
        this.valueSetter = valueSetter;
        this.onChange = onChange;
        this.tooltipSupplier = tooltipSupplier;
        objectValue = initialValue;
        this.objectValueSupplier = objectValueSupplier;
        updateMessage();
    }

    public LegacySliderButton(int i, int j, int k, int l, Function<LegacySliderButton<T>, Component> messageGetter, Function<LegacySliderButton<T>, Tooltip> tooltipSupplier, T initialValue, Supplier<List<T>> values, Consumer<LegacySliderButton<T>> onChange) {
        this(i, j, k, l, messageGetter, tooltipSupplier, initialValue, values, onChange, null);
    }

    public LegacySliderButton(int i, int j, int k, int l, Function<LegacySliderButton<T>, Component> messageGetter, Function<LegacySliderButton<T>, Tooltip> tooltipSupplier, T initialValue, Supplier<List<T>> values, Consumer<LegacySliderButton<T>> onChange, Supplier<T> objectValueSupplier) {
        this(i, j, k, l, messageGetter, tooltipSupplier, initialValue, b -> values.get().get((int) Math.round(b.value * (values.get().size() - 1))), t -> Math.max(0d, values.get().indexOf(t)) / (values.get().size() - 1), onChange, objectValueSupplier);
    }

    public LegacySliderButton(int i, int j, int k, int l, Function<LegacySliderButton<T>, Component> messageGetter, Function<LegacySliderButton<T>, Tooltip> tooltipSupplier, T initialValue, Function<LegacySliderButton<T>, T> valueGetter, Function<T, Double> valueSetter, Consumer<LegacySliderButton<T>> onChange, Supplier<T> objectValueSupplier, double rangeMultiplier) {
        this(i, j, k, l, messageGetter, tooltipSupplier, initialValue, valueGetter, valueSetter, onChange, objectValueSupplier);
        this.rangeMul = rangeMultiplier;
    }

    public static <T> LegacySliderButton<T> createFromInt(int i, int j, int k, int l, Function<LegacySliderButton<T>, Component> messageGetter, Function<LegacySliderButton<T>, Tooltip> tooltipSupplier, T initialValue, Function<Integer, T> valueGetter, Function<T, Integer> valueSetter, Supplier<Integer> valuesSize, Consumer<LegacySliderButton<T>> onChange, Supplier<T> objectValueSupplier) {
        return new LegacySliderButton<>(i, j, k, l, messageGetter, tooltipSupplier, initialValue, b -> valueGetter.apply((int) Math.round(b.value * (valuesSize.get() - 1))), t -> Math.max(0d, valueSetter.apply(t)) / (valuesSize.get() - 1), onChange, objectValueSupplier, -1);
    }

    public static LegacySliderButton<Integer> createFromIntRange(int i, int j, int k, int l, Function<LegacySliderButton<Integer>, Component> messageGetter, Function<LegacySliderButton<Integer>, Tooltip> tooltipSupplier, Integer initialValue, int min, IntSupplier max, Consumer<LegacySliderButton<Integer>> onChange, Supplier<Integer> objectValueSupplier) {
        return new LegacySliderButton<>(i, j, k, l, messageGetter, tooltipSupplier, initialValue, b -> min + (int) Math.round(b.value * (max.getAsInt() - min)), t -> Mth.clamp((double) (t - min) / (max.getAsInt() - min), 0d, 1d), onChange, objectValueSupplier, -1);
    }

    public static LegacySliderButton<Integer> createFromIntRange(int i, int j, int k, int l, Function<LegacySliderButton<Integer>, Component> messageGetter, Function<LegacySliderButton<Integer>, Tooltip> tooltipSupplier, Integer initialValue, int min, int max, Consumer<LegacySliderButton<Integer>> onChange, Supplier<Integer> objectValueSupplier) {
        return createFromIntRange(i, j, k, l, messageGetter, tooltipSupplier, initialValue, min, () -> max, onChange, objectValueSupplier);
    }

    public Component getDefaultMessage(Component caption, Component visibleValue) {
        return CommonComponents.optionNameValue(caption, visibleValue);
    }

    public T getObjectValue() {
        return objectValue == null ? valueGetter.apply(this) : objectValue;
    }

    public void setObjectValue(T objectValue) {
        this.objectValue = objectValue;
        value = valueSetter.apply(objectValue);
    }

    public double getValue() {
        return value;
    }

    @Override
    public void updateMessage() {
        setTooltip(tooltipSupplier.apply(this));
        setMessage(messageGetter.apply(this));
    }

    public void setFocused(boolean bl) {
        super.setFocused(bl);
        if (bl) canChangeValue = Legacy4JClient.controllerManager.canChangeSlidersValue;
    }

    @Override
    public boolean keyPressed(KeyEvent keyEvent) {
        if (!active) return false;
        if (keyEvent.isSelection()) {
            Legacy4JClient.controllerManager.canChangeSlidersValue = this.canChangeValue = !this.canChangeValue;
            return true;
        }
        if (this.canChangeValue) {
            boolean bl = keyEvent.isLeft();
            if ((bl && value > 0) || (keyEvent.isRight() && value < 1.0)) {
                if (slidingMul > 0 && keyEvent.key() != lastSliderInput) slidingMul = 1;
                lastSliderInput = keyEvent.key();
                double part = 1d / (width - 8) * slidingMul;
                double precision = 100 * rangeMul;
                T v = getObjectValue();
                while (v.equals(getObjectValue())) {
                    double newValue = this.value + (bl ? -part : part);
                    double flooredValue = rangeMul != -1 ? Math.floor(newValue * precision) / precision : newValue;
                    setValue(flooredValue + ((int) flooredValue < flooredValue ? 0e-10 : 0));
                    if (part >= 1) break;
                    part = Math.min(part * 2, 1);
                }
                slidingMul++;
                return true;
            }
        }
        return false;
    }

    @Override
    protected void renderScrollingString(GuiGraphics guiGraphics, Font font, int i, int j) {
        LegacyFontUtil.applySDFont(b -> super.renderScrollingString(guiGraphics, font, i, j));
    }

    @Override
    public boolean keyReleased(KeyEvent keyEvent) {
        if (this.canChangeValue && (keyEvent.isLeft() || keyEvent.isRight())) slidingMul = 1;
        return false;
    }

    @Override
    public boolean mouseClicked(MouseButtonEvent mouseButtonEvent, boolean bl) {
        dragging = active;
        return super.mouseClicked(mouseButtonEvent, bl);
    }

    @Override
    public void onRelease(MouseButtonEvent mouseButtonEvent) {
        dragging = false;
        if (applyChangesOnRelease) {
            onChange.accept(this);
            applyChangesOnRelease = false;
            updateMessage();
        }
        super.onRelease(mouseButtonEvent);
    }

    @Override
    protected void applyValue() {
        T oldValue = objectValue;
        setObjectValue(valueGetter.apply(this));
        if (!oldValue.equals(objectValue)) {
            LegacySoundUtil.playSimpleUISound(LegacyRegistries.SCROLL.get(), 1.0f);
            if (dragging)
                applyChangesOnRelease = true;
            else
                onChange.accept(this);
        }
    }

    public boolean updateValue() {
        if (objectValueSupplier != null) {
            T oldValue = objectValue;
            setObjectValue(objectValueSupplier.get());
            if (!oldValue.equals(objectValue)) {
                updateMessage();
                return true;
            }
        }
        return false;
    }
}
