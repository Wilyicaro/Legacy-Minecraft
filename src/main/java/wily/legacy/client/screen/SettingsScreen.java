package wily.legacy.client.screen;

import net.minecraft.client.gui.components.AbstractButton;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import wily.factoryapi.FactoryAPI;
import wily.legacy.Legacy4JClient;
import wily.legacy.client.LegacyOptions;
//? if fabric || (>=1.21 && neoforge) {
import wily.legacy.client.screen.compat.SodiumCompat;
//?}


import java.util.List;
import java.util.function.Function;
import java.util.stream.Collectors;

public class SettingsScreen extends RenderableVListScreen {
    public static final List<Function<Screen, AbstractButton>> SETTINGS_BUTTONS = OptionsScreen.Section.list.stream().map(section -> (Function<Screen, AbstractButton>) s -> section.createButtonBuilder(s).build()).collect(Collectors.toList());

    protected SettingsScreen(Screen parent) {
        super(parent, Component.translatable("legacy.menu.settings"), r -> {
        });
        for (OptionsScreen.Section section : OptionsScreen.Section.list) {
            if (shouldHideSection(section)) continue;
            renderableVList.addRenderable(section.createButtonBuilder(this).build());
        }
        renderableVList.addRenderable(openScreenButton(Component.translatable("legacy.menu.reset_defaults"), () -> new ConfirmationScreen(this, Component.translatable("legacy.menu.reset_settings"), Component.translatable("legacy.menu.reset_message"), b1 -> {
            Legacy4JClient.resetOptions(minecraft);
            b1.onClose();
        })).build());
    }

    private static boolean shouldHideSection(OptionsScreen.Section section) {
        if (!LegacyOptions.legacySettingsMenus.get() && !LegacyOptions.hideSodiumSettings.get()) return false;
        //? if fabric || (>=1.21 && neoforge) {
        return FactoryAPI.isModLoaded("sodium") && section == SodiumCompat.SODIUM;
        //?} else {
        /*return false;
        *///?}
    }

}
