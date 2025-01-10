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
import wily.factoryapi.base.client.SimpleLayoutRenderable;
import wily.legacy.Legacy4J;
import wily.legacy.Legacy4JClient;
import wily.legacy.client.CommonColor;
import wily.legacy.client.screen.*;

import java.lang.reflect.Field;
import java.util.*;

public class SodiumCompat {

    public static final OptionsScreen.Section SODIUM = OptionsScreen.Section.add(new OptionsScreen.Section(Component.literal(FactoryAPIPlatform.getModInfo("sodium").getName()), s-> Panel.centered(s, 250,200,0,18), new ArrayList<>(List.of(o-> addSodiumOptionsFromPage(o, SodiumGameOptionPages.general(), SodiumGameOptionPages.quality(), SodiumGameOptionPages.performance(), SodiumGameOptionPages.advanced())))));

    public static final Map<String,Field> SODIUM_SLIDER_CONTROL_FIELDS = FactoryAPI.getAccessibleFieldsMap(SliderControl.class,"min","max","mode");
    public static final Map<String,Field> SODIUM_CYCLING_CONTROL_FIELDS = FactoryAPI.getAccessibleFieldsMap(CyclingControl.class,"allowedValues","names");
    public static final Map<String,Field> SODIUM_GAME_OPTIONS_PAGE_FIELDS = FactoryAPI.getAccessibleFieldsMap(SodiumGameOptionPages.class,"sodiumOpts");
    public static final Map<String,Field> SODIUM_OPTIONS_STORAGE_FIELDS = FactoryAPI.getAccessibleFieldsMap(SodiumOptionsStorage.class,"options");

    public static AbstractWidget getSodiumOptionWidget(Option<?> option){
        if (option.getStorage() instanceof MinecraftOptionsStorage) return null;

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

    public static void addSodiumOptionsFromPage(OptionsScreen screen, OptionPage... pages){
        for (OptionPage page : pages) {
            List<AbstractWidget> widgets = page.getOptions().stream().map(SodiumCompat::getSodiumOptionWidget).filter(Objects::nonNull).toList();
            if (!widgets.isEmpty()){
                screen.getRenderableVList().addRenderable(SimpleLayoutRenderable.createDrawString(page.getName(),0,1,200,9, CommonColor.INVENTORY_GRAY_TEXT.get(), false));
                screen.getRenderableVList().renderables.addAll(widgets);
            }
        }
    }

    public static void init(){
        Legacy4JClient.whenResetOptions.add(()-> {
            SodiumClientMod.restoreDefaultOptions();
            ReflectionUtil.setFieldValue(SODIUM_OPTIONS_STORAGE_FIELDS.get("options"),ReflectionUtil.getStaticFieldValue(SODIUM_GAME_OPTIONS_PAGE_FIELDS.get("sodiumOpts")), SodiumGameOptions.defaults());
        });
    }
}
//?}
