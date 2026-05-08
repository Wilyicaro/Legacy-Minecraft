package wily.legacy.client.screen;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.WinScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.PlayerModelPart;
import wily.factoryapi.base.ArbitrarySupplier;
import wily.factoryapi.base.client.UIAccessor;
import wily.factoryapi.base.config.FactoryConfig;
import wily.legacy.client.LegacyOptions;
import wily.legacy.util.LegacySprites;

import java.util.ArrayList;
import java.util.List;

public class HelpAndOptionsScreen extends RenderableVListScreen {
    private static final Component LEGACY_SKIN_OPTIONS = Component.literal("Legacy4J Skin Options");
    private static final Component VANILLA_SKIN_OPTIONS = Component.literal("Vanilla Skin Options");

    public static final OptionsScreen.Section HOW_TO_PLAY = new OptionsScreen.Section(Component.translatable("legacy.menu.how_to_play"), s -> Panel.createPanel(s, p -> p.appearance(LegacySprites.PANEL, 240, Math.min(7, s.renderableVList.renderables.size()) * 25 + 24), p -> p.pos(p.centeredLeftPos(s), p.centeredTopPos(s) + 20)), new ArrayList<>(List.of(o -> HowToPlayScreen.Section.getWithButton().forEach(s -> o.getRenderableVList().addRenderable(s.createButtonBuilder(o).build())))), ArbitrarySupplier.empty(), ((screen, section) -> new OptionsScreen(screen, section) {
        @Override
        public void renderableVListInit() {
            getRenderableVList().cyclic(false).layoutSpacing(l -> 5).init(panel.x + 8, panel.getY() + 8, panel.getWidth() - 16, panel.getHeight() - 16);
        }
    }));
    public static final OptionsScreen.Section CHANGE_SKIN_OPTIONS = new OptionsScreen.Section(Component.translatable("legacy.menu.change_skin"), s -> Panel.centered(s, 250, 150), new ArrayList<>(List.of(HelpAndOptionsScreen::addPlayerSkinOptions)));
    public static ScreenSection<?> CHANGE_SKIN = CHANGE_SKIN_OPTIONS;
    private static Screen createMouseSettingsScreen(Screen parent) {
        return new OptionsScreen(parent, new OptionsScreen.Section(
                Component.translatable("options.mouse_settings.title"),
                s -> Panel.centered(s, 250, 175, 0, 10),
                new ArrayList<>(List.of(
                        o -> o.renderableVList.addMultSliderOption(LegacyOptions.of(Minecraft.getInstance().options.sensitivity()), 2),
                        o -> o.renderableVList.addMultSliderOption(LegacyOptions.of(Minecraft.getInstance().options.mouseWheelSensitivity()), 2),
                        o -> o.renderableVList.addOptions(
                                LegacyOptions.of(Minecraft.getInstance().options.invertMouseX()),
                                LegacyOptions.of(Minecraft.getInstance().options.invertMouseY()),
                                LegacyOptions.systemCursor,
                                LegacyOptions.of(Minecraft.getInstance().options.allowCursorChanges()),
                                LegacyOptions.cursorAtFirstInventorySlot,
                                LegacyOptions.of(Minecraft.getInstance().options.rawMouseInput()),
                                LegacyOptions.of(Minecraft.getInstance().options.discreteMouseScroll()),
                                LegacyOptions.of(Minecraft.getInstance().options.touchscreen())
                        )))));
    }

    private static Screen createControlsScreen(Screen parent) {
        return new RenderableVListScreen(parent, Component.translatable("controls.title"), r -> r.addRenderables(
                openScreenButton(Component.translatable("options.mouse_settings.title"), () -> createMouseSettingsScreen(r.getScreen())).build(),
                Button.builder(Component.translatable("controls.keybinds.title"), button -> Minecraft.getInstance().setScreen(new LegacyKeyMappingScreen(r.getScreen()))).build(),
                Button.builder(Component.translatable("legacy.options.selectedController"), button -> Minecraft.getInstance().setScreen(new ControllerMappingScreen(r.getScreen()))).build()));
    }

    private static Screen createCreditsScreen(Screen parent) {
        if (LegacyOptions.legacySettingsMenus.get()) {
            return new WinScreen(false, () -> Minecraft.getInstance().setScreen(parent));
        }
        return new RenderableVListScreen(parent, Component.translatable("credits_and_attribution.screen.title"), r -> r.addRenderables(
                openScreenButton(Component.translatable("credits_and_attribution.button.credits"), () -> new WinScreen(false, () -> Minecraft.getInstance().setScreen(r.getScreen()))).build(),
                Button.builder(Component.translatable("credits_and_attribution.button.attribution"), b -> Minecraft.getInstance().setScreen(ConfirmationScreen.createLinkScreen(r.getScreen(), "https://aka.ms/MinecraftJavaAttribution"))).build(),
                Button.builder(Component.translatable("credits_and_attribution.button.licenses"), b -> Minecraft.getInstance().setScreen(ConfirmationScreen.createLinkScreen(r.getScreen(), "https://aka.ms/MinecraftJavaLicenses"))).build()));
    }
    public HelpAndOptionsScreen(Screen parent) {
        super(parent, Component.translatable("options.title"), r -> {
        });
        renderableVList.addRenderable(CHANGE_SKIN.createButtonBuilder(this).build());
        renderableVList.addRenderable(HOW_TO_PLAY.createButtonBuilder(this).build());
        renderableVList.addRenderable(openScreenButton(Component.translatable("controls.title"), () -> createControlsScreen(this)).build());
        renderableVList.addRenderable(openScreenButton(Component.translatable("legacy.menu.settings"), () -> new SettingsScreen(this)).build());
        renderableVList.addRenderable(openScreenButton(Component.translatable("credits_and_attribution.button.credits"), () -> createCreditsScreen(this)).build());
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

    public static List<AbstractWidget> createPlayerSkinWidgets() {
        List<AbstractWidget> list = new ArrayList<>();
        for (PlayerModelPart p : PlayerModelPart.values()) {
            list.add(new TickBox(0, 0, Minecraft.getInstance().options.isModelPartEnabled(p), b -> p.getName(), b -> null, t -> {
                Minecraft.getInstance().options./*? if <1.21.2 {*//*toggleModelPart*//*?} else {*/setModelPart/*?}*/(p, t.selected);
                Minecraft.getInstance().options.save();
            }));
        }
        list.add(LegacyConfigWidgets.createWidget(LegacyOptions.of(Minecraft.getInstance().options.mainHand())));
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
