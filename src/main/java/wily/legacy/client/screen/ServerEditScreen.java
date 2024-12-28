package wily.legacy.client.screen;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.multiplayer.ServerData;
import net.minecraft.client.multiplayer.resolver.ServerAddress;
import net.minecraft.network.chat.Component;
import wily.factoryapi.base.client.UIDefinition;
import wily.legacy.client.CommonColor;

import java.util.Arrays;

public class ServerEditScreen extends ConfirmationScreen{
    protected EditBox nameBox;
    protected EditBox ipBox;
    private final ServerData serverData;

    public ServerEditScreen(PlayGameScreen parent, ServerData serverData, boolean add) {
        super(parent, 230, 187, Component.translatable("addServer.title"), Component.translatable("addServer.enterName"), (b)->{});
        this.serverData = serverData;
        okAction =  b->{
            serverData.name = nameBox.getValue();
            serverData.ip = ipBox.getValue();
            if (add){
                ServerData data = parent.getServers().unhide(serverData.ip);
                if (data != null) {
                    data.copyNameIconFrom(serverData);
                } else {
                    parent.getServers().add(serverData, false);
                }
            }
            parent.getServers().save();
            parent.serverRenderableList.updateServers();
            return true;
        };
    }
    @Override
    public void repositionElements() {
        String string = this.ipBox.getValue();
        String string2 = this.nameBox.getValue();
        super.repositionElements();
        this.ipBox.setValue(string);
        this.nameBox.setValue(string2);
    }
    @Override
    protected void init() {
        super.init();
        nameBox = new EditBox(font, width / 2 - 100,panel.y + 47,200, 20, Component.empty());
        ipBox = new EditBox(font, width / 2 - 100,panel.y + 87,200, 20, Component.translatable("addServer.enterIp"));
        nameBox.setValue(serverData.name);
        ipBox.setValue(serverData.ip);
        ipBox.setMaxLength(128);
        nameBox.setResponder(s-> updateAddButtonStatus());
        ipBox.setResponder(s-> updateAddButtonStatus());
        addRenderableWidget(nameBox);
        addRenderableWidget(ipBox);
        this.addRenderableWidget(new LegacySliderButton<>(this.width / 2 - 100, panel.y + 112, 200, 16, b-> b.getDefaultMessage(Component.translatable("addServer.resourcePack"),b.getObjectValue().getName()),b-> null,this.serverData.getResourcePackStatus(), ()->Arrays.stream(ServerData.ServerPackStatus.values()).toList(), b->this.serverData.setResourcePackStatus(b.objectValue)));
        this.setInitialFocus(this.nameBox);
        updateAddButtonStatus();
    }

    @Override
    public void render(GuiGraphics guiGraphics, int i, int j, float f) {
        super.render(guiGraphics, i, j, f);
        guiGraphics.drawString(this.font, ipBox.getMessage(), panel.x + 15, panel.y + 73, CommonColor.INVENTORY_GRAY_TEXT.get(),false);
    }

    private void updateAddButtonStatus() {
        okButton.active = ServerAddress.isValidAddress(this.ipBox.getValue()) && !this.nameBox.getValue().isEmpty();
    }
}
