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
import wily.legacy.Legacy4JClient;
import wily.legacy.client.LegacyClientWorldSettings;

import java.util.function.Consumer;

import static wily.legacy.client.screen.LoadSaveScreen.GAME_MODEL_LABEL;
import static wily.legacy.client.screen.LoadSaveScreen.GAME_TYPES;

public class PublishScreen extends ConfirmationScreen{
    public static final Component PORT_INFO_TEXT = Component.translatable("lanServer.port");
    public static final Component LAN_SERVER = Component.translatable("lanServer.title");
    public static final Component PUBLISH = Component.translatable(hasWorldHost() ? "legacy.menu.online" : "menu.shareToLan");
    public boolean publish = false;
    protected EditBox portEdit;
    protected final LegacySliderButton<GameType> gameTypeSlider;
    private int port = HttpUtil.getAvailablePort();

    public static boolean hasWorldHost(){
        return FactoryAPI.isModLoaded(FactoryAPI.getLoader().isForgeLike() ? "world_host" : "world-host");
    }

    public PublishScreen(Screen parent, GameType gameType, Consumer<PublishScreen> okAction) {
        super(parent, 230, 145, LAN_SERVER, Component.translatable("lanServer.port"), b-> {});
        this.okAction = b-> {
            publish = true;
            okAction.accept(this);
            return true;
        };
        gameTypeSlider = new LegacySliderButton<>(0,0, 200,16, b -> b.getDefaultMessage(GAME_MODEL_LABEL,b.getObjectValue().getLongDisplayName()),b->Tooltip.create(Component.translatable("selectWorld.gameMode."+b.getObjectValue().getName()+ ".info")),gameType,()->GAME_TYPES, b->{});
    }
    public PublishScreen(Screen parent, GameType gameType) {
        this(parent,gameType,s-> {});
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
        portEdit = new EditBox(font, panel.x + panel.width / 2 - 100,panel.y + 45,200, 20,PORT_INFO_TEXT);
        portEdit.setHint(Component.literal("" + this.port).withStyle(ChatFormatting.DARK_GRAY));
        portEdit.setMaxLength(128);
        portEdit.setResponder(string -> {
            Pair<Integer,Component> p = Legacy4JClient.tryParsePort(string);
            if(p.getFirst() != null) port = p.getFirst();
            portEdit.setHint(Component.literal("" + this.port).withStyle(ChatFormatting.DARK_GRAY));
            if (p.getSecond() == null) {
                portEdit.setTextColor(0xE0E0E0);
                portEdit.setTooltip(null);
                okButton.active = true;
            } else {
                portEdit.setTextColor(0xFF5555);
                portEdit.setTooltip(Tooltip.create(p.getSecond()));
                okButton.active = false;
            }
        });
        addRenderableWidget(portEdit);
        gameTypeSlider.setPosition(panel.x + panel.width / 2 - 100, panel.y + 69);
        addRenderableWidget(gameTypeSlider);
    }
    public void publish(IntegratedServer server){
        if (!publish) return;
        FactoryAPI.SECURE_EXECUTOR.executeNowIfPossible(()->this.minecraft.gui.getChat().addMessage(server.publishServer(gameTypeSlider.getObjectValue(), server.getWorldData()./*? if <1.20.5 {*//*getAllowCommands*//*?} else {*/isAllowCommands/*?}*/() && LegacyClientWorldSettings.of(server.getWorldData()).trustPlayers(), this.port) ? PublishCommand.getSuccessMessage(this.port) : Component.translatable("commands.publish.failed")),()-> Minecraft.getInstance().player != null);
    }


}
