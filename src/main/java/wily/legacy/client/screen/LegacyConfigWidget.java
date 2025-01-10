package wily.legacy.client.screen;


import net.minecraft.Util;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.network.chat.Component;
import wily.legacy.config.LegacyConfig;

import java.util.function.*;

public interface LegacyConfigWidget<T> {
    Function<Component,Tooltip> TOOLTIP_CACHE = Util.memoize(c->Tooltip.create(c));

    static Tooltip getCachedTooltip(Component component){
        return component == null ? null : TOOLTIP_CACHE.apply(component);
    }

    static <T> AbstractWidget createWidget(LegacyConfig<T> config, int x, int y, int width, Consumer<T> afterSet) {
        LegacyConfigWidget<T> configWidget = (LegacyConfigWidget<T>) LegacyConfig.displayMap.get(config).get();
        return configWidget.createWidget(x,y,width,afterSet).apply(config);
    }

    static <T> AbstractWidget createWidget(LegacyConfig<T> config) {
        return createWidget(config, 0, 0, 0, v-> {});
    }

    Function<LegacyConfig<T>, AbstractWidget> createWidget(int x, int y, int width, Consumer<T> afterSet);


    static LegacyConfigWidget<Boolean> createTickBox(Function<Boolean, Tooltip> tooltipFunction){
        return (x, y, width, afterSet)-> config-> new TickBox(x,y,width,config.secureCast(Boolean.class).get(), b-> config.getDisplay().get().name(), tooltipFunction, t-> LegacyConfig.saveOptionAndConsume(config,t.selected,afterSet));
    }

    static <T> LegacyConfigWidget<T> createSliderFromInt(Function<T,Tooltip> tooltipFunction, BiFunction<Component,T,Component> captionFunction, Function<Integer,T> valueGetter, Function<T, Integer> valueSetter, Supplier<Integer> valuesSize){
        return (x, y, width, afterSet)-> config->  LegacySliderButton.createFromInt(x,y,width,16, s-> captionFunction.apply(config.getDisplay().get().name(), s.getObjectValue()), s-> tooltipFunction.apply(s.getObjectValue()), config.get(), valueGetter, valueSetter, valuesSize, s-> LegacyConfig.saveOptionAndConsume(config,s.getObjectValue(),afterSet));
    }

    static <T> LegacyConfigWidget<T> createSlider(Function<T,Tooltip> tooltipFunction, BiFunction<Component,T,Component> captionFunction, Function<LegacySliderButton<T>,T> valueGetter, Function<T, Double> valueSetter){
        return (x, y, width, afterSet)-> config-> new LegacySliderButton<>(x, y, width,16, s-> captionFunction.apply(config.getDisplay().get().name(),s.getObjectValue()),b->tooltipFunction.apply(b.getObjectValue()),config.get(), valueGetter, valueSetter, s-> LegacyConfig.saveOptionAndConsume(config,s.getObjectValue(),afterSet));
    }

    static LegacyConfigWidget<Integer> createIntegerSlider(Function<Integer,Tooltip> tooltipFunction, BiFunction<Component,Integer,Component> captionFunction, int min, IntSupplier max){
        return (x, y, width, afterSet)-> config-> LegacySliderButton.createFromIntRange(x, y, width,16, s-> captionFunction.apply(config.getDisplay().get().name(),s.getObjectValue()),b->tooltipFunction.apply(b.getObjectValue()),config.get(), min, max, s-> LegacyConfig.saveOptionAndConsume(config,s.getObjectValue(),afterSet));
    }

    static LegacyConfigWidget<Boolean> createCommonTickBox(Function<Boolean, Component> tooltipFunction){
        return createTickBox(b->getCachedTooltip(tooltipFunction.apply(b)));
    }

    static <T> LegacyConfigWidget<T> createCommonSliderFromInt(Function<T,Component> tooltipFunction, BiFunction<Component,T,Component> captionFunction, Function<Integer,T> valueGetter, Function<T, Integer> valueSetter, Supplier<Integer> valuesSize){
        return createSliderFromInt(v->getCachedTooltip(tooltipFunction.apply(v)),captionFunction,valueGetter,valueSetter,valuesSize);
    }

    static LegacyConfigWidget<Double> createCommonSlider(Function<Double,Component> tooltipFunction, BiFunction<Component,Double,Component> captionFunction){
        return createSlider(v->getCachedTooltip(tooltipFunction.apply(v)),captionFunction,LegacySliderButton::getValue, v-> v);
    }

    static LegacyConfigWidget<Integer> createCommonIntegerSlider(Function<Integer,Component> tooltipFunction, BiFunction<Component,Integer,Component> captionFunction, int min, IntSupplier max){
        return createIntegerSlider(v->getCachedTooltip(tooltipFunction.apply(v)),captionFunction,min,max);
    }

}