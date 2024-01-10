package wily.legacy.client.screen;

import net.minecraft.client.gui.components.AbstractSliderButton;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.navigation.CommonInputs;
import net.minecraft.network.chat.Component;

import java.util.List;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;

public class LegacySliderButton<T> extends AbstractSliderButton {
    private final Function<LegacySliderButton<T>,Component> messageGetter;
    private final Supplier<List<T>> values;
    private final Consumer<LegacySliderButton<T>> onChange;
    private final Supplier<Tooltip> tooltipSupplier;
    public T objectValue;
    public LegacySliderButton(int i, int j, int k, int l, Function<LegacySliderButton<T>,Component> messageGetter, Supplier<Tooltip> tooltipSupplier, double initialValue, Supplier<List<T>> values, Consumer<LegacySliderButton<T>>  onChange) {
        super(i, j, k, l, Component.empty(), initialValue);
        this.messageGetter = messageGetter;
        this.values = values;
        this.onChange = onChange;
        this.tooltipSupplier = tooltipSupplier;
        updateMessage();
    }

    public LegacySliderButton(int i, int j, int k, int l, Function<LegacySliderButton<T>,Component> messageGetter, Supplier<Tooltip> tooltipSupplier, T initialValue, Supplier<List<T>> values, Consumer<LegacySliderButton<T>>  onChange) {
        this(i, j, k, l, messageGetter, tooltipSupplier, (double) Math.max(0,values.get().indexOf(initialValue))/ (values.get().size() - 1),values,onChange);
        objectValue = initialValue;
    }
    public Component getDefaultMessage(Component caption, Component visibleValue){
        return caption.copy().append(": ").append(visibleValue);
    }
    public T getValue(){
        return objectValue == null ? values.get().get((int) Math.round(value * (values.get().size() - 1))) : objectValue;
    }

    @Override
    protected void updateMessage() {
        setMessage(messageGetter.apply(this));
        setTooltip(tooltipSupplier.get());
    }
    @Override
    public boolean keyPressed(int i, int j, int k) {
        if (CommonInputs.selected(i)) {
            this.canChangeValue = !this.canChangeValue;
            return true;
        }
        if (this.canChangeValue) {
            boolean bl = i == 263;
            if (bl || i == 262) {
                double part = 1D / values.get().size();
                setValue(this.value + (bl ? -part : part));
                return true;
            }
        }
        return false;
    }
    @Override
    protected void applyValue() {
        int index = (int) Math.round(value * (values.get().size() - 1));
        value = (double) index / (values.get().size() - 1);
        objectValue = values.get().get(index);
        onChange.accept(this);
    }

}
