package wily.legacy.skins.client.screen;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import wily.legacy.client.CommonColor;
import wily.legacy.client.LegacyOptions;
import wily.legacy.client.screen.ConfirmationScreen;
import wily.legacy.skins.SkinsClientBootstrap;
import wily.legacy.skins.skin.CustomSkinPackStore;
import wily.legacy.skins.skin.SkinPackFiles;
import wily.legacy.util.LegacyComponents;
import wily.legacy.util.client.LegacyFontUtil;

import java.io.IOException;
import java.nio.file.Path;

public class CreateCustomSkinPackScreen extends ConfirmationScreen {
    private static final Component ADD_TITLE = Component.translatable("legacy.menu.add_custom_skin_pack");
    private static final Component EDIT_TITLE = Component.translatable("legacy.menu.edit_custom_skin_pack");
    private static final Component MESSAGE = Component.translatable("legacy.menu.custom_skin_pack_name");
    private static final Component CHOOSE_ICON = Component.translatable("legacy.menu.choose_pack_icon");
    private static final Component REORDER = Component.translatable("legacy.menu.reorder_custom_skin_pack");
    private final Screen rootParent;
    private final String packId;
    private final String initialName;
    private EditBox nameBox;
    private Button iconButton, reorderButton;
    private Path iconPath;

    private CreateCustomSkinPackScreen(Screen parent, Screen rootParent, String packId, String initialName) {
        super(parent, ConfirmationScreen::getPanelWidth, () -> LegacyOptions.getUIMode().isSD() ? (packId == null ? 120 : 142) : (packId == null ? 148 : 174), packId == null ? ADD_TITLE : EDIT_TITLE, Component.empty(), screen -> {
        });
        this.rootParent = rootParent;
        this.packId = packId;
        this.initialName = initialName == null ? "" : initialName;
        okAction = screen -> applyPack(false);
    }

    public static CreateCustomSkinPackScreen create(Screen parent, Screen rootParent) {
        return new CreateCustomSkinPackScreen(parent, rootParent, null, "");
    }

    public static CreateCustomSkinPackScreen edit(Screen parent, Screen rootParent, String packId, String name) {
        return new CreateCustomSkinPackScreen(parent, rootParent, packId, name);
    }

    private static String errorText(Exception ex) {
        String message = ex.getMessage();
        return message == null || message.isBlank() ? ex.toString() : message;
    }

    @Override
    protected void init() {
        super.init();
        boolean sd = LegacyOptions.getUIMode().isSD();
        int fieldHeight = sd ? 16 : 20;
        int layoutX = panel.x + (panel.width - renderableVList.listWidth) / 2;
        int nameY = panel.y + (sd ? 36 : 48);
        nameBox = new EditBox(font, layoutX, nameY, renderableVList.listWidth, fieldHeight, MESSAGE);
        nameBox.setMaxLength(64);
        nameBox.setValue(initialName);
        nameBox.setResponder(value -> updateSaveButtonStatus());
        addRenderableWidget(nameBox);
        setInitialFocus(nameBox);
        updateSaveButtonStatus();
    }

    @Override
    public void repositionElements() {
        String name = nameBox == null ? "" : nameBox.getValue();
        super.repositionElements();
        if (nameBox != null) nameBox.setValue(name);
    }

    @Override
    protected void addButtons() {
        renderableVList.addRenderable(Button.builder(Component.translatable("gui.cancel"), button -> onClose()).build());
        renderableVList.addRenderable(iconButton = Button.builder(CHOOSE_ICON, button -> browseForIcon()).build());
        if (packId != null)
            renderableVList.addRenderable(reorderButton = Button.builder(REORDER, button -> applyPack(true)).build());
        renderableVList.addRenderable(okButton = Button.builder(packId == null ? LegacyComponents.CREATE : LegacyComponents.EDIT, button -> okAction.accept(this)).build());
    }

    @Override
    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        super.render(guiGraphics, mouseX, mouseY, partialTick);
        if (nameBox == null) return;
        int textX = panel.x + (panel.width - renderableVList.listWidth) / 2;
        int textY = nameBox.getY() - (LegacyOptions.getUIMode().isSD() ? 11 : 14);
        LegacyFontUtil.applySDFont(ignored -> guiGraphics.drawString(font, MESSAGE, textX, textY, CommonColor.GRAY_TEXT.get(), false));
    }

    private void browseForIcon() {
        try {
            Path selected = SkinPackFiles.choosePng(minecraft, CHOOSE_ICON.getString());
            if (selected == null) return;
            iconPath = selected;
        } catch (Exception ex) {
            if (minecraft != null)
                minecraft.setScreen(ConfirmationScreen.createInfoScreen(this, title, Component.literal(errorText(ex))));
        }
    }

    private void applyPack(boolean reorder) {
        if (minecraft == null || nameBox == null) return;
        try {
            String targetPackId = packId == null ? CustomSkinPackStore.createPack(minecraft, nameBox.getValue(), iconPath) : packId;
            if (packId != null) CustomSkinPackStore.updatePack(minecraft, packId, nameBox.getValue(), iconPath);
            CustomSkinPackStore.enableResourcePack(minecraft);
            SkinsClientBootstrap.reloadChangeSkinScreen(minecraft, rootParent == null ? parent : rootParent, targetPackId, null, reorder);
        } catch (IOException ex) {
            minecraft.setScreen(ConfirmationScreen.createInfoScreen(this, reorder ? EDIT_TITLE : title, Component.literal(errorText(ex))));
        }
    }

    private void updateSaveButtonStatus() {
        boolean active = nameBox != null && !nameBox.getValue().trim().isEmpty();
        if (okButton != null) okButton.active = active;
        if (reorderButton != null) reorderButton.active = active;
    }
}
