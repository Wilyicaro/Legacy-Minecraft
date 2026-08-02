package wily.legacy.client.screen;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import wily.factoryapi.base.client.WidgetAccessor;
import wily.legacy.client.LegacyOptions;
import wily.legacy.util.LegacyComponents;
import wily.legacy.util.ScreenUtil;

public class SaveInfoScreen extends ConfirmationScreen {
    public SaveInfoScreen(Screen parent) {
        super(parent, () -> LegacyOptions.getUIMode().isSD() ? 200 : 275, () -> LegacyOptions.getUIMode().isSD() ? 92 : 130, () -> 0, () -> LegacyOptions.getUIMode().isSD() ? 0 : 25, Component.empty(), w -> w.lineSpacing(LegacyOptions.getUIMode().isSD() ? 8 : 12).withLines(LegacyComponents.AUTOSAVE_MESSAGE, LegacyOptions.getUIMode().isSD() ? 176 : 220), LegacyScreen::onClose);
    }

    protected void addButtons() {
        transparentBackground = false;
        renderableVList.addRenderable(okButton = Button.builder(Component.translatable("gui.ok"), b -> okAction.accept(this)).build());
    }

    @Override
    public void renderableVListInit() {
        boolean sd = LegacyOptions.getUIMode().isSD();
        messageYOffset.set(sd ? 57 : 68);
        okButton.setWidth(sd ? 150 : 200);
        //? if <=1.20.1 {
        /*((WidgetAccessor) okButton).setHeight(sd ? 18 : 20);
        *///?} else {
        okButton.setHeight(sd ? 18 : 20);
        //?}
        int listWidth = LegacyOptions.getUIMode().isSD() ? panel.width - 24 : panel.width - 55;
        renderableVList.init(panel.x + (panel.width - listWidth) / 2, panel.y + panel.height - (sd ? 28 : 40), listWidth, 0);
    }

    @Override
    public boolean shouldCloseOnEsc() {
        return false;
    }

    @Override
    public void renderDefaultBackground(GuiGraphics guiGraphics, int i, int j, float f) {
        ScreenUtil.renderDefaultBackground(accessor, guiGraphics);
    }

    public void render(GuiGraphics guiGraphics, int i, int j, float f) {
        super.render(guiGraphics, i, j, f);
        ScreenUtil.drawAutoSavingIcon(guiGraphics, panel.x + (panel.width - 24) / 2, panel.y + (LegacyOptions.getUIMode().isSD() ? 28 : 36));
    }
}
