package wily.legacy.client.screen;

import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.Renderable;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.layouts.LayoutElement;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.Difficulty;
import net.minecraft.world.level.GameRules;
import net.minecraft.world.level.GameType;
import wily.factoryapi.base.client.SimpleLayoutRenderable;
import wily.factoryapi.base.client.UIDefinition;
import wily.factoryapi.base.network.CommonNetwork;
import wily.legacy.Legacy4J;
import wily.legacy.Legacy4JClient;
import wily.legacy.client.LegacyOptions;
import wily.legacy.config.LegacyCommonOptions;
import wily.legacy.network.PlayerInfoSync;
import wily.legacy.network.ServerHostOptionsPayload;
import wily.legacy.util.LegacyComponents;
import wily.legacy.util.LegacySprites;

import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import static wily.legacy.client.screen.LoadSaveScreen.GAME_MODEL_LABEL;

public class GameHostOptionsScreen extends PanelVListScreen {
    private static final List<String> WEATHERS = List.of("clear", "rain", "thunder");

    protected final Map<String, Object> nonOpGamerules = new HashMap<>();
    protected final Map<Object, Runnable> actionsOnClose = new LinkedHashMap<>();

    public GameHostOptionsScreen(Screen parent, Minecraft minecraft) {
        super(parent, s -> Panel.createPanel(s,
                p -> p.appearance(LegacySprites.PANEL, 265, ((GameHostOptionsScreen) s).getPanelHeight(minecraft.player.hasPermissions(2), LegacyOptions.useLegacyWorldOptions())),
                p -> p.centered(s)), HostOptionsScreen.HOST_OPTIONS);
        getRenderableVList().layoutSpacing(l -> 2);

        boolean isOp = minecraft.player.hasPermissions(2);
        boolean legacyMenus = LegacyOptions.useLegacyWorldOptions();
        accessor.getStaticDefinitions().add(UIDefinition.createBeforeInit(a -> a.putStaticElement("isOp", isOp)));

        if (!isOp) {
            List<GameRules.Key<GameRules.BooleanValue>> nonOpRules = legacyMenus ? HostOptionsScreen.LEGACY_OTHER_RULES : PlayerInfoSync.All.NON_OP_GAMERULES;
            for (GameRules.Key<GameRules.BooleanValue> key : nonOpRules) {
                getRenderableVList().addRenderable(new TickBox(0, 0, Legacy4JClient.gameRules.getRule(key).get(), b -> LegacyComponents.getMenuGameRuleName(key), b -> null, b -> nonOpGamerules.put(key.getId(), b.selected)));
            }
            if (!legacyMenus) {
                LegacyCommonOptions.COMMON_STORAGE.configMap.values().forEach(c -> getRenderableVList().addRenderable(LegacyConfigWidgets.createWidget(c, b -> c.sync())));
                Legacy4J.MIXIN_CONFIGS_STORAGE.configMap.values().forEach(c -> getRenderableVList().addRenderable(LegacyConfigWidgets.createWidget(c, b -> c.sync())));
            }
            return;
        }

        int initialWeather = minecraft.level.isThundering() ? 2 : minecraft.level.isRaining() ? 1 : 0;
        List<GameRules.Key<GameRules.BooleanValue>> worldRules = legacyMenus ? HostOptionsScreen.LEGACY_WORLD_RULES : HostOptionsScreen.WORLD_RULES;
        for (GameRules.Key<GameRules.BooleanValue> key : worldRules)
            getRenderableVList().addRenderable(new TickBox(0, 0, Legacy4JClient.gameRules.getRule(key).get(), b -> LegacyComponents.getMenuGameRuleName(key), b -> null, b -> queueHostCommand(key.getId(), "gamerule %s %s".formatted(key.getId(), b.selected))));
        getRenderableVList().addRenderable(new LegacyButton(Component.translatable("legacy.menu.host_options.set_day"), b1 -> queueHostAction("time", () -> runHostAction(ServerHostOptionsPayload.time("day"), "time set day"))));
        getRenderableVList().addRenderable(new LegacyButton(Component.translatable("legacy.menu.host_options.set_night"), b1 -> queueHostAction("time", () -> runHostAction(ServerHostOptionsPayload.time("night"), "time set night"))));
        getRenderableVList().addRenderable(new LegacySliderButton<>(0, 0, 230, 16, b1 -> b1.getDefaultMessage(Component.translatable("options.difficulty"), b1.getObjectValue().getDisplayName()), b1 -> Tooltip.create(minecraft.level.getDifficulty().getInfo()), minecraft.level.getDifficulty(), () -> Arrays.asList(Difficulty.values()), b1 -> queueHostAction("difficulty", () -> runHostAction(ServerHostOptionsPayload.difficulty(b1.getObjectValue()), "difficulty " + b1.getObjectValue().getSerializedName()))));
        List<GameType> gameTypes = Arrays.stream(GameType.values()).toList();
        getRenderableVList().addRenderable(new LegacySliderButton<>(0, 0, 230, 16, b1 -> b1.getDefaultMessage(GAME_MODEL_LABEL, b1.getObjectValue().getShortDisplayName()), b1 -> Tooltip.create(Component.translatable("selectWorld.gameMode." + b1.getObjectValue().getName() + ".info")), Legacy4JClient.defaultServerGameType, () -> gameTypes, b1 -> queueHostAction("gameMode", () -> runHostAction(ServerHostOptionsPayload.defaultGameMode(b1.getObjectValue()), "defaultgamemode " + b1.getObjectValue().getName()))));
        getRenderableVList().addRenderable(new LegacyButton(Component.translatable("legacy.menu.host_options.set_world_spawn"), b1 -> queueHostAction("worldSpawn", () -> runHostAction(ServerHostOptionsPayload.worldSpawn(), "setworldspawn"))));
        getRenderableVList().addRenderables(SimpleLayoutRenderable.create(240, 12, l -> (graphics, i, j, f) -> {}));
        getRenderableVList().addCategory(Component.translatable("soundCategory.weather"));
        getRenderableVList().addRenderable(new LegacySliderButton<>(0, 0, 230, 16, b -> Component.translatable("legacy.weather_state." + b.getObjectValue()), b -> null, WEATHERS.get(initialWeather), () -> WEATHERS, b -> {
            if (!Objects.equals(b.getObjectValue(), WEATHERS.get(initialWeather))) queueHostAction("weather", () -> runHostAction(ServerHostOptionsPayload.weather(b.getObjectValue()), "weather " + b.getObjectValue()));
        }));

        List<GameRules.Key<GameRules.BooleanValue>> otherRules = legacyMenus ? HostOptionsScreen.LEGACY_OTHER_RULES : HostOptionsScreen.OTHER_RULES;
        for (GameRules.Key<GameRules.BooleanValue> key : otherRules) {
            getRenderableVList().addRenderable(new TickBox(0, 0, Legacy4JClient.gameRules.getRule(key).get(), b -> LegacyComponents.getMenuGameRuleName(key), b -> null, b -> queueHostCommand(key.getId(), "gamerule %s %s".formatted(key.getId(), b.selected))));
        }

        if (legacyMenus) return;

        LegacyCommonOptions.COMMON_STORAGE.configMap.values().forEach(c -> getRenderableVList().addRenderable(LegacyConfigWidgets.createWidget(c, b -> c.sync())));
        Legacy4J.MIXIN_CONFIGS_STORAGE.configMap.values().forEach(c -> getRenderableVList().addRenderable(LegacyConfigWidgets.createWidget(c, b -> c.sync())));
        if (minecraft.hasSingleplayerServer() && !minecraft.getSingleplayerServer().isPublished()) return;
        if (legacyMenus && HostOptionsScreen.getActualPlayerInfos().size() <= 1) return;

        getRenderableVList().addRenderable(createTeleportButton(minecraft, true, Component.translatable("legacy.menu.host_options.teleport_player")));
        getRenderableVList().addRenderable(createTeleportButton(minecraft, false, Component.translatable("legacy.menu.host_options.teleport_me")));
    }

    protected Button createTeleportButton(Minecraft minecraft, boolean toPlayer, Component title) {
        return new LegacyButton(title, b1 -> minecraft.setScreen(new HostOptionsScreen(title) {
            @Override
            protected void addHostOptionsButton() {
            }

            @Override
            protected void addPlayerButtons() {
                addPlayerButtons(false, (profile, b1) -> {
                    if (toPlayer) minecraft.player.connection.sendCommand("tp %s".formatted(profile.getProfile().getName()));
                    else minecraft.player.connection.sendCommand("tp %s ~ ~ ~".formatted(profile.getProfile().getName()));
                });
            }

            public boolean isPauseScreen() {
                return false;
            }
        }));
    }

    protected void runHostAction(ServerHostOptionsPayload payload, String fallbackCommand) {
        if (Legacy4JClient.hasModOnServer()) CommonNetwork.sendToServer(payload);
        else minecraft.player.connection.sendCommand(fallbackCommand);
    }

    protected void runHostCommand(String command) {
        runHostAction(ServerHostOptionsPayload.command(command), command);
    }

    protected void queueHostAction(Object key, Runnable action) {
        actionsOnClose.remove(key);
        actionsOnClose.put(key, action);
    }

    protected void queueHostCommand(Object key, String command) {
        queueHostAction(key, () -> runHostCommand(command));
    }

    protected int getPanelHeight(boolean isOp, boolean legacyMenus) {
        int baseHeight = isOp ? 200 : 130;
        if (!legacyMenus) return baseHeight;

        int contentHeight = 16;
        int entryCount = 0;
        for (Renderable renderable : getRenderableVList().renderables) {
            if (renderable instanceof LayoutElement element) {
                contentHeight += element.getHeight();
                entryCount++;
            }
        }
        if (entryCount > 1) contentHeight += (entryCount - 1) * 2;
        return Math.min(baseHeight, contentHeight);
    }

    @Override
    public void renderableVListInit() {
        getRenderableVList().init(panel.x + 8, panel.y + 8, panel.width - 16, panel.height - 16);
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (keyCode == InputConstants.KEY_O && LegacyOptions.revealAdvancedWorldOptions()) {
            GameHostOptionsScreen screen = new GameHostOptionsScreen(parent, minecraft);
            screen.nonOpGamerules.putAll(nonOpGamerules);
            screen.actionsOnClose.putAll(actionsOnClose);
            minecraft.setScreen(screen);
            return true;
        }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    @Override
    protected void panelInit() {
        panel.init();
    }

    @Override
    public void onClose() {
        super.onClose();
        actionsOnClose.values().forEach(Runnable::run);
        if (!nonOpGamerules.isEmpty() && Legacy4JClient.hasModOnServer()) CommonNetwork.sendToServer(new PlayerInfoSync.All(nonOpGamerules, PlayerInfoSync.All.ID_C2S));
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    @Override
    public void renderDefaultBackground(GuiGraphics guiGraphics, int i, int j, float f) {
        panel.render(guiGraphics, i, j, f);
    }
}
