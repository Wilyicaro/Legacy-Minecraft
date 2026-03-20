package wily.legacy.client.screen;

//~ gamerule

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.world.Difficulty;
//? if >=1.21.11 {
/*import net.minecraft.world.level.gamerules.GameRule;
import net.minecraft.world.level.gamerules.GameRules;
*///?} else {
import net.minecraft.world.level.GameRules;
//?}
import net.minecraft.world.level.GameType;
import wily.factoryapi.base.client.SimpleLayoutRenderable;
import wily.factoryapi.base.client.UIDefinition;
import wily.factoryapi.base.network.CommonNetwork;
import wily.legacy.Legacy4J;
import wily.legacy.Legacy4JClient;
import wily.legacy.config.LegacyCommonOptions;
import wily.legacy.init.LegacyGameRules;
import wily.legacy.network.PlayerInfoSync;
import wily.legacy.util.LegacySprites;

import java.util.*;
import java.util.function.Supplier;

import static wily.legacy.client.screen.LoadSaveScreen.GAME_MODEL_LABEL;

public class GameHostOptionsScreen extends PanelVListScreen {
    public static final List<GameRules.Key<GameRules.BooleanValue>> WORLD_RULES = new ArrayList<>(List.of(GameRules.RULE_DOFIRETICK, GameRules.RULE_TNT_EXPLODES, GameRules.RULE_DAYLIGHT, GameRules.RULE_KEEPINVENTORY, GameRules.RULE_DOMOBSPAWNING, GameRules.RULE_MOBGRIEFING, LegacyGameRules.GLOBAL_MAP_PLAYER_ICON, LegacyGameRules.LEGACY_SWIMMING, LegacyGameRules.LEGACY_FLIGHT));
    public static final List<GameRules.Key<GameRules.BooleanValue>> OTHER_RULES = new ArrayList<>(List.of(GameRules.RULE_WEATHER_CYCLE, GameRules.RULE_DOMOBLOOT, GameRules.RULE_DOBLOCKDROPS, GameRules.RULE_NATURAL_REGENERATION, GameRules.RULE_DO_IMMEDIATE_RESPAWN));
    public static final List<String> WEATHERS = List.of("clear", "rain", "thunder");

    protected final Map<String, Object> nonOpGamerules = new HashMap<>();
    protected final Map<AbstractWidget, Runnable> commandsOnClose = new HashMap<>();

    public GameHostOptionsScreen(Screen parent, Minecraft minecraft) {
        super(parent, s -> Panel.centered(s, LegacySprites.PANEL, 265, /*? if >=1.21.11 {*//*Commands.LEVEL_GAMEMASTERS.check(minecraft.player.permissions())*//*?} else {*/minecraft.player.hasPermissions(2)/*?}*/ ? 200 : 130), HostOptionsScreen.HOST_OPTIONS);
        getRenderableVList().layoutSpacing(l -> 2);

        boolean isOp = /*? if >=1.21.11 {*//*Commands.LEVEL_GAMEMASTERS.check(minecraft.player.permissions())*//*?} else {*/minecraft.player.hasPermissions(2)/*?}*/;

        accessor.addStatic(UIDefinition.createBeforeInit(a -> a.putStaticElement("isOp", isOp)));

        if (!isOp) {
            for (GameRules.Key<GameRules.BooleanValue> key : PlayerInfoSync.All.NON_OP_GAMERULES)
                getRenderableVList().addRenderable(new TickBox(0, 0, Legacy4JClient.gameRules.getRule(key).get(), b1 -> Component.translatable(key.getDescriptionId()), b1 -> null, b1 -> nonOpGamerules.put(key.getId(), b1.selected)));
            LegacyCommonOptions.COMMON_STORAGE.configMap.values().forEach(c -> getRenderableVList().addRenderable(LegacyConfigWidgets.createWidget(c, b1 -> c.sync())));
            Legacy4J.MIXIN_CONFIGS_STORAGE.configMap.values().forEach(c -> getRenderableVList().addRenderable(LegacyConfigWidgets.createWidget(c, b1 -> c.sync())));
            return;
        }
        int initialWeather = minecraft.level.isThundering() ? 2 : minecraft.level.isRaining() ? 1 : 0;

        for (GameRules.Key<GameRules.BooleanValue> key : WORLD_RULES)
            getRenderableVList().addRenderable(new TickBox(0, 0, Legacy4JClient.gameRules.getRule(key).get(), b1 -> Component.translatable(key.getDescriptionId()), b1 -> null, b1 -> commandsOnClose.put(b1, () -> minecraft.player.connection.sendCommand("gamerule %s %s".formatted(key.getId(), b1.selected)))));
        getRenderableVList().addRenderable(new LegacyButton(Component.translatable("legacy.menu.host_options.set_day"), b1 -> minecraft.player.connection.sendCommand("time set day")));
        getRenderableVList().addRenderable(new LegacyButton(Component.translatable("legacy.menu.host_options.set_night"), b1 -> minecraft.player.connection.sendCommand("time set 14000")));
        getRenderableVList().addRenderable(new LegacySliderButton<>(0, 0, 230, 16, b1 -> b1.getDefaultMessage(Component.translatable("options.difficulty"), b1.getObjectValue().getDisplayName()), b1 -> Tooltip.create(minecraft.level.getDifficulty().getInfo()), minecraft.level.getDifficulty(), () -> Arrays.asList(Difficulty.values()), b1 -> commandsOnClose.put(b1, () -> minecraft.getConnection().sendCommand("difficulty " + b1.getObjectValue().getKey()))));
        Supplier<GameType> gameType = () -> Legacy4JClient.defaultServerGameType == null ? minecraft.gameMode.getPlayerMode() : Legacy4JClient.defaultServerGameType;
        List<GameType> gameTypes = Arrays.stream(GameType.values()).toList();
        getRenderableVList().addRenderable(new LegacySliderButton<>(0, 0, 230, 16, b1 -> b1.getDefaultMessage(GAME_MODEL_LABEL, b1.getObjectValue().getShortDisplayName()), b1 -> Tooltip.create(Component.translatable("selectWorld.gameMode." + gameType.get().getName() + ".info")), gameType.get(), () -> gameTypes, b1 -> commandsOnClose.put(b1, () -> minecraft.getConnection().sendCommand("defaultgamemode " + b1.getObjectValue().getName()))));
        getRenderableVList().addRenderable(new LegacyButton(Component.translatable("legacy.menu.host_options.set_world_spawn"), b1 -> commandsOnClose.put(b1, () -> minecraft.player.connection.sendCommand("setworldspawn"))));
        getRenderableVList().addRenderables(SimpleLayoutRenderable.create(240, 12, (l -> ((graphics, i, j, f) -> {
        }))));
        getRenderableVList().addCategory(Component.translatable("soundCategory.weather"));
        getRenderableVList().addRenderable(new LegacySliderButton<>(0, 0, 230, 16, b1 -> Component.translatable("legacy.weather_state." + b1.getObjectValue()), b1 -> null, WEATHERS.get(initialWeather), () -> WEATHERS, b1 -> {
            if (!Objects.equals(b1.getObjectValue(), WEATHERS.get(initialWeather)))
                commandsOnClose.put(b1, () -> minecraft.getConnection().sendCommand("weather " + b1.getObjectValue()));
        }));
        for (GameRules.Key<GameRules.BooleanValue> key : OTHER_RULES)
            getRenderableVList().addRenderable(new TickBox(0, 0, Legacy4JClient.gameRules.getRule(key).get(), b1 -> Component.translatable(key.getDescriptionId()), b1 -> null, b1 -> commandsOnClose.put(b1, () -> minecraft.player.connection.sendCommand("gamerule %s %s".formatted(key.getId(), b1.selected)))));
        LegacyCommonOptions.COMMON_STORAGE.configMap.values().forEach(c -> getRenderableVList().addRenderable(LegacyConfigWidgets.createWidget(c, b1 -> c.sync())));
        Legacy4J.MIXIN_CONFIGS_STORAGE.configMap.values().forEach(c -> getRenderableVList().addRenderable(LegacyConfigWidgets.createWidget(c, b1 -> c.sync())));
        if (minecraft.hasSingleplayerServer() && !minecraft.getSingleplayerServer().isPublished()) return;
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
                    if (toPlayer)
                        minecraft.player.connection.sendCommand("tp %s".formatted(profile.getProfile().name()));
                    else minecraft.player.connection.sendCommand("tp %s ~ ~ ~".formatted(profile.getProfile().name()));
                });
            }

            public boolean isPauseScreen() {
                return false;
            }
        }));
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
        commandsOnClose.values().forEach(Runnable::run);
        if (!nonOpGamerules.isEmpty())
            CommonNetwork.sendToServer(new PlayerInfoSync.All(nonOpGamerules, PlayerInfoSync.All.ID_C2S));
    }

    public boolean isPauseScreen() {
        return false;
    }

    @Override
    public void renderDefaultBackground(GuiGraphics guiGraphics, int i, int j, float f) {
        panel.render(guiGraphics, i, j, f);
    }
}
