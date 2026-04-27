package wily.legacy.client.screen;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.Renderable;
import net.minecraft.client.gui.layouts.LayoutElement;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.server.permissions.Permissions;
import net.minecraft.world.Difficulty;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.gamerules.GameRule;
import net.minecraft.world.level.gamerules.GameRules;
import wily.factoryapi.base.client.SimpleLayoutRenderable;
import wily.factoryapi.base.client.UIDefinition;
import wily.factoryapi.base.network.CommonNetwork;
import wily.legacy.Legacy4J;
import wily.legacy.Legacy4JClient;
import wily.legacy.client.LegacyOptions;
import wily.legacy.config.LegacyCommonOptions;
import wily.legacy.init.LegacyGameRules;
import wily.legacy.network.PlayerInfoSync;
import wily.legacy.network.ServerHostOptionsPayload;
import wily.legacy.util.LegacyComponents;
import wily.legacy.util.LegacySprites;

import java.util.*;

import static wily.legacy.client.screen.LoadSaveScreen.GAME_MODEL_LABEL;

public class GameHostOptionsScreen extends PanelVListScreen {
    public static final List<GameRule<Boolean>> WORLD_RULES = new ArrayList<>(List.of(GameRules.FIRE_DAMAGE, LegacyGameRules.getTntExplodes(), GameRules.ADVANCE_TIME, GameRules.KEEP_INVENTORY, GameRules.SPAWN_MOBS, GameRules.MOB_GRIEFING, LegacyGameRules.GLOBAL_MAP_PLAYER_ICON.get(), LegacyGameRules.LEGACY_SWIMMING.get(), LegacyGameRules.LEGACY_FLIGHT.get(), LegacyGameRules.LEGACY_OFFHAND_LIMITS.get()));
    public static final List<GameRule<Boolean>> OTHER_RULES = new ArrayList<>(List.of(GameRules.ADVANCE_WEATHER, GameRules.MOB_DROPS, GameRules.BLOCK_DROPS, GameRules.NATURAL_HEALTH_REGENERATION, GameRules.IMMEDIATE_RESPAWN));
    public static final List<GameRule<Boolean>> LEGACY_WORLD_RULES = List.of(GameRules.FIRE_DAMAGE, LegacyGameRules.getTntExplodes(), GameRules.ADVANCE_TIME, GameRules.KEEP_INVENTORY, GameRules.SPAWN_MOBS, GameRules.MOB_GRIEFING);
    public static final List<GameRule<Boolean>> LEGACY_NON_OP_RULES = List.of(GameRules.FIRE_DAMAGE, LegacyGameRules.getTntExplodes(), GameRules.MOB_DROPS, GameRules.BLOCK_DROPS, GameRules.NATURAL_HEALTH_REGENERATION, GameRules.IMMEDIATE_RESPAWN);
    public static final List<GameRule<Boolean>> LEGACY_OTHER_RULES = List.of(GameRules.ADVANCE_WEATHER, GameRules.MOB_DROPS, GameRules.BLOCK_DROPS, GameRules.NATURAL_HEALTH_REGENERATION, GameRules.IMMEDIATE_RESPAWN);
    public static final List<String> WEATHERS = List.of("clear", "rain", "thunder");

    protected final Map<Identifier, Integer> nonOpGamerules = new HashMap<>();
    protected final Map<Object, Runnable> actionsOnClose = new LinkedHashMap<>();

    public GameHostOptionsScreen(Screen parent, Minecraft minecraft) {
        super(parent, s -> Panel.createPanel(s, p -> p.appearance(LegacySprites.PANEL, 265, ((GameHostOptionsScreen) s).getPanelHeight(minecraft.player.permissions().hasPermission(Permissions.COMMANDS_GAMEMASTER), LegacyOptions.legacySettingsMenus.get())), p -> p.centered(s)), HostOptionsScreen.HOST_OPTIONS);
        getRenderableVList().layoutSpacing(l -> 2);

        boolean isOp =  minecraft.player.permissions().hasPermission(Permissions.COMMANDS_GAMEMASTER);
        boolean legacyMenus = LegacyOptions.legacySettingsMenus.get();

        accessor.addStatic(UIDefinition.createBeforeInit(a -> a.putStaticElement("isOp", isOp)));

        if (!isOp) {
            List<GameRule<Boolean>> nonOpRules = legacyMenus ? LEGACY_NON_OP_RULES : PlayerInfoSync.All.NON_OP_GAMERULES;
            for (GameRule<Boolean> key : nonOpRules)
                getRenderableVList().addRenderable(new TickBox(0, 0, Legacy4JClient.gameRules.get(key), b1 -> LegacyComponents.getMenuGameRuleName(key), b1 -> null, b1 -> nonOpGamerules.put(key.getIdentifier(), b1.selected ? 1 : 0)));
            if (!legacyMenus) {
                LegacyCommonOptions.COMMON_STORAGE.configMap.values().forEach(c -> getRenderableVList().addRenderable(LegacyConfigWidgets.createWidget(c, b1 -> c.sync())));
                Legacy4J.MIXIN_CONFIGS_STORAGE.configMap.values().forEach(c -> getRenderableVList().addRenderable(LegacyConfigWidgets.createWidget(c, b1 -> c.sync())));
            }
            return;
        }
        int initialWeather = minecraft.level.isThundering() ? 2 : minecraft.level.isRaining() ? 1 : 0;

        List<GameRule<Boolean>> worldRules = legacyMenus ? LEGACY_WORLD_RULES : WORLD_RULES;
        for (GameRule<Boolean> key : worldRules)
            getRenderableVList().addRenderable(new TickBox(0, 0, Legacy4JClient.gameRules.get(key), b1 -> LegacyComponents.getMenuGameRuleName(key), b1 -> null, b1 -> queueHostCommand(key.getIdentifier(), "gamerule %s %s".formatted(key.id(), b1.selected))));
        getRenderableVList().addRenderable(new LegacyButton(Component.translatable("legacy.menu.host_options.set_day"), b1 -> queueHostAction("time", () -> runHostAction(ServerHostOptionsPayload.time("day"), "time set day"))));
        getRenderableVList().addRenderable(new LegacyButton(Component.translatable("legacy.menu.host_options.set_night"), b1 -> queueHostAction("time", () -> runHostAction(ServerHostOptionsPayload.time("night"), "time set night"))));
        getRenderableVList().addRenderable(new LegacySliderButton<>(0, 0, 230, 16, b1 -> b1.getDefaultMessage(Component.translatable("options.difficulty"), b1.getObjectValue().getDisplayName()), b1 -> Tooltip.create(minecraft.level.getDifficulty().getInfo()), minecraft.level.getDifficulty(), () -> Arrays.asList(Difficulty.values()), b1 -> queueHostAction("difficulty", () -> runHostAction(ServerHostOptionsPayload.difficulty(b1.getObjectValue()), "difficulty " + b1.getObjectValue().getKey()))));
        List<GameType> gameTypes = Arrays.stream(GameType.values()).toList();
        getRenderableVList().addRenderable(new LegacySliderButton<>(0, 0, 230, 16, b1 -> b1.getDefaultMessage(GAME_MODEL_LABEL, b1.getObjectValue().getShortDisplayName()), b1 -> Tooltip.create(Component.translatable("selectWorld.gameMode." + b1.getObjectValue().getName() + ".info")), Legacy4JClient.defaultServerGameType, () -> gameTypes, b1 -> queueHostAction("gameMode", () -> runHostAction(ServerHostOptionsPayload.defaultGameMode(b1.getObjectValue()), "defaultgamemode " + b1.getObjectValue().getName()))));
        getRenderableVList().addRenderable(new LegacyButton(Component.translatable("legacy.menu.host_options.set_world_spawn"), b1 -> queueHostAction("worldSpawn", () -> runHostAction(ServerHostOptionsPayload.worldSpawn(), "setworldspawn"))));
        getRenderableVList().addRenderables(SimpleLayoutRenderable.create(240, 12, (l -> ((graphics, i, j, f) -> {}))));
        getRenderableVList().addCategory(Component.translatable("soundCategory.weather"));
        getRenderableVList().addRenderable(new LegacySliderButton<>(0, 0, 230, 16, b1 -> Component.translatable("legacy.weather_state." + b1.getObjectValue()), b1 -> null, WEATHERS.get(initialWeather), () -> WEATHERS, b1 -> {
            if (!Objects.equals(b1.getObjectValue(), WEATHERS.get(initialWeather)))
                queueHostAction("weather", () -> runHostAction(ServerHostOptionsPayload.weather(b1.getObjectValue()), "weather " + b1.getObjectValue()));
        }));
        List<GameRule<Boolean>> otherRules = legacyMenus ? LEGACY_OTHER_RULES : OTHER_RULES;
        for (GameRule<Boolean> key : otherRules)
            getRenderableVList().addRenderable(new TickBox(0, 0, Legacy4JClient.gameRules.get(key), b1 -> LegacyComponents.getMenuGameRuleName(key), b1 -> null, b1 -> queueHostCommand(key.getIdentifier(), "gamerule %s %s".formatted(key.id(), b1.selected))));
        if (!legacyMenus) {
            LegacyCommonOptions.COMMON_STORAGE.configMap.values().forEach(c -> getRenderableVList().addRenderable(LegacyConfigWidgets.createWidget(c, b1 -> c.sync())));
            Legacy4J.MIXIN_CONFIGS_STORAGE.configMap.values().forEach(c -> getRenderableVList().addRenderable(LegacyConfigWidgets.createWidget(c, b1 -> c.sync())));
        }
        if (minecraft.hasSingleplayerServer() && !minecraft.getSingleplayerServer().isPublished()) return;
        if (legacyMenus && HostOptionsScreen.getActualPlayerInfos().size() <= 1) return;
        getRenderableVList().addRenderable(createTeleportButton(true, Component.translatable("legacy.menu.host_options.teleport_player")));
        getRenderableVList().addRenderable(createTeleportButton(false, Component.translatable("legacy.menu.host_options.teleport_me")));
    }

    protected Button createTeleportButton(boolean toPlayer, Component component) {
        return new LegacyButton(component, b1 -> minecraft.setScreen(new HostOptionsScreen(title) {
            @Override
            protected void addHostOptionsButton() {
            }

            @Override
            protected void addPlayerButtons() {
                addPlayerButtons(false, (profile, b1) -> {
                    if (toPlayer) minecraft.player.connection.sendCommand("tp %s".formatted(profile.getProfile().name()));
                    else minecraft.player.connection.sendCommand("tp %s ~ ~ ~".formatted(profile.getProfile().name()));
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

    protected void runCommand(String command) {
        runHostAction(ServerHostOptionsPayload.command(command), command);
    }

    protected void queueHostAction(Object key, Runnable action) {
        actionsOnClose.remove(key);
        actionsOnClose.put(key, action);
    }

    protected void queueHostCommand(Object key, String command) {
        queueHostAction(key, () -> runCommand(command));
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
    protected void panelInit() {
        panel.init();
    }

    public void onClose() {
        super.onClose();
        actionsOnClose.values().forEach(Runnable::run);
        if (!nonOpGamerules.isEmpty() && Legacy4JClient.hasModOnServer())
            CommonNetwork.sendToServer(new PlayerInfoSync.All(nonOpGamerules, PlayerInfoSync.All.ID_C2S));
    }

    public boolean isPauseScreen() {
        return false;
    }

    @Override
    public void renderDefaultBackground(GuiGraphics guiGraphics, int i, int j, float f) {
        panel.render(guiGraphics, i, j, f);
    }

    @Override
    public void render(GuiGraphics guiGraphics, int i, int j, float f) {
        super.render(guiGraphics, i, j, f);
        if (LegacyOptions.legacySettingsMenus.get()) guiGraphics.deferredTooltip = null;
    }
}
