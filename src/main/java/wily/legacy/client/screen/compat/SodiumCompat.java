//? if fabric || >=1.21 && neoforge {
package wily.legacy.client.screen.compat;

//? if >=1.21 {
import net.caffeinemc.mods.sodium.client.SodiumClientMod;
import net.caffeinemc.mods.sodium.client.gui.SodiumGameOptionPages;
import net.caffeinemc.mods.sodium.client.gui.SodiumGameOptions;
import net.caffeinemc.mods.sodium.client.gui.options.Option;
import net.caffeinemc.mods.sodium.client.gui.options.OptionPage;
import net.caffeinemc.mods.sodium.client.gui.options.control.ControlValueFormatter;
import net.caffeinemc.mods.sodium.client.gui.options.control.CyclingControl;
import net.caffeinemc.mods.sodium.client.gui.options.control.SliderControl;
import net.caffeinemc.mods.sodium.client.gui.options.control.TickBoxControl;
import net.caffeinemc.mods.sodium.client.gui.options.storage.MinecraftOptionsStorage;
import net.caffeinemc.mods.sodium.client.gui.options.storage.SodiumOptionsStorage;
//?} else {
/*import me.jellysquid.mods.sodium.client.SodiumClientMod;
import me.jellysquid.mods.sodium.client.gui.SodiumGameOptionPages;
import me.jellysquid.mods.sodium.client.gui.SodiumGameOptions;
import me.jellysquid.mods.sodium.client.gui.options.Option;
import me.jellysquid.mods.sodium.client.gui.options.OptionPage;
import me.jellysquid.mods.sodium.client.gui.options.control.ControlValueFormatter;
import me.jellysquid.mods.sodium.client.gui.options.control.CyclingControl;
import me.jellysquid.mods.sodium.client.gui.options.control.SliderControl;
import me.jellysquid.mods.sodium.client.gui.options.control.TickBoxControl;
import me.jellysquid.mods.sodium.client.gui.options.storage.MinecraftOptionsStorage;
import me.jellysquid.mods.sodium.client.gui.options.storage.SodiumOptionsStorage;
*///?}
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.network.chat.Component;
import org.apache.logging.log4j.core.util.ReflectionUtil;
import wily.factoryapi.FactoryAPI;
import wily.factoryapi.FactoryAPIPlatform;
import wily.legacy.Legacy4JClient;
import wily.legacy.client.screen.*;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.*;

public class SodiumCompat {
    private static final boolean MODERN = hasClass("net.caffeinemc.mods.sodium.client.config.structure.Config");

    public static final OptionsScreen.Section SODIUM = OptionsScreen.Section.add(new OptionsScreen.Section(Component.literal(FactoryAPIPlatform.getModInfo("sodium").getName()), s-> Panel.centered(s, 250,200,0,18), new ArrayList<>(List.of(o-> {
        if (MODERN) Modern.addOptions(o);
        else Legacy.addOptions(o);
    }))));

    private static class Legacy {
        private static final Map<String,Field> SLIDER_CONTROL = FactoryAPI.getAccessibleFieldsMap(SliderControl.class,"min","max","mode");
        private static final Map<String,Field> CYCLING_CONTROL = FactoryAPI.getAccessibleFieldsMap(CyclingControl.class,"allowedValues","names");
        private static final Map<String,Field> GAME_OPTIONS_PAGE = FactoryAPI.getAccessibleFieldsMap(SodiumGameOptionPages.class,"sodiumOpts");
        private static final Map<String,Field> OPTIONS_STORAGE = FactoryAPI.getAccessibleFieldsMap(SodiumOptionsStorage.class,"options");

        private static AbstractWidget widget(Option<?> option){
            if (option.getStorage() instanceof MinecraftOptionsStorage) return null;

            Tooltip tooltip = option.getTooltip() == null ? null : Tooltip.create(option.getTooltip());

            if (option.getControl() instanceof TickBoxControl c) {
                return new TickBox(0,0,0,c.getOption().getValue(), b-> option.getName(), b-> tooltip, t-> {
                    c.getOption().setValue(t.selected);
                    option.applyChanges();
                    option.getStorage().save();
                });
            }else if (option.getControl() instanceof SliderControl c) {
                ControlValueFormatter formatter = (ControlValueFormatter) ReflectionUtil.getFieldValue(SLIDER_CONTROL.get("mode"),c);
                int min = (int) ReflectionUtil.getFieldValue(SLIDER_CONTROL.get("min"),c);
                int max = (int) ReflectionUtil.getFieldValue(SLIDER_CONTROL.get("max"),c);
                return LegacySliderButton.createFromIntRange(0, 0, 0, 16, (b) -> b.getDefaultMessage(option.getName(), formatter.format(b.getObjectValue())), b -> tooltip, c.getOption().getValue(), min, max, s -> {
                    if (!Objects.equals(c.getOption().getValue(), s.getObjectValue())) {
                        c.getOption().setValue(s.getObjectValue());
                        option.applyChanges();
                        option.getStorage().save();
                    }
                }, () -> c.getOption().getValue());
            }else if (option.getControl() instanceof CyclingControl<?> c) {
                List<Enum<?>> values = List.of((Enum<?>[])ReflectionUtil.getFieldValue(CYCLING_CONTROL.get("allowedValues"),c));
                Component[] components = (Component[]) ReflectionUtil.getFieldValue(CYCLING_CONTROL.get("names"),c);
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

        private static void addOptions(OptionsScreen screen){
            for (OptionPage page : List.of(SodiumGameOptionPages.general(), SodiumGameOptionPages.quality(), SodiumGameOptionPages.performance(), SodiumGameOptionPages.advanced())) {
                List<AbstractWidget> widgets = page.getOptions().stream().map(Legacy::widget).filter(Objects::nonNull).toList();
                if (!widgets.isEmpty()){
                    screen.getRenderableVList().addCategory(page.getName());
                    screen.getRenderableVList().renderables.addAll(widgets);
                }
            }
        }

        private static void resetOptions() {
            SodiumClientMod.restoreDefaultOptions();
            ReflectionUtil.setFieldValue(OPTIONS_STORAGE.get("options"),ReflectionUtil.getStaticFieldValue(GAME_OPTIONS_PAGE.get("sodiumOpts")), SodiumGameOptions.defaults());
        }
    }

    public static void init(){
        Legacy4JClient.whenResetOptions.add(()-> {
            if (MODERN) Modern.resetOptions();
            else Legacy.resetOptions();
        });
    }

    private static boolean hasClass(String name) {
        try {
            Class.forName(name, false, SodiumCompat.class.getClassLoader());
            return true;
        } catch (ClassNotFoundException ignored) {
            return false;
        }
    }

    private static class Modern {
        private static final String BUILDER = "net.caffeinemc.mods.sodium.client.gui.SodiumConfigBuilder";
        private static final Object CONFIG = field(null, "net.caffeinemc.mods.sodium.client.config.ConfigManager", "CONFIG");
        private static final Class<?> VANILLA_STORAGE = field(create(BUILDER), BUILDER, "vanillaStorage").getClass();

        private static void addOptions(OptionsScreen screen) {
            Object sodium = sodiumOptions();
            if (sodium == null) return;
            for (Object page : (Collection<?>) call(sodium, "pages")) {
                List<AbstractWidget> widgets = new ArrayList<>();
                for (Object group : (Collection<?>) call(page, "groups")) {
                    for (Object option : (Collection<?>) call(group, "options")) {
                        AbstractWidget widget = widget(option);
                        if (widget != null) widgets.add(widget);
                    }
                }
                if (!widgets.isEmpty()) {
                    screen.getRenderableVList().addCategory((Component) call(page, "name"));
                    screen.getRenderableVList().renderables.addAll(widgets);
                }
            }
        }

        private static AbstractWidget widget(Object option) {
            Object control = call(option, "getControl");
            Component name = (Component) call(option, "getName");
            if (control == null || !hasMethod(option, "getStorage") || isVanilla(option)) return null;
            Component tooltipText = (Component) call(option, "getTooltip");
            Tooltip tooltip = tooltipText == null ? null : Tooltip.create(tooltipText);
            return switch (control.getClass().getSimpleName()) {
                case "TickBoxControl" -> new TickBox(0, 0, 0, (Boolean) value(option), b -> name, b -> tooltip, b -> setValue(option, b.selected));
                case "SliderControl" -> {
                    Object range = call(option, hasMethod(option, "getSteppedValidator") ? "getSteppedValidator" : "getRange");
                    int min = (Integer) call(range, "min");
                    int max = (Integer) call(range, "max");
                    int step = (Integer) call(range, "step");
                    yield LegacySliderButton.createFromInt(0, 0, 0, 16, b -> b.getDefaultMessage(name, (Component) call(option, "formatValue", b.getObjectValue())), b -> tooltip, value(option), i -> min + i * step, v -> ((Integer) v - min) / step, () -> (max - min) / step + 1, b -> setValue(option, b.getObjectValue()), () -> value(option));
                }
                case "CyclingControl" -> cyclingWidget(option, name, tooltip);
                default -> null;
            };
        }

        private static AbstractWidget cyclingWidget(Object option, Component name, Tooltip tooltip) {
            List<Object> values = new ArrayList<>();
            List<Component> names = new ArrayList<>();
            for (Object value : (Collection<?>) call(call(option, "getAllowedValues"), "get", CONFIG)) {
                values.add(value);
                names.add((Component) call(option, "getElementName", value));
            }
            return values.isEmpty() ? null : new LegacySliderButton<Object>(0, 0, 0, 16, b -> b.getDefaultMessage(name, names.get(values.indexOf(b.getObjectValue()))), b -> tooltip, value(option), () -> values, b -> setValue(option, b.getObjectValue()));
        }

        private static void setValue(Object option, Object value) {
            if (Objects.equals(value(option), value)) return;
            call(option, "modifyValue", value);
            call(CONFIG, "applyAllOptions");
        }

        private static Object value(Object option) {
            return call(option, "getValidatedValue");
        }

        private static boolean isVanilla(Object option) {
            Object storage = call(option, "getStorage");
            return storage != null && storage.getClass() == VANILLA_STORAGE;
        }

        private static void resetOptions() {
            Object sodium = sodiumOptions();
            if (sodium == null) return;
            for (Object page : (Collection<?>) call(sodium, "pages"))
                for (Object group : (Collection<?>) call(page, "groups"))
                    for (Object option : (Collection<?>) call(group, "options"))
                        if (hasMethod(option, "getStorage") && !isVanilla(option)) call(option, "modifyValue", call(call(option, "getDefaultValue"), "get", CONFIG));
            call(CONFIG, "applyAllOptions");
        }

        private static Object sodiumOptions() {
            for (Object options : (Collection<?>) call(CONFIG, "getModOptions"))
                if ("sodium".equals(call(options, "configId"))) return options;
            return null;
        }

        private static Object call(Object target, String name, Object... arguments) {
            for (Method method : target.getClass().getMethods()) {
                if (!method.getName().equals(name) || method.getParameterCount() != arguments.length) continue;
                try {
                    return accessibleMethod(target, method).invoke(target, arguments);
                } catch (ReflectiveOperationException e) {
                    throw new IllegalStateException(e);
                }
            }
            throw new IllegalStateException("Missing method " + name);
        }

        private static boolean hasMethod(Object target, String name) {
            for (Method method : target.getClass().getMethods())
                if (method.getName().equals(name)) return true;
            return false;
        }

        private static Method accessibleMethod(Object target, Method method) {
            if (method.canAccess(target)) return method;
            for (Class<?> interfaceType : target.getClass().getInterfaces()) {
                try {
                    return interfaceType.getMethod(method.getName(), method.getParameterTypes());
                } catch (NoSuchMethodException ignored) {
                }
            }
            return method;
        }

        private static Class<?> type(String name) {
            try {
                return Class.forName(name, false, SodiumCompat.class.getClassLoader());
            } catch (ClassNotFoundException e) {
                throw new IllegalStateException(e);
            }
        }

        private static Object create(String owner) {
            try {
                return type(owner).getDeclaredConstructor().newInstance();
            } catch (ReflectiveOperationException e) {
                throw new IllegalStateException(e);
            }
        }

        private static Object field(Object target, String owner, String name) {
            try {
                Field field = type(owner).getDeclaredField(name);
                field.setAccessible(true);
                return field.get(target);
            } catch (ReflectiveOperationException e) {
                throw new IllegalStateException(e);
            }
        }
    }
}
//?}
