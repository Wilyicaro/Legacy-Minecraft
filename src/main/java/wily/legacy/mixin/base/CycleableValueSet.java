package wily.legacy.mixin.base;

import net.minecraft.client.OptionInstance;
import net.minecraft.client.Options;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.CycleButton;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;
import wily.legacy.Legacy4JClient;
import wily.legacy.client.screen.LegacySliderButton;
import wily.legacy.client.screen.TickBox;

import java.util.List;
import java.util.function.Consumer;
import java.util.function.Function;

@Mixin(OptionInstance.CycleableValueSet.class)
public interface CycleableValueSet<T> extends OptionInstance.ValueSet<T> {
    @Shadow CycleButton.ValueListSupplier<T> valueListSupplier();

    @Shadow OptionInstance.CycleableValueSet.ValueSetter<T> valueSetter();
    /**
     * @author Wilyicaro
     * @reason Legacy Edition Accuracy
     */
    @Overwrite
    default Function<OptionInstance<T>, AbstractWidget> createButton(OptionInstance.TooltipSupplier<T> tooltipSupplier, Options options, int i, int j, int k, Consumer<T> consumer){
        return optionInstance -> {
            List<T> values = valueListSupplier().getSelectedList();
            if (optionInstance.get() instanceof Boolean){
                return new TickBox(i,j,k,values.indexOf(optionInstance.get()) == 0,b-> Legacy4JClient.booleanOptionCaptionOverride.getOrDefault(optionInstance.caption,optionInstance.caption), b-> tooltipSupplier.apply(values.get(b ? 0 : 1)), t-> {
                    this.valueSetter().set(optionInstance,values.get(t.selected ? 0 : 1));
                    options.save();
                    consumer.accept(optionInstance.get());
                });
            } else {
                return new LegacySliderButton<>(i,j,k,16, (b)-> b.getDefaultMessage(optionInstance.caption,optionInstance.toString.apply(optionInstance.get())),b->tooltipSupplier.apply(optionInstance.get()),optionInstance.get(),()->valueListSupplier().getSelectedList(),s->{
                    if (optionInstance.get() != s.getObjectValue()) {
                        valueSetter().set(optionInstance, s.getObjectValue());
                        options.save();
                        consumer.accept(s.getObjectValue());
                    }
                });
            }
        };
    }

}
