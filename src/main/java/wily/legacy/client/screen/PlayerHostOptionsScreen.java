package wily.legacy.client.screen;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.multiplayer.PlayerInfo;
import net.minecraft.network.chat.Component;
import net.minecraft.world.level.GameType;
import wily.factoryapi.base.client.UIDefinition;
import wily.factoryapi.base.network.CommonNetwork;
import wily.legacy.Legacy4JClient;
import wily.legacy.client.CommonColor;
import wily.legacy.entity.LegacyPlayerInfo;
import wily.legacy.network.PlayerInfoSync;
import wily.legacy.network.ServerHostOptionsPayload;
import wily.legacy.util.LegacySprites;
import wily.legacy.util.ScreenUtil;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static wily.legacy.client.screen.LoadSaveScreen.GAME_MODEL_LABEL;

public class PlayerHostOptionsScreen extends PanelVListScreen {
    protected final PlayerInfo playerInfo;
    protected final Map<AbstractWidget, Runnable> actionsOnClose = new HashMap<>();

    public PlayerHostOptionsScreen(Screen parent, PlayerInfo playerInfo, Minecraft minecraft) {
        super(parent, s -> Panel.centered(s, LegacySprites.PANEL, 280, playerInfo.getGameMode().isSurvival() ? 120 : 88), HostOptionsScreen.HOST_OPTIONS);
        this.playerInfo = playerInfo;

        boolean isSurvival = playerInfo.getGameMode().isSurvival();
        accessor.getStaticDefinitions().add(UIDefinition.createBeforeInit(a -> a.putStaticElement("isSurvival", isSurvival)));

        boolean initialVisibility = !((LegacyPlayerInfo) playerInfo).legacy$isVisible();
        List<GameType> gameTypes = Arrays.stream(GameType.values()).toList();

        getRenderableVList().addRenderable(new TickBox(0, 0, initialVisibility, b -> Component.translatable("legacy.menu.host_options.player.invisible"), b -> null, b -> {
            if (initialVisibility != b.selected) actionsOnClose.put(b, () -> {
                if (Legacy4JClient.hasModOnServer()) CommonNetwork.sendToServer(PlayerInfoSync.invisibility(b.selected, playerInfo.getProfile()));
            });
            else actionsOnClose.remove(b);
        }));
        if (isSurvival) {
            getRenderableVList().addRenderable(new TickBox(0, 0, ((LegacyPlayerInfo) playerInfo).mayFlySurvival(), b -> Component.translatable("legacy.menu.host_options.player.mayFly"), b -> null, b -> {
                if (Legacy4JClient.hasModOnServer()) CommonNetwork.sendToServer(PlayerInfoSync.mayFlySurvival(b.selected, playerInfo.getProfile()));
            }));
            getRenderableVList().addRenderable(new TickBox(0, 0, ((LegacyPlayerInfo) playerInfo).isExhaustionDisabled(), b -> Component.translatable("legacy.menu.host_options.player.disableExhaustion"), b -> null, b -> {
                if (Legacy4JClient.hasModOnServer()) CommonNetwork.sendToServer(PlayerInfoSync.disableExhaustion(b.selected, playerInfo.getProfile()));
            }));
        }
        getRenderableVList().addRenderable(new LegacySliderButton<>(0, 0, 230, 16, b -> b.getDefaultMessage(GAME_MODEL_LABEL, b.getObjectValue().getShortDisplayName()), b -> Tooltip.create(Component.translatable("selectWorld.gameMode." + playerInfo.getGameMode().getName() + ".info")), playerInfo.getGameMode(), () -> gameTypes, b -> actionsOnClose.put(b, () -> runHostAction(minecraft, ServerHostOptionsPayload.gameMode(b.getObjectValue(), playerInfo.getProfile().getId()), "gamemode %s %s".formatted(b.getObjectValue().getName(), playerInfo.getProfile().getName())))));
        getRenderableVList().addRenderable(Button.builder(Component.translatable("legacy.menu.host_options.set_player_spawn"), b -> actionsOnClose.put(b, () -> runHostAction(minecraft, ServerHostOptionsPayload.playerSpawn(playerInfo.getProfile().getId()), "spawnpoint %s ~ ~ ~".formatted(playerInfo.getProfile().getName())))).bounds(0, 0, 215, 20).build());
    }

    protected void runHostAction(Minecraft minecraft, ServerHostOptionsPayload payload, String fallbackCommand) {
        if (Legacy4JClient.hasModOnServer()) CommonNetwork.sendToServer(payload);
        else minecraft.player.connection.sendCommand(fallbackCommand);
    }

    @Override
    public void renderableVListInit() {
        getRenderableVList().init(
                accessor.getInteger("renderableVList.x", panel.x + 8),
                accessor.getInteger("renderableVList.y", panel.y + 27),
                accessor.getInteger("renderableVList.width", panel.width - 16),
                accessor.getInteger("renderableVList.height", panel.height - 16));
    }

    @Override
    protected void panelInit() {
        panel.init();
    }

    @Override
    public void onClose() {
        actionsOnClose.values().forEach(Runnable::run);
        super.onClose();
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    @Override
    public void renderDefaultBackground(GuiGraphics guiGraphics, int i, int j, float f) {
        panel.render(guiGraphics, i, j, f);
        HostOptionsScreen.drawPlayerIcon((LegacyPlayerInfo) playerInfo, guiGraphics, panel.x + accessor.getInteger("playerIcon.x", 7), panel.y + accessor.getInteger("playerIcon.y", 5));
        ScreenUtil.applySDFont(ignored -> guiGraphics.drawString(font, playerInfo.getProfile().getName(), panel.x + accessor.getInteger("playerName.x", 31), panel.y + accessor.getInteger("playerName.y", 12), CommonColor.INVENTORY_GRAY_TEXT.get(), false));
    }
}
