package wily.legacy.client.screen;

import net.minecraft.client.gui.components.AbstractButton;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import wily.factoryapi.base.client.UIDefinition;
import wily.legacy.Legacy4JClient;


import java.util.List;
import java.util.function.Function;
import java.util.stream.Collectors;

public class SettingsScreen extends RenderableVListScreen {
    public static final List<Function<Screen,AbstractButton>> settingsButtons = OptionsScreen.Section.list.stream().map(section-> (Function<Screen,AbstractButton>) s->RenderableVListScreen.openScreenButton(section.title(),()->section.build(s)).build()).collect(Collectors.toList());

    protected SettingsScreen(Screen parent) {
        super(parent,Component.translatable("legacy.menu.settings"), r->{});
        settingsButtons.forEach(f->renderableVList.addRenderable(f.apply(this)));
        renderableVList.addRenderable(openScreenButton(Component.translatable("legacy.menu.reset_defaults"),()->new ConfirmationScreen(this,Component.translatable("legacy.menu.reset_settings"),Component.translatable("legacy.menu.reset_message"), b1->{
            Legacy4JClient.resetOptions(minecraft);
            minecraft.setScreen(this);
        })).build());
    }

}
