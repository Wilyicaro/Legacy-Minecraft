//? if fabric || (>=1.21 && neoforge) {
package wily.legacy.client.screen.compat;

import net.caffeinemc.mods.sodium.api.config.option.ControlValueFormatter;
import net.caffeinemc.mods.sodium.client.SodiumClientMod;
import net.caffeinemc.mods.sodium.client.config.ConfigManager;
import net.caffeinemc.mods.sodium.client.config.structure.*;
import net.caffeinemc.mods.sodium.client.config.structure.Option;
import net.caffeinemc.mods.sodium.client.gui.options.control.CyclingControl;
import net.caffeinemc.mods.sodium.client.gui.options.control.SliderControl;
import net.caffeinemc.mods.sodium.client.gui.options.control.TickBoxControl;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.contents.TranslatableContents;
import net.minecraft.resources.Identifier;
import org.apache.commons.lang3.reflect.MethodUtils;
import org.apache.logging.log4j.core.util.ReflectionUtil;
import wily.factoryapi.FactoryAPI;
import wily.factoryapi.FactoryAPIPlatform;
import wily.factoryapi.base.client.SimpleLayoutRenderable;
import wily.legacy.Legacy4JClient;
import wily.legacy.client.CommonColor;
import wily.legacy.client.screen.*;

import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.util.*;

public class SodiumCompat {

    public static final Map<String, Field> SODIUM_OPTION_FIELDS = FactoryAPI.getAccessibleFieldsMap(Option.class, "id", "state");
    public static final OptionsScreen.Section SODIUM = OptionsScreen.Section.add(new OptionsScreen.Section(Component.literal(FactoryAPIPlatform.getModInfo("sodium").getName()), s -> Panel.centered(s, 250, 200, 0, 18), new ArrayList<>(List.of(SodiumCompat::addSodiumOptions))));
    public static AbstractWidget getGenericSodiumOptionWidget(Option option) {
        if (option instanceof StatefulOption<?> opt) return getSodiumOptionWidget(opt);
        return null;
    }

    public static <T> AbstractWidget getSodiumOptionWidget(StatefulOption<T> option) {
        if (option.getName().getContents() instanceof TranslatableContents contents && !contents.getKey().startsWith("sodium")) return null;
        
        Tooltip tooltip = option.getTooltip() == null ? null : Tooltip.create(option.getTooltip());

        if (option.getControl() instanceof TickBoxControl c) {
            return new TickBox(0, 0, 0, c.getOption().getAppliedValue(), b -> option.getName(), b -> tooltip, t -> {
                c.getOption().modifyValue(t.selected);
                applyChangesStatefulOption(option);
                option.getStorage().afterSave();
            });
        } else if (option.getControl() instanceof SliderControl c) {
            IntegerOption integerOption = (IntegerOption) c.getOption();
            return LegacySliderButton.createFromIntRange(0, 0, 0, 16, (b) -> b.getDefaultMessage(option.getName(), integerOption.getValueFormatter().format(b.getObjectValue())), b -> tooltip, c.getOption().getAppliedValue(), integerOption.getSteppedValidator().min(), integerOption.getSteppedValidator().max(), s -> {
                if (!Objects.equals(c.getOption().getAppliedValue(), s.getObjectValue())) {
                    c.getOption().modifyValue(s.getObjectValue());
                    applyChangesStatefulOption(option);
                    option.getStorage().afterSave();
                }
            }, () -> (Integer) option.getAppliedValue());
        } else if (option.getControl() instanceof CyclingControl<?>) {
            return createCyclingControlWidget((EnumOption<?>) option, tooltip);
        }
        return null;
    }


    public static <T extends Enum<T>> AbstractWidget createCyclingControlWidget(EnumOption<T> option, Tooltip tooltip) {
        Config parent = (Config) ReflectionUtil.getFieldValue(SODIUM_OPTION_FIELDS.get("state"), option);
        List<T> values = option.getAllowedValues().get(parent).stream().toList();
        return new LegacySliderButton<>(0, 0, 0, 16, (b) -> b.getDefaultMessage(option.getName(), option.getElementName(b.getObjectValue())), b -> tooltip, option.getAppliedValue(), () -> values, s -> {
            if (!Objects.equals(option.getAppliedValue(), s.getObjectValue())) {
                option.modifyValue(s.getObjectValue());
                applyChangesStatefulOption(option);
            }
        }, option::getAppliedValue);
    }

    public static <T> void applyChangesStatefulOption(StatefulOption<T> statefulOption) {
        //why package-private an id :(
        ConfigManager.CONFIG.applyOption((Identifier) ReflectionUtil.getFieldValue(SODIUM_OPTION_FIELDS.get("id"), statefulOption));
    }

    public static void addSodiumOptions(OptionsScreen screen) {
        for (ModOptions modOption : ConfigManager.CONFIG.getModOptions()) {
            for (Page page : modOption.pages()) {
                List<AbstractWidget> widgets = page.groups().stream().flatMap(optionGroup -> optionGroup.options().stream()).map(SodiumCompat::getGenericSodiumOptionWidget).filter(Objects::nonNull).toList();
                if (!widgets.isEmpty()) {
                    screen.getRenderableVList().addRenderable(SimpleLayoutRenderable.createDrawString(page.name(), 0, 1, 200, 9, CommonColor.GRAY_TEXT.get(), false));
                    screen.getRenderableVList().renderables.addAll(widgets);
                }
            }
        }
    }

    public static void init() {
        Legacy4JClient.whenResetOptions.add(() -> {
            for (ModOptions modOption : ConfigManager.CONFIG.getModOptions()) {
                for (Page page : modOption.pages()) {
                    for (OptionGroup group : page.groups()) {
                        for (Option option : group.options()) {
                            //Sodium doesn't have the 0.8.11 version available in the repo (with the reset to default method), so we're doing this for now
                            if (option instanceof StatefulOption<?> statefulOption)
                                resetSodiumOption(statefulOption);
                        }
                    }
                }
            }
            ConfigManager.CONFIG.applyAllOptions();
        });
    }

    public static <T> void resetSodiumOption(StatefulOption<T> statefulOption) {
        statefulOption.modifyValue(statefulOption.getDefaultValue().get((Config) ReflectionUtil.getFieldValue(SODIUM_OPTION_FIELDS.get("state"), statefulOption)));
    }
}
//?}
