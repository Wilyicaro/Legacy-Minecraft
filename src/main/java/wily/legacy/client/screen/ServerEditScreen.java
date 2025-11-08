package wily.legacy.client.screen;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.multiplayer.ServerData;
import net.minecraft.client.multiplayer.resolver.ServerAddress;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;
import wily.legacy.client.CommonColor;
import wily.legacy.client.LegacyOptions;
import wily.legacy.util.LegacyComponents;
import wily.legacy.util.client.LegacyFontUtil;

import java.util.Arrays;

public class ServerEditScreen extends ConfirmationScreen {
    private final ServerData serverData;
    protected EditBox nameBox;
    protected EditBox ipBox;

    public ServerEditScreen(PlayGameScreen parent, ServerData serverData, boolean add) {
        super(parent, ConfirmationScreen::getPanelWidth, () -> LegacyOptions.getUIMode().isSD() ? 140 : 187, add ? LegacyComponents.ADD_SERVER : LegacyComponents.EDIT_SERVER, LegacyComponents.ENTER_NAME, (b) -> {
        });
        this.serverData = serverData;
        okAction = s -> {
            serverData.name = nameBox.getValue();
            serverData.ip = ipBox.getValue();
            if (add) {
                ServerData data = parent.getServers().unhide(serverData.ip);
                if (data != null) {
                    data.copyNameIconFrom(serverData);
                } else {
                    parent.getServers().add(serverData, false);
                }
            }
            parent.getServers().save();
            parent.serverRenderableList.updateServers();
            onClose();
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
        boolean sd = LegacyOptions.getUIMode().isSD();
        int editBoxesHeight = LegacyOptions.getUIMode().isSD() ? 16 : 20;
        int layoutX = panel.x + (panel.width - messageLabel.width) / 2;
        nameBox = new EditBox(font, layoutX, panel.y + (sd ? 32 : 47), renderableVList.listWidth, editBoxesHeight, CommonComponents.EMPTY);
        ipBox = new EditBox(font, layoutX, panel.y + (sd ? 67 : 87), renderableVList.listWidth, editBoxesHeight, LegacyComponents.ENTER_IP);
        nameBox.setValue(serverData.name);
        ipBox.setValue(serverData.ip);
        ipBox.setMaxLength(128);
        nameBox.setResponder(s -> updateAddButtonStatus());
        ipBox.setResponder(s -> updateAddButtonStatus());
        addRenderableWidget(nameBox);
        addRenderableWidget(ipBox);
        this.addRenderableWidget(new LegacySliderButton<>(layoutX, panel.y + (sd ? 86 : 112), renderableVList.listWidth, 16, b -> b.getDefaultMessage(Component.translatable("manageServer.resourcePack"), b.getObjectValue().getName()), b -> null, this.serverData.getResourcePackStatus(), () -> Arrays.stream(ServerData.ServerPackStatus.values()).toList(), b -> this.serverData.setResourcePackStatus(b.objectValue)));
        this.setInitialFocus(this.nameBox);
        updateAddButtonStatus();
    }

    @Override
    public void render(GuiGraphics guiGraphics, int i, int j, float f) {
        super.render(guiGraphics, i, j, f);
        int textX = panel.x + (panel.width - messageLabel.width) / 2;
        LegacyFontUtil.applySDFont(b -> guiGraphics.drawString(this.font, LegacyComponents.ENTER_IP, textX, panel.y + (b ? 53 : 73), CommonColor.INVENTORY_GRAY_TEXT.get(), false));
    }

    private void updateAddButtonStatus() {
        okButton.active = ServerAddress.isValidAddress(this.ipBox.getValue()) && !this.nameBox.getValue().isEmpty();
    }
}
