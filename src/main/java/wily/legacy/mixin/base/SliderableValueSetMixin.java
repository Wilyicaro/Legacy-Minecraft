package wily.legacy.mixin.base;

import net.minecraft.client.OptionInstance;
import net.minecraft.client.Options;
import net.minecraft.client.gui.components.AbstractWidget;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;
import wily.legacy.client.screen.LegacySliderButton;

import java.util.function.Consumer;
import java.util.function.Function;

@Mixin(OptionInstance.SliderableValueSet.class)
public interface SliderableValueSetMixin<T> extends OptionInstance.ValueSet<T> {
    @Shadow T fromSliderValue(double d);

    @Shadow double toSliderValue(T object);

    /**
     * @author Wilyicaro
     * @reason Legacy Edition Accuracy
     */
    @Overwrite
    default Function<OptionInstance<T>, AbstractWidget> createButton(OptionInstance.TooltipSupplier<T> tooltipSupplier, Options options, int i, int j, int k, Consumer<T> consumer) {
        return optionInstance -> new LegacySliderButton<T>(i,j,k,16,l-> optionInstance.toString.apply(optionInstance.get()),b->tooltipSupplier.apply(optionInstance.get()),optionInstance.get(),b->fromSliderValue(b.getValue()), this::toSliderValue, b->{
            optionInstance.set(b.getObjectValue());
            options.save();
            consumer.accept(b.getObjectValue());
        });
    }
}
