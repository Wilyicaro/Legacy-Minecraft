package wily.legacy.client.screen;

import com.mojang.datafixers.util.Pair;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.server.IntegratedServer;
import net.minecraft.network.chat.Component;
import net.minecraft.server.commands.PublishCommand;
import net.minecraft.util.HttpUtil;
import net.minecraft.world.level.GameType;
import wily.factoryapi.FactoryAPI;
import wily.legacy.client.LegacyClientWorldSettings;
import wily.legacy.client.LegacyOptions;

import java.util.function.Consumer;

import static wily.legacy.client.screen.LoadSaveScreen.GAME_MODEL_LABEL;
import static wily.legacy.client.screen.LoadSaveScreen.GAME_TYPES;

public class PublishScreen extends ConfirmationScreen {
    public static final Component PORT_INFO_TEXT = Component.translatable("lanServer.port");
    public static final Component LAN_SERVER = Component.translatable("lanServer.title");
    public static final Component PUBLISH = Component.translatable(hasWorldHost() ? "legacy.menu.online" : "menu.shareToLan");
    public static final Component PORT_UNAVAILABLE = Component.translatable("lanServer.port.unavailable", 1024, 65535);
    public static final Component INVALID_PORT = Component.translatable("lanServer.port.invalid", 1024, 65535);
    protected final LegacySliderButton<GameType> gameTypeSlider;
    public boolean publish = false;
    protected EditBox portEdit;
    private int port = HttpUtil.getAvailablePort();

    public PublishScreen(Screen parent, GameType gameType, Consumer<PublishScreen> okAction) {
        super(parent, ConfirmationScreen::getPanelWidth, () -> LegacyOptions.getUIMode().isSD() ? 108 : 145, LAN_SERVER, Component.translatable("lanServer.port"), b -> {
        });
        this.okAction = s -> {
            publish = true;
            okAction.accept(this);
            s.onClose();
        };
        gameTypeSlider = new LegacySliderButton<>(0, 0, 200, 16, b -> b.getDefaultMessage(GAME_MODEL_LABEL, b.getObjectValue().getLongDisplayName()), b -> Tooltip.create(Component.translatable("selectWorld.gameMode." + b.getObjectValue().getName() + ".info")), gameType, () -> GAME_TYPES, b -> {
        });
    }

    public PublishScreen(Screen parent, GameType gameType) {
        this(parent, gameType, s -> {
        });
    }

    public static boolean hasWorldHost() {
        return FactoryAPI.isModLoaded(FactoryAPI.getLoader().isForgeLike() ? "world_host" : "world-host");
    }

    public static Pair<Integer, Component> tryParsePort(String string) {
        if (string.isBlank())
            return Pair.of(HttpUtil.getAvailablePort(), null);
        try {
            int port = Integer.parseInt(string);
            if (port < 1024 || port > 65535) {
                return Pair.of(port, INVALID_PORT);
            }
            if (!HttpUtil.isPortAvailable(port)) {
                return Pair.of(port, PORT_UNAVAILABLE);
            }
            return Pair.of(port, null);
        } catch (NumberFormatException numberFormatException) {
            return Pair.of(HttpUtil.getAvailablePort(), INVALID_PORT);
        }
    }

    @Override
    public void repositionElements() {
        String string = this.portEdit.getValue();
        super.repositionElements();
        this.portEdit.setValue(string);
    }

    @Override
    protected void init() {
        super.init();
        boolean sd = LegacyOptions.getUIMode().isSD();
        int layoutX = panel.x + (panel.width - renderableVList.listWidth) / 2;
        portEdit = new EditBox(font, layoutX, panel.y + (sd ? 32 : 45), renderableVList.listWidth, sd ? 16 : 20, PORT_INFO_TEXT);
        portEdit.setHint(Component.literal("" + this.port).withStyle(ChatFormatting.DARK_GRAY));
        portEdit.setMaxLength(128);
        portEdit.setResponder(string -> {
            Pair<Integer, Component> p = tryParsePort(string);
            if (p.getFirst() != null) port = p.getFirst();
            portEdit.setHint(Component.literal("" + this.port).withStyle(ChatFormatting.DARK_GRAY));
            if (p.getSecond() == null) {
                portEdit.setTextColor(0xFFE0E0E0);
                portEdit.setTooltip(null);
                okButton.active = true;
            } else {
                portEdit.setTextColor(0xFFFF5555);
                portEdit.setTooltip(Tooltip.create(p.getSecond()));
                okButton.active = false;
            }
        });
        addRenderableWidget(portEdit);
        gameTypeSlider.setPosition(layoutX, panel.y + (sd ? 51 : 69));
        gameTypeSlider.setWidth(renderableVList.listWidth);
        addRenderableWidget(gameTypeSlider);
    }

    public void publish(IntegratedServer server) {
        if (!publish) return;
        FactoryAPI.SECURE_EXECUTOR.executeNowIfPossible(() -> this.minecraft.gui.getChat().addMessage(server.publishServer(gameTypeSlider.getObjectValue(), server.getWorldData()./*? if <1.20.5 {*//*getAllowCommands*//*?} else {*/isAllowCommands/*?}*/() && LegacyClientWorldSettings.of(server.getWorldData()).trustPlayers(), this.port) ? PublishCommand.getSuccessMessage(this.port) : Component.translatable("commands.publish.failed")), () -> Minecraft.getInstance().player != null);
    }


}
