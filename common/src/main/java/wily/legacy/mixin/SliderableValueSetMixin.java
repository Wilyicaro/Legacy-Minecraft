package wily.legacy.mixin;

import net.minecraft.client.OptionInstance;
import net.minecraft.client.Options;
import net.minecraft.client.gui.components.AbstractWidget;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.function.Consumer;
import java.util.function.Function;

@Mixin(OptionInstance.SliderableValueSet.class)
public interface SliderableValueSetMixin<T> extends OptionInstance.ValueSet<T> {
    /**
     * @author Wilyicaro
     * @reason Legacy Edition Accuracy
     */
    @Overwrite
    default Function<OptionInstance<T>, AbstractWidget> createButton(OptionInstance.TooltipSupplier<T> tooltipSupplier, Options options, int i, int j, int k, Consumer<T> consumer) {
        return optionInstance -> new OptionInstance.OptionInstanceSliderButton(options, i, j, k, 16, optionInstance, (OptionInstance.SliderableValueSet) this, tooltipSupplier, consumer);
    }
}
