package wily.legacy.client.screen;

import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.Renderable;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import wily.factoryapi.base.client.SimpleLayoutRenderable;
import wily.legacy.client.CommonColor;
import wily.legacy.client.LegacyOptions;

import java.util.ArrayList;
import java.util.List;

public class Legacy4JSettingsScreen extends AbstractSettingsScreen {

    public Legacy4JSettingsScreen(Screen screen) {
        super(screen, true);
    }

    @Override
    protected void addSections() {
        renderablesByTab.add(new ArrayList<>());
        OptionsScreen.Section.list.forEach(this::addOptionSection);
    }

    protected void addOptionSection(OptionsScreen.Section section) {
        tabList.add(100, 25, LegacyTabButton.Type.MIDDLE, section.title(), _ -> resetElements());
        section.elements().forEach(c -> c.accept(this));
        if (section == Section.GAME_OPTIONS)
            getRenderableVList().addRenderables(Button.builder(Component.translatable("controls.keybinds.title"), _ -> this.minecraft.setScreen(new LegacyKeyMappingScreen(this))).build(), Button.builder(Component.translatable("legacy.options.selectedController"), _ -> this.minecraft.setScreen(new ControllerMappingScreen(this))).build());
        section.advancedSection().ifPresent(s1 -> {
            getRenderableVList().addRenderable(SimpleLayoutRenderable.createDrawString(s1.title(), 0, 1, 200, 9, CommonColor.GRAY_TEXT.get(), false));
            if (s1 == Section.ADVANCED_USER_INTERFACE)
                getRenderableVList().addOptions(LegacyOptions.advancedOptionsMode);
            s1.elements().forEach(c -> c.accept(this));
        });
        List<Renderable> renderables = List.copyOf(getRenderableVList().renderables);
        getRenderableVList().renderables.clear();
        renderablesByTab.getFirst().addAll(renderables);
        renderablesByTab.add(renderables);
    }
}
