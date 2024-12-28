//? if fabric || >=1.21 && neoforge {
package wily.legacy.client.screen.compat;

//? if >=1.21 {
import net.caffeinemc.mods.sodium.client.SodiumClientMod;
import net.caffeinemc.mods.sodium.client.gui.SodiumGameOptionPages;
import net.caffeinemc.mods.sodium.client.gui.SodiumGameOptions;
import net.caffeinemc.mods.sodium.client.gui.options.Option;
import net.caffeinemc.mods.sodium.client.gui.options.control.ControlValueFormatter;
import net.caffeinemc.mods.sodium.client.gui.options.control.CyclingControl;
import net.caffeinemc.mods.sodium.client.gui.options.control.SliderControl;
import net.caffeinemc.mods.sodium.client.gui.options.control.TickBoxControl;
import net.caffeinemc.mods.sodium.client.gui.options.storage.SodiumOptionsStorage;
//?} else {
/*import me.jellysquid.mods.sodium.client.SodiumClientMod;
import me.jellysquid.mods.sodium.client.gui.SodiumGameOptionPages;
import me.jellysquid.mods.sodium.client.gui.SodiumGameOptions;
import me.jellysquid.mods.sodium.client.gui.options.Option;
import me.jellysquid.mods.sodium.client.gui.options.control.ControlValueFormatter;
import me.jellysquid.mods.sodium.client.gui.options.control.CyclingControl;
import me.jellysquid.mods.sodium.client.gui.options.control.SliderControl;
import me.jellysquid.mods.sodium.client.gui.options.control.TickBoxControl;
import me.jellysquid.mods.sodium.client.gui.options.storage.SodiumOptionsStorage;
*///?}
import net.minecraft.client.gui.components.AbstractButton;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import org.apache.logging.log4j.core.util.ReflectionUtil;
import wily.factoryapi.FactoryAPIPlatform;
import wily.legacy.Legacy4J;
import wily.legacy.Legacy4JClient;
import wily.legacy.Legacy4JPlatform;
import wily.legacy.client.screen.*;

import java.lang.reflect.Field;
import java.util.*;
import java.util.function.Function;

public class SodiumCompat {

    public static final List<Function<Screen, AbstractButton>> optionsButtons = new ArrayList<>();
    public static OptionsScreen.Section add(OptionsScreen.Section section){
        optionsButtons.add(s->RenderableVListScreen.openScreenButton(section.title(),()->section.build(s)).build());
        return section;
    }

    public static final OptionsScreen.Section GENERAL = add(new OptionsScreen.Section(SodiumGameOptionPages.general().getName(), s-> Panel.centered(s, 250,200,0,18), new ArrayList<>(List.of(o->SodiumGameOptionPages.general().getOptions().forEach(opt->addSodiumOptionWidgetIfPossible(o,opt))))));
    public static final OptionsScreen.Section QUALITY = add(new OptionsScreen.Section(SodiumGameOptionPages.quality().getName(), s-> Panel.centered(s, 250,200,0,18), new ArrayList<>(List.of(o->SodiumGameOptionPages.quality().getOptions().forEach(opt->addSodiumOptionWidgetIfPossible(o,opt))))));
    public static final OptionsScreen.Section PERFORMANCE = add(new OptionsScreen.Section(SodiumGameOptionPages.performance().getName(), s-> Panel.centered(s, 250,142), new ArrayList<>(List.of(o->SodiumGameOptionPages.performance().getOptions().forEach(opt->addSodiumOptionWidgetIfPossible(o,opt))))));
    public static final OptionsScreen.Section ADVANCED = add(new OptionsScreen.Section(SodiumGameOptionPages.advanced().getName(), s-> Panel.centered(s, 250,52), new ArrayList<>(List.of(o->SodiumGameOptionPages.advanced().getOptions().forEach(opt->addSodiumOptionWidgetIfPossible(o,opt))))));

    public static final OptionsScreen.Section SODIUM = OptionsScreen.Section.add(new OptionsScreen.Section(Component.literal(FactoryAPIPlatform.getModInfo("sodium").getName()), s-> Panel.centered(s, 220, optionsButtons.size()*24+16), new ArrayList<>(List.of(o-> optionsButtons.forEach(s-> o.getRenderableVList().addRenderable(s.apply(o)))))));

    public static final Map<String,Field> SODIUM_SLIDER_CONTROL_FIELDS = Legacy4J.getAccessibleFieldsMap(SliderControl.class,"min","max","mode");
    public static final Map<String,Field> SODIUM_CYCLING_CONTROL_FIELDS = Legacy4J.getAccessibleFieldsMap(CyclingControl.class,"allowedValues","names");
    public static final Map<String,Field> SODIUM_GAME_OPTIONS_PAGE_FIELDS = Legacy4J.getAccessibleFieldsMap(SodiumGameOptionPages.class,"sodiumOpts");
    public static final Map<String,Field> SODIUM_OPTIONS_STORAGE_FIELDS = Legacy4J.getAccessibleFieldsMap(SodiumOptionsStorage.class,"options");

    public static AbstractWidget getSodiumOptionWidget(Option<?> option){
        Tooltip tooltip = option.getTooltip() == null ? null : Tooltip.create(option.getTooltip());

        if (option.getControl() instanceof TickBoxControl c) {
            return new TickBox(0,0,0,c.getOption().getValue(), b-> option.getName(), b-> tooltip, t-> {
                c.getOption().setValue(t.selected);
                option.applyChanges();
                option.getStorage().save();
            });
        }else if (option.getControl() instanceof SliderControl c) {
            ControlValueFormatter formatter = (ControlValueFormatter) ReflectionUtil.getFieldValue(SODIUM_SLIDER_CONTROL_FIELDS.get("mode"),c);
            int min = (int) ReflectionUtil.getFieldValue(SODIUM_SLIDER_CONTROL_FIELDS.get("min"),c);
            int max = (int) ReflectionUtil.getFieldValue(SODIUM_SLIDER_CONTROL_FIELDS.get("max"),c);
            return LegacySliderButton.createFromIntRange(0, 0, 0, 16, (b) -> b.getDefaultMessage(option.getName(), formatter.format(b.getObjectValue())), b -> tooltip, c.getOption().getValue(), min, max, s -> {
                if (!Objects.equals(c.getOption().getValue(), s.getObjectValue())) {
                    c.getOption().setValue(s.getObjectValue());
                    option.applyChanges();
                    option.getStorage().save();
                }
            });
        }else if (option.getControl() instanceof CyclingControl<?> c) {
            List<Enum<?>> values = List.of((Enum<?>[])ReflectionUtil.getFieldValue(SODIUM_CYCLING_CONTROL_FIELDS.get("allowedValues"),c));
            Component[] components = (Component[]) ReflectionUtil.getFieldValue(SODIUM_CYCLING_CONTROL_FIELDS.get("names"),c);
            return new LegacySliderButton<>(0, 0, 0, 16, (b) -> b.getDefaultMessage(option.getName(), components[values.indexOf(b.getObjectValue())]), b -> tooltip, c.getOption().getValue(), ()->values, s -> {
                if (!Objects.equals(c.getOption().getValue(), s.getObjectValue())) {
                    ((Option<Enum<?>>)c.getOption()).setValue(s.getObjectValue());
                    option.applyChanges();
                    option.getStorage().save();
                }
            });
        }
        return null;
    }

    public static void addSodiumOptionWidgetIfPossible(OptionsScreen screen, Option<?> option){
        AbstractWidget widget = getSodiumOptionWidget(option);
        if (widget != null) screen.getRenderableVList().addRenderable(widget);
    }

    public static void init(){
        Legacy4JClient.whenResetOptions.add(()-> {
            SodiumClientMod.restoreDefaultOptions();
            ReflectionUtil.setFieldValue(SODIUM_OPTIONS_STORAGE_FIELDS.get("options"),ReflectionUtil.getStaticFieldValue(SODIUM_GAME_OPTIONS_PAGE_FIELDS.get("sodiumOpts")), SodiumGameOptions.defaults());
        });
    }
}
//?}
