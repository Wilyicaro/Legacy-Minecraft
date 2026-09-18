package wily.legacy.client.screen.compat;

import dev.jfronny.libjf.config.api.v2.ConfigCategory;
import dev.jfronny.libjf.config.api.v2.ConfigInstance;
import dev.jfronny.libjf.config.api.v2.EntryInfo;
import dev.jfronny.libjf.config.api.v2.Naming;
import dev.jfronny.libjf.config.api.v2.type.Type;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.components.Renderable;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.world.phys.Vec2;
import org.jspecify.annotations.Nullable;
import wily.legacy.client.screen.LegacySliderButton;
import wily.legacy.client.screen.LegacyTabButton;
import wily.legacy.client.screen.OptionsScreen;
import wily.legacy.client.screen.Panel;
import wily.legacy.client.screen.TabList;
import wily.legacy.client.screen.TickBox;
import wily.legacy.util.LegacyComponents;
import wily.legacy.util.client.LegacyRenderUtil;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Locale;
import java.util.Objects;

/// @see wily.legacy.client.screen.Legacy4JSettingsScreen
public class LegacyTripleTConfigScreen extends OptionsScreen implements TabList.Access {
    protected final TabList tabList = new TabList(accessor);
    protected final List<List<Renderable>> renderablesByTab = new ArrayList<>();
    protected final EditBox editBox = new EditBox(Minecraft.getInstance().font, 0, 0, 200, 20, Component.translatable("legacy.menu.filter.search"));

    interface ThrowableRunnable {
        void run() throws Throwable;
    }
    List<ThrowableRunnable> sync = new ArrayList<>();

    protected final ConfigCategory config;
    protected final Naming naming;
    protected final boolean useTabs;

    public LegacyTripleTConfigScreen(ConfigCategory config, Naming naming, @Nullable Screen parent) {
        this(config, naming, parent, shouldConsiderTabs(config, naming));
    }


    public LegacyTripleTConfigScreen(ConfigCategory config, Naming naming, @Nullable Screen parent, boolean considerTabs) {
        super(parent, s -> Panel.createPanel(s, p -> p.appearance(250, Math.min(250, s.height - 52)), p -> p.pos(p.centeredLeftPos(s) + 50, p.centeredTopPos(s))), CommonComponents.EMPTY);
        this.config = config;
        this.naming = naming;
        this.useTabs = considerTabs;
        if (useTabs) {
            tabList.add(100, 25, LegacyTabButton.Type.MIDDLE, LegacyComponents.ALL, _ -> resetElements());

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

        addActualRenderables();
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
            AbstractWidget widget = LegacyTTTConfigWidgets.createWidget((EntryInfo) entry, entryNaming, 0, 0, entry.getWidth(), entry.getValue(),
                    value -> {
                        try {
                            ((EntryInfo) entry).setValue(value);
                        } catch (IllegalAccessException e) {
                            throw new RuntimeException(e);
                        }
                        if (config instanceof ConfigInstance i) {i.write();}
                    });
            if (widget != null) {
                // 'add(wily.legacy.client.screen.compat.LegacyTripleTConfigScreen.ThrowableRunnable)' in 'java.util.List' cannot be applied to '(wily.legacy.client.screen.compat.LegacyTripleTConfigScreen.ThrowableRunnable)'
                sync.add(switch (widget) {
                    case EditBox box -> () -> box.setValue(Objects.toString(entry.getValue()));
					default -> switch (entry.getValueType()) {
                        case Type.TBool _ -> () -> ((TickBox)widget).selected = (Boolean) entry.getValue();
                        case Type.TDouble _, Type.TEnum<?> _, Type.TFloat _, Type.TInt _, Type.TLong _  -> () -> ((LegacySliderButton)(widget)).setObjectValue(entry.getValue());
                        case Type.TString _ -> () -> ((EditBox)widget).setValue((String) entry.getValue());
                        case Type.TUnknown _ -> () -> {};
                    };
                });
                list.add(widget);
            }
        }

        return list;
    }

    protected void resetElements() {
        getRenderableVList().renderables.clear();
        addActualRenderables();
        getRenderableVList().scrolledList.set(0);
        repositionElements();
    }

    protected void addActualRenderables() {
        if (!this.config.getPresets().isEmpty()) {
            renderableVList.renderables.add(Button.builder(Component.translatable("libjf-config-core-v2.presets"), (button) -> {
                try {
                    this.minecraft.setScreen(makePresetsScreen(this, this.config, this.naming, () -> {
                        for (ThrowableRunnable throwableRunnable : sync) {
                            try {
                                throwableRunnable.run();
                            } catch (Throwable t) {t.printStackTrace();}
                        }
                        if (config instanceof ConfigInstance i) {i.write();}
                        resetElements();
                    }));
                } catch (Throwable e) {
                    throw new RuntimeException(e);
                }
            }).build());
        }
        String value = editBox.getValue().toLowerCase(Locale.ROOT);

        if (value.isBlank()) {
            getRenderableVList().renderables.addAll(renderablesByTab.get(useTabs ? getTabList().getIndex() : 0));
        } else {
            for (Renderable renderable : renderablesByTab.get(useTabs ? getTabList().getIndex() : 0)) {
                if (renderable instanceof AbstractWidget w && w.getMessage().getString().toLowerCase(Locale.ROOT).contains(value)) {
                    getRenderableVList().renderables.add(w);
                }
            }
        }
    }

    @Override
    public void renderDefaultBackground(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float delta) {
        LegacyRenderUtil.renderDefaultBackground(accessor, graphics, false);
    }

    @Override
    protected void init() {
        if (useTabs) addRenderableWidget(tabList);
        super.init();
        if (useTabs) addRenderableOnly(tabList::renderSelected);
        addRenderableWidget(editBox);
        editBox.setWidth(panel.getWidth() - 50);
        editBox.setPosition(panel.getX() + (panel.width - editBox.getWidth()) / 2, panel.getY() + 10);
        editBox.setResponder(_ -> resetElements());
        if (useTabs) {
            tabList.init((b, i) -> {
                b.spriteRender = accessor.getElementValue("tabList.sprites", LegacyTabButton.ToggleableTabSprites.VERTICAL, LegacyTabButton.Render.class);
                b.setX(panel.x - b.getWidth() + 6);
                b.setY(panel.y + i + 5);
                b.offset = (t1) -> new Vec2(t1.selected ? 0 : 3.4f, 0.4f);
            }, true);
        }
    }

    @Override
    public void renderableVListInit() {
        getRenderableVList().init(panel.x + 10, panel.y + 40, panel.width - 20, panel.height - 50);
    }

    @Override
    public boolean keyPressed(KeyEvent keyEvent) {
        if (useTabs && tabList.controlTab(keyEvent)) return true;
        return super.keyPressed(keyEvent);
    }

    @Override
    public TabList getTabList() {
        return tabList;
    }
}
