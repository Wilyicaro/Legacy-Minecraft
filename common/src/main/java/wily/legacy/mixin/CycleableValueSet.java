package wily.legacy.mixin;

import net.minecraft.client.Minecraft;
import net.minecraft.client.OptionInstance;
import net.minecraft.client.Options;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractSliderButton;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.CycleButton;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import wily.legacy.LegacyMinecraftClient;
import wily.legacy.client.screen.LegacySliderButton;
import wily.legacy.client.screen.TickBox;

import java.awt.*;
import java.util.List;
import java.util.Map;
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
            if (values.size() <= 2){
                return new TickBox(i,j,k,values.indexOf(optionInstance.value) == 0,b-> LegacyMinecraftClient.OPTION_BOOLEAN_CAPTION.getOrDefault(optionInstance.caption,optionInstance.caption), b-> tooltipSupplier.apply(values.get(b ? 0 : 1)), t-> {
                    this.valueSetter().set(optionInstance,values.get(t.selected ? 0 : 1));
                    options.save();
                });
            }else {
                return new LegacySliderButton<>(i,j,k,16, (b)-> b.getDefaultMessage(optionInstance.caption,optionInstance.toString.apply(optionInstance.value)),()->tooltipSupplier.apply(optionInstance.value),optionInstance.value,()->valueListSupplier().getSelectedList(),s->{
                    if (optionInstance.value != s.objectValue) {
                        valueSetter().set(optionInstance, s.objectValue);
                        options.save();
                        consumer.accept(s.objectValue);
                    }
                });
            }
        };
    }

}
