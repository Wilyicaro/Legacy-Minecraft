package wily.legacy.client.screen;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.WinScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.PlayerModelPart;
import wily.factoryapi.base.ArbitrarySupplier;
import wily.factoryapi.base.config.FactoryConfig;
import wily.legacy.client.ContentReinstaller;
import wily.legacy.client.LegacyOptions;
import wily.legacy.util.LegacySprites;

import java.util.ArrayList;
import java.util.List;

public class HelpAndOptionsScreen extends RenderableVListScreen {
    //public static final List<Function<Screen, AbstractButton>> HOW_TO_PLAY_BUTTONS = new ArrayList<>(List.of(s->RenderableVListScreen.openScreenButton(Component.literal("Minecraft Wiki"),()->ConfirmationScreen.createLinkScreen(s,"https://minecraft.wiki/")).build(), s->RenderableVListScreen.openScreenButton(Component.literal("Legacy4J Wiki"),()->ConfirmationScreen.createLinkScreen(s,"https://github.com/Wilyicaro/Legacy-Minecraft/wiki")).build(),s->RenderableVListScreen.openScreenButton(Component.translatable("legacy.options.hints"),()->new ItemViewerScreen(s,s1->Panel.centered(s1,325,180), CommonComponents.EMPTY)).build()));
    private static final Component LEGACY_SKIN_OPTIONS = Component.literal("Legacy4J Skin Options");
    private static final Component VANILLA_SKIN_OPTIONS = Component.literal("Vanilla Skin Options");
    private static final Component REINSTALL_CONTENT = Component.translatable("legacy.menu.reinstall_content");
    private static final Component REINSTALLING_CONTENT = Component.translatable("legacy.menu.reinstall_content.running");

    public static List<AbstractWidget> createPlayerSkinWidgets(){
        List<AbstractWidget> list = new ArrayList<>();
        for (PlayerModelPart p : PlayerModelPart.values()) {
            list.add(new TickBox(0,0,Minecraft.getInstance().options.isModelPartEnabled(p),b->p.getName(),b->null,t->{
                Minecraft.getInstance().options./*? if <1.21.2 {*/toggleModelPart/*?} else {*//*setModelPart*//*?}*/(p,t.selected);
                Minecraft.getInstance().options.save();
            }));
        }
        list.add(LegacyConfigWidgets.createWidget(LegacyOptions.of(Minecraft.getInstance().options.mainHand())));
        return list;
    }

    public static final OptionsScreen.Section CHANGE_SKIN_OPTIONS = new OptionsScreen.Section(Component.translatable("legacy.menu.change_skin"), s->Panel.centered(s, 250,150), new ArrayList<>(List.of(HelpAndOptionsScreen::addPlayerSkinOptions)));
    public static ScreenSection<?> CHANGE_SKIN = CHANGE_SKIN_OPTIONS;
    public static final OptionsScreen.Section HOW_TO_PLAY = new OptionsScreen.Section(Component.translatable("legacy.menu.how_to_play"), s->Panel.centered(s, LegacySprites.PANEL, 240, Math.min(7, (int)HowToPlayScreen.Section.getWithButton().count())*25+24, 0, 20), new ArrayList<>(List.of(o-> HowToPlayScreen.Section.getWithButton().forEach(s-> o.getRenderableVList().addRenderable(s.createButtonBuilder(o).build())))), ArbitrarySupplier.empty(), ((screen, section) -> new OptionsScreen(screen, section){
        @Override
        public void renderableVListInit() {
            getRenderableVList().cyclic(false).layoutSpacing(l->5).init(panel.x + 8,panel.getY() + 8,panel.getWidth() - 16,panel.getHeight()-16);
        }
    }));

    public HelpAndOptionsScreen(Screen parent) {
        super(parent,Component.translatable("options.title"), r-> {});
        renderableVList.addRenderable(CHANGE_SKIN.createButtonBuilder(this).build());
        renderableVList.addRenderable(HOW_TO_PLAY.createButtonBuilder(this).build());
        renderableVList.addRenderable(openScreenButton(Component.translatable("controls.title"),()-> new RenderableVListScreen(this,Component.translatable("controls.title"),r->r.addRenderables(Button.builder(Component.translatable("options.mouse_settings.title"), button -> this.minecraft.setScreen(
                new OptionsScreen(r.getScreen(), new OptionsScreen.Section(
                        Component.translatable("options.mouse_settings.title"),
                        s-> Panel.centered(s, 250, 150, 0, 10),
                        new ArrayList<>(List.of(
                                o -> o.renderableVList.addMultSliderOption(LegacyOptions.of(Minecraft.getInstance().options.sensitivity()), 2),
                                o -> o.renderableVList.addMultSliderOption(LegacyOptions.of(Minecraft.getInstance().options.mouseWheelSensitivity()), 2),
                                o -> o.renderableVList.addOptions(
                                LegacyOptions.of(minecraft.options.invertYMouse()),
                                LegacyOptions.systemCursor,
                                LegacyOptions.cursorAtFirstInventorySlot,
                                LegacyOptions.of(minecraft.options.rawMouseInput()),
                                /*? if >=1.21.10 {*//*LegacyOptions.of(minecraft.options.allowCursorChanges()),*//*?}*/
                                LegacyOptions.of(minecraft.options.discreteMouseScroll()),
                                LegacyOptions.of(minecraft.options.touchscreen())
                        ))))))).build(),Button.builder(Component.translatable("controls.keybinds.title"), button -> this.minecraft.setScreen(new LegacyKeyMappingScreen(r.getScreen()))).build(),Button.builder(Component.translatable("legacy.options.selectedController"), button -> this.minecraft.setScreen(new ControllerMappingScreen(r.getScreen()))).build()))).build());
        renderableVList.addRenderable(openScreenButton(Component.translatable("legacy.menu.settings"),()->new SettingsScreen(this)).build());
        renderableVList.addRenderable(openScreenButton(Component.translatable("credits_and_attribution.button.credits"),()->new RenderableVListScreen(this,Component.translatable("credits_and_attribution.screen.title"),r-> r.addRenderables(openScreenButton(Component.translatable("credits_and_attribution.button.credits"),()->new WinScreen(false, () -> this.minecraft.setScreen(r.getScreen()))).build(),Button.builder(Component.translatable("credits_and_attribution.button.attribution"), b-> Minecraft.getInstance().setScreen(ConfirmationScreen.createLinkScreen(r.getScreen(), "https://aka.ms/MinecraftJavaAttribution"))).build(),Button.builder(Component.translatable("credits_and_attribution.button.licenses"), b-> Minecraft.getInstance().setScreen(ConfirmationScreen.createLinkScreen(r.getScreen(), "https://aka.ms/MinecraftJavaLicenses"))).build()))).build());
        if (LegacyOptions.displayReinstallContentButton.get()) {
            renderableVList.addRenderable(Button.builder(REINSTALL_CONTENT, this::reinstallContent).build());
        }
    }

    private void reinstallContent(Button button) {
        setReinstallButtonActive(button, false);
        ContentReinstaller.reinstallInstalledContent().whenComplete((result, throwable) ->
            Minecraft.getInstance().execute(() -> finishReinstallContent(button, result, throwable))
        );
    }

    private void finishReinstallContent(Button button, ContentReinstaller.Result result, Throwable throwable) {
        setReinstallButtonActive(button, true);
        Minecraft minecraft = Minecraft.getInstance();
        if (result != null && result.requiresResourceReload()) minecraft.reloadResourcePacks();
        if (minecraft.screen != this) return;
        if (throwable != null) {
            minecraft.setScreen(ConfirmationScreen.createInfoScreen(this, REINSTALL_CONTENT, Component.translatable("legacy.menu.reinstall_content.error")));
            return;
        }
        minecraft.setScreen(ConfirmationScreen.createInfoScreen(this, REINSTALL_CONTENT, reinstallContentMessage(result)));
    }

    private void setReinstallButtonActive(Button button, boolean active) {
        button.active = active;
        button.setMessage(active ? REINSTALL_CONTENT : REINSTALLING_CONTENT);
    }

    private Component reinstallContentMessage(ContentReinstaller.Result result) {
        if (result.failed() > 0 && result.updated() > 0) {
            return Component.translatable("legacy.menu.reinstall_content.partial", result.updated(), result.failed());
        }
        if (result.failed() > 0) {
            return Component.translatable("legacy.menu.reinstall_content.failed", result.failed());
        }
        if (result.updated() > 0) {
            return Component.translatable("legacy.menu.reinstall_content.success", result.updated());
        }
        return Component.translatable("legacy.menu.reinstall_content.none");
    }

    private static void addPlayerSkinOptions(OptionsScreen screen) {
        List<AbstractWidget> legacyWidgets = createLegacyPlayerSkinWidgets();
        if (!legacyWidgets.isEmpty()) {
            screen.renderableVList.addCategory(LEGACY_SKIN_OPTIONS);
            screen.renderableVList.renderables.addAll(legacyWidgets);
        }
        List<AbstractWidget> vanillaWidgets = createPlayerSkinWidgets();
        if (!vanillaWidgets.isEmpty()) {
            screen.renderableVList.addCategory(VANILLA_SKIN_OPTIONS);
            screen.renderableVList.renderables.addAll(vanillaWidgets);
        }
    }

    private static void addWidget(List<AbstractWidget> list, FactoryConfig<?> config) {
        AbstractWidget widget = LegacyConfigWidgets.createWidget(config);
        if (widget != null) list.add(widget);
    }

    public static List<AbstractWidget> createLegacyPlayerSkinWidgets() {
        List<AbstractWidget> list = new ArrayList<>();
        addWidget(list, LegacyOptions.tu3ChangeSkinScreen);
        addWidget(list, LegacyOptions.smoothPreviewScroll);
        addWidget(list, LegacyOptions.hideArmorOnAllBoxSkins);
        if (!LegacyOptions.legacySettingsMenus.get()) {
            addWidget(list, LegacyOptions.customSkinAnimation);
        }
        addWidget(list, LegacyOptions.showCustomPackOptionsTooltip);
        return list;
    }

    public static Screen buildChangeSkinOptionsScreen(Screen parent) {
        try {
            return CHANGE_SKIN_OPTIONS.build(parent);
        } catch (Throwable ignored) {
            return new OptionsScreen(parent, new OptionsScreen.Section(
                    Component.translatable("legacy.menu.change_skin"),
                    s -> Panel.centered(s, 250, 150),
                    new ArrayList<>(List.of(HelpAndOptionsScreen::addPlayerSkinOptions))
            ));
        }
    }

}
