package wily.legacy.client.screen;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.ConfirmLinkScreen;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.WinScreen;
import net.minecraft.network.chat.Component;
import wily.factoryapi.FactoryAPIClient;
import wily.factoryapi.base.client.UIDefinition;
import wily.legacy.client.LegacyTip;

public class HelpAndOptionsScreen extends RenderableVListScreen {
    public HelpAndOptionsScreen(Screen parent) {
        super(parent,Component.translatable("options.title"), r-> {});
        renderableVList.addRenderable(Button.builder(Component.translatable("legacy.menu.change_skin"),(b)-> FactoryAPIClient.getToasts().addToast(new LegacyTip(Component.literal("Work in Progress!!"), 80, 40).disappearTime(960))).build());
        renderableVList.addRenderable(Button.builder(Component.translatable("legacy.menu.how_to_play"),(b)-> FactoryAPIClient.getToasts().addToast(new LegacyTip(Component.literal("Work in Progress!!"), 80, 40).disappearTime(960))).build());
        renderableVList.addRenderable(openScreenButton(Component.translatable("controls.title"),()-> new RenderableVListScreen(this,Component.translatable("controls.title"),r->r.addRenderables(Button.builder(Component.translatable("options.mouse_settings.title"), button -> this.minecraft.setScreen(new OptionsScreen(r.getScreen(), s->Panel.centered(s, 250,102),Component.translatable("options.mouse_settings.title"), minecraft.options.invertYMouse(), Minecraft.getInstance().options.sensitivity(), minecraft.options.mouseWheelSensitivity(), minecraft.options.discreteMouseScroll(), minecraft.options.touchscreen()))).build(),Button.builder(Component.translatable("controls.keybinds.title"), button -> this.minecraft.setScreen(new LegacyKeyBindsScreen(r.getScreen(),minecraft.options))).build(),Button.builder(Component.translatable("legacy.options.selectedController"), button -> this.minecraft.setScreen(new ControllerMappingScreen(r.getScreen(),minecraft.options))).build()))).build());
        renderableVList.addRenderable(openScreenButton(Component.translatable("legacy.menu.settings"),()->new SettingsScreen(this)).build());
        renderableVList.addRenderable(openScreenButton(Component.translatable("credits_and_attribution.button.credits"),()->new RenderableVListScreen(this,Component.translatable("credits_and_attribution.screen.title"),r-> r.addRenderables(openScreenButton(Component.translatable("credits_and_attribution.button.credits"),()->new WinScreen(false, () -> this.minecraft.setScreen(r.getScreen()))).build(),Button.builder(Component.translatable("credits_and_attribution.button.attribution"), ConfirmLinkScreen.confirmLink(/*? if >1.20.2 {*/this,/*?}*/"https://aka.ms/MinecraftJavaAttribution"/*? if <=1.20.2 {*//*, this, true*//*?}*/)).build(),Button.builder(Component.translatable("credits_and_attribution.button.licenses"), ConfirmLinkScreen.confirmLink(/*? if >1.20.2 {*/this,/*?}*/"https://aka.ms/MinecraftJavaLicenses"/*? if <=1.20.2 {*//*, this, true*//*?}*/)).build()))).build());
    }
}
