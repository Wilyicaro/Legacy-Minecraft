package wily.legacy.client.screen;

import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.ConfirmLinkScreen;
import net.minecraft.client.gui.screens.MouseSettingsScreen;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.WinScreen;
import net.minecraft.client.gui.screens.controls.ControlsScreen;
import net.minecraft.client.gui.screens.controls.KeyBindsScreen;
import net.minecraft.network.chat.Component;

public class HelpOptionsScreen extends RenderableVListScreen {
    public HelpOptionsScreen(Screen parent) {
        super(parent,Component.translatable("options.title"), r-> {});
        renderableVList.addRenderable(Button.builder(Component.translatable("legacy.menu.how_to_play"),(b)->{}).build());
        renderableVList.addRenderable(openScreenButton(Component.translatable("controls.title"),()-> new RenderableVListScreen(this,Component.translatable("controls.title"),r->r.addRenderables(Button.builder(Component.translatable("options.mouse_settings"), button -> this.minecraft.setScreen(new MouseSettingsScreen(this, minecraft.options))).build(),Button.builder(Component.translatable("controls.keybinds"), button -> this.minecraft.setScreen(new KeyBindsScreen(this, minecraft.options))).build()))).build());
        renderableVList.addRenderable(openScreenButton(Component.translatable("legacy.menu.settings"),()->new SettingsScreen(this)).build());
        renderableVList.addRenderable(openScreenButton(Component.translatable("credits_and_attribution.button.credits"),()->new RenderableVListScreen(this,Component.translatable("credits_and_attribution.screen.title"),r-> r.addRenderables(openScreenButton(Component.translatable("credits_and_attribution.button.credits"),()->new WinScreen(false, () -> this.minecraft.setScreen(this))).build(),Button.builder(Component.translatable("credits_and_attribution.button.attribution"), ConfirmLinkScreen.confirmLink(this,"https://aka.ms/MinecraftJavaAttribution")).build(),Button.builder(Component.translatable("credits_and_attribution.button.licenses"), ConfirmLinkScreen.confirmLink(this,"https://aka.ms/MinecraftJavaLicenses")).build()))).build());
        this.parent = parent;
    }
}
