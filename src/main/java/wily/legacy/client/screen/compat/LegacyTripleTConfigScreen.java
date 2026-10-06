package wily.legacy.client.screen.compat;

import dev.jfronny.libjf.config.api.v2.ConfigCategory;
import dev.jfronny.libjf.config.api.v2.ConfigInstance;
import dev.jfronny.libjf.config.api.v2.EntryInfo;
import dev.jfronny.libjf.config.api.v2.Naming;
import dev.jfronny.libjf.config.api.v2.type.Type;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.components.Renderable;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import org.jspecify.annotations.Nullable;
import wily.legacy.Legacy4J;
import wily.legacy.client.screen.AbstractSettingsScreen;
import wily.legacy.client.screen.LegacySliderButton;
import wily.legacy.client.screen.LegacyTabButton;
import wily.legacy.client.screen.TickBox;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Objects;

/// @see wily.legacy.client.screen.Legacy4JSettingsScreen
/// @see wily.legacy.client.screen.AbstractSettingsScreen
public class LegacyTripleTConfigScreen extends AbstractSettingsScreen {
    interface ThrowableRunnable {
        void run() throws Throwable;
    }
    List<ThrowableRunnable> sync;

    protected final ConfigCategory config;
    protected final Naming naming;

    public LegacyTripleTConfigScreen(ConfigCategory config, Naming naming, @Nullable Screen parent) {
        this(config, naming, parent, shouldConsiderTabs(config, naming));
    }


    public LegacyTripleTConfigScreen(ConfigCategory config, Naming naming, @Nullable Screen parent, boolean considerTabs) {
        // Field initialization needs to happen **before** the superclass constructor is called, otherwise, `config`, `naming`, and `sync`, which are used by `LegacyTripleTConfigScreen#addSection` would be null when called.
        this.config = config;
        this.naming = naming;
        this.sync = new ArrayList<>();
        super(parent, considerTabs);
    }

    @Override
    protected void addSections() {
        if (useTabs) {
            for (ConfigCategory category : config.getCategories().values()) {
                int index = renderablesByTab.size();
                tabList.add(100, 25, LegacyTabButton.Type.MIDDLE, naming.category(category.getId()).name(), _ -> selectTab(index));
                try {
                    renderablesByTab.add(buildCategory(category, naming));
                } catch (IllegalAccessException e) {
                    throw new RuntimeException(e);
                }
            }
            renderablesByTab.addFirst(renderablesByTab.stream().flatMap(Collection::stream).toList());
        } else {
            try {
                renderablesByTab.add(buildCategory(config, naming));
            } catch (IllegalAccessException e) {
                throw new RuntimeException(e);
            }
        }
    }

    private static boolean shouldConsiderTabs(ConfigCategory config, Naming naming) {
        return config.getEntries().isEmpty() && config.getReferencedConfigs().isEmpty() && config.getCategories().size() > 1 && naming.description() == null;
    }

    private void selectTab(int index) {
        getRenderableVList().renderables.clear();
        getRenderableVList().renderables.addAll(renderablesByTab.get(index));

        getRenderableVList().scrolledList.set(0);
        resetElements();
        repositionElements();
    }

    // TODO fix warcrimes
    private Screen makePresetsScreen(Screen parent, ConfigCategory config, Naming naming, Runnable afterSelect) throws Throwable {
        MethodHandles.Lookup lookup = MethodHandles.lookup();
        Class<?> clazz = lookup.findClass("dev.jfronny.libjf.config.impl.ui.tiny.presets.PresetsScreen");
        MethodHandle constructor = lookup.findConstructor(clazz, MethodType.methodType(void.class, Screen.class, ConfigCategory.class, Naming.class, Runnable.class));
        constructor = constructor.asType(MethodType.methodType(Screen.class, Screen.class, ConfigCategory.class, Naming.class, Runnable.class));
        return (Screen) constructor.invokeExact(parent, config, naming, afterSelect);
    }

    private List<Renderable> buildCategory(ConfigCategory category, Naming naming) throws IllegalAccessException {
        ArrayList<Renderable> list = new ArrayList<>();
        for (EntryInfo<?> entry : category.getEntries()) {
            if (!entry.supportsRepresentation()) {
                continue;
            }
            Naming.Entry entryNaming = naming.entry(entry.getName());
            //noinspection unchecked, rawtypes
            AbstractWidget widget = LegacyTTTConfigWidgets.createWidget((EntryInfo) entry, entryNaming, 0, 0, entry.getWidth(), entry.getValue(),
                    value -> {
                        try {
                            //noinspection unchecked, rawtypes
                            ((EntryInfo) entry).setValue(value);
                        } catch (IllegalAccessException e) {
                            throw new RuntimeException(e);
                        }
                        if (config instanceof ConfigInstance i) {i.write();}
                    });
            if (widget != null) {
                // 'add(wily.legacy.client.screen.compat.LegacyTripleTConfigScreen.ThrowableRunnable)' in 'java.util.List' cannot be applied to '(wily.legacy.client.screen.compat.LegacyTripleTConfigScreen.ThrowableRunnable)'
                // which is why this isn't directly inlined into sync.add()
				//noinspection SwitchStatementWithTooFewBranches
				ThrowableRunnable thrRunnable = switch (widget) {
                    case EditBox box -> () -> box.setValue(Objects.toString(entry.getValue()));
                    default -> switch (entry.getValueType()) {
                        case Type.TBool _ -> () -> ((TickBox)widget).selected = (Boolean) entry.getValue();
                        case Type.TDouble _, Type.TEnum<?> _, Type.TFloat _, Type.TInt _, Type.TLong _  -> //noinspection unchecked, rawtypes
								() -> ((LegacySliderButton)(widget)).setObjectValue(entry.getValue());
                        case Type.TString _, Type.TUnknown _ -> () -> {};
                    };
                };
                sync.add(thrRunnable);
                list.add(widget);
            }
        }

        return list;
    }

    @Override
    protected void addActualRenderables() {
        if (!this.config.getPresets().isEmpty()) {
            renderableVList.renderables.add(Button.builder(Component.translatable("libjf-config-core-v2.presets"), _ -> {
                try {
                    this.minecraft.setScreen(makePresetsScreen(this, this.config, this.naming, () -> {
                        for (ThrowableRunnable throwableRunnable : sync) {
                            try {
                                throwableRunnable.run();
                            } catch (Throwable t) {
                                Legacy4J.LOGGER.error("Failed to synchronize UI element!", t);
                            }
                        }
                        if (config instanceof ConfigInstance i) i.write();
                        resetElements();
                    }));
                } catch (Throwable e) {
                    throw new RuntimeException(e);
                }
            }).build());
        }
        super.addActualRenderables();
    }
}
