package wily.legacy.client.screen;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.multiplayer.PlayerInfo;
import net.minecraft.network.chat.Component;
import net.minecraft.world.level.GameType;
import wily.factoryapi.base.client.UIDefinition;
import wily.factoryapi.base.network.CommonNetwork;
import wily.legacy.Legacy4JClient;
import wily.legacy.client.CommonColor;
import wily.legacy.client.LegacyOptions;
import wily.legacy.entity.LegacyPlayerInfo;
import wily.legacy.network.PlayerInfoSync;
import wily.legacy.network.ServerHostOptionsPayload;
import wily.legacy.util.LegacySprites;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static wily.legacy.client.screen.LoadSaveScreen.GAME_MODEL_LABEL;

public class PlayerHostOptionsScreen extends PanelVListScreen {
    protected final PlayerInfo playerInfo;
    protected final Map<AbstractWidget, Runnable> commandsOnClose = new HashMap<>();

    public PlayerHostOptionsScreen(Screen parent, PlayerInfo playerInfo, Minecraft minecraft) {
        super(parent, s -> Panel.centered(s, LegacySprites.PANEL, 280, playerInfo.getGameMode().isSurvival() ? 120 : 88), HostOptionsScreen.HOST_OPTIONS);
        this.playerInfo = playerInfo;

        boolean isSurvival = playerInfo.getGameMode().isSurvival();

        accessor.addStatic(UIDefinition.createBeforeInit(a -> a.putStaticElement("isSurvival", isSurvival)));

        boolean initialVisibility = !LegacyPlayerInfo.of(playerInfo).isVisible();

        List<GameType> gameTypes = Arrays.stream(GameType.values()).toList();
        getRenderableVList().addRenderable(new TickBox(0, 0, initialVisibility, b1 -> Component.translatable("legacy.menu.host_options.player.invisible"), b1 -> null, b1 -> {
            if (initialVisibility != b1.selected) {
                commandsOnClose.put(b1, () -> {
                    if (Legacy4JClient.hasModOnServer()) CommonNetwork.sendToServer(PlayerInfoSync.invisibility(b1.selected, playerInfo.getProfile()));
                });
            } else commandsOnClose.remove(b1);
        }));
        if (playerInfo.getGameMode().isSurvival()) {
            getRenderableVList().addRenderable(new TickBox(0, 0, ((LegacyPlayerInfo) playerInfo).mayFlySurvival(), b1 -> Component.translatable("legacy.menu.host_options.player.mayFly"), b1 -> null, b1 -> {
                if (Legacy4JClient.hasModOnServer()) CommonNetwork.sendToServer(PlayerInfoSync.mayFlySurvival(b1.selected, playerInfo.getProfile()));
            }));
            getRenderableVList().addRenderable(new TickBox(0, 0, ((LegacyPlayerInfo) playerInfo).isExhaustionDisabled(), b1 -> Component.translatable("legacy.menu.host_options.player.disableExhaustion"), b1 -> null, b1 -> {
                if (Legacy4JClient.hasModOnServer()) CommonNetwork.sendToServer(PlayerInfoSync.disableExhaustion(b1.selected, playerInfo.getProfile()));
            }));
        }
        getRenderableVList().addRenderable(new LegacySliderButton<>(0, 0, 230, 16, b1 -> b1.getDefaultMessage(GAME_MODEL_LABEL, b1.getObjectValue().getShortDisplayName()), (b1) -> Tooltip.create(Component.translatable("selectWorld.gameMode." + playerInfo.getGameMode().getName() + ".info")), playerInfo.getGameMode(), () -> gameTypes, b1 -> commandsOnClose.put(b1, () -> {
            if (Legacy4JClient.hasModOnServer()) CommonNetwork.sendToServer(ServerHostOptionsPayload.gameMode(b1.getObjectValue(), playerInfo.getProfile().id()));
            else minecraft.getConnection().sendCommand("gamemode %s %s".formatted(b1.getObjectValue().getName(), playerInfo.getProfile().name()));
        })));
        getRenderableVList().addRenderable(new LegacyButton(0, 0, 215, 20, Component.translatable("legacy.menu.host_options.set_player_spawn"), b1 -> commandsOnClose.put(b1, () -> {
            if (Legacy4JClient.hasModOnServer()) CommonNetwork.sendToServer(ServerHostOptionsPayload.playerSpawn(playerInfo.getProfile().id()));
            else minecraft.player.connection.sendCommand("spawnpoint %s ~ ~ ~".formatted(playerInfo.getProfile().name()));
        })));
    }

    @Override
    public void renderableVListInit() {
        getRenderableVList().init(panel.x + 8, panel.y + 27, panel.width - 16, panel.height - 16);
    }

    @Override
    protected void panelInit() {
        panel.init();
    }

    @Override
    public void onClose() {
        commandsOnClose.values().forEach(Runnable::run);
        super.onClose();
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    @Override
    public void renderDefaultBackground(GuiGraphicsExtractor GuiGraphicsExtractor, int i, int j, float f) {
        panel.extractRenderState(GuiGraphicsExtractor, i, j, f);
        HostOptionsScreen.drawPlayerIcon((LegacyPlayerInfo) playerInfo, GuiGraphicsExtractor, panel.x + 7, panel.y + 5);
        GuiGraphicsExtractor.text(font, playerInfo.getProfile().name(), panel.x + 31, panel.y + 12, CommonColor.GRAY_TEXT.get(), false);
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor GuiGraphicsExtractor, int i, int j, float f) {
        super.extractRenderState(GuiGraphicsExtractor, i, j, f);
        if (LegacyOptions.legacySettingsMenus.get()) GuiGraphicsExtractor.deferredTooltip = null;
    }
}
