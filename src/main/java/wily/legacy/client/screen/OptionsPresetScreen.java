package wily.legacy.client.screen;

import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.network.chat.Component;
import wily.factoryapi.base.config.FactoryConfig;
import wily.legacy.client.LegacyOptions;
import wily.legacy.client.OptionsPreset;
import wily.legacy.util.LegacyComponents;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class OptionsPresetScreen extends ConfirmationScreen {
    public final OptionsPreset preset;
    public final OptionsPreset originalPreset;

    protected final List<RenderableVList> renderableVLists = new ArrayList<>();
    protected final RenderableVList showOptionsList = new RenderableVList(accessor);

    public OptionsPresetScreen(Screen parent, OptionsPreset preset) {
        super(parent, ConfirmationScreen::getPanelWidth, () -> LegacyOptions.getUIMode().isSD() ? 130 : 175, Component.translatable("legacy.menu.options_preset", preset.nameOrEmpty()), LegacyComponents.OPTIONS_PRESET_MESSAGE, screen -> {
            screen.onClose();
            ((OptionsPresetScreen)screen).preset.applyAndSave();
        });
        renderableVLists.add(renderableVList);
        renderableVLists.add(showOptionsList);
        this.originalPreset = preset;
        this.preset = new OptionsPreset(preset.id(), preset.name(), new HashMap<>(preset.legacyOptions()), new HashMap<>(preset.vanillaOptions()));

        preset.legacyOptions().forEach((key, value) -> addSelectableOption(LegacyOptions.CLIENT_STORAGE.configMap.get(key), this.preset.legacyOptions(), value));
        preset.vanillaOptions().forEach((key, value) -> addSelectableOption(LegacyOptions.of(OptionsPreset.VANILLA_OPTIONS_MAP.get(key)), this.preset.vanillaOptions(), value));
    }

    public <T> void addSelectableOption(FactoryConfig<T> config, Map<String, Object> map, Object presetValue) {
        if (config.getDisplay() == null) return;
        showOptionsList.addRenderable(new TickBox(0, 0, true, b -> config.getDisplay().getMessage((T) presetValue), b -> null, b -> {
            if (b.selected) map.put(config.getKey(), presetValue);
            else map.remove(config.getKey());
        }));
    }

    @Override
    protected void init() {
        super.init();
        setInitialFocus(okButton);
    }

    @Override
    public boolean mouseScrolled(double d, double e, double f, double g) {
        if (super.mouseScrolled(d, e, f, g)) return true;
        showOptionsList.mouseScrolled(g);
        return false;
    }

    @Override
    public boolean keyPressed(KeyEvent keyEvent) {
        return showOptionsList.keyPressed(keyEvent.key()) || super.keyPressed(keyEvent);
    }

    @Override
    public void renderableVListInit() {
        boolean sd = LegacyOptions.getUIMode().isSD();
        super.renderableVListInit();
        showOptionsList.init(renderableVList.leftPos, panel.y + messageYOffset.get() + messageLabel.height + 8, renderableVList.listWidth, sd ? 56 : 72);
    }
}
