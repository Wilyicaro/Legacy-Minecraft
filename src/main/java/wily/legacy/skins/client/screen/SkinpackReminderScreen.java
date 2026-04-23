package wily.legacy.Skins.client.screen;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;
import wily.legacy.client.CommonColor;
import wily.legacy.client.ContentManager;
import wily.legacy.client.screen.Legacy4JStoreScreen;
import wily.legacy.client.screen.Panel;
import wily.legacy.client.screen.PanelBackgroundScreen;

import java.util.List;

public class SkinpackReminderScreen extends PanelBackgroundScreen {
    private static final Component TITLE = Component.translatable("legacy.menu.skinpack_reminder.title");
    private static final Component MESSAGE = Component.translatable("legacy.menu.skinpack_reminder.message");
    private final Panel panelRecess;
    private List<FormattedCharSequence> titleLines = List.of();
    private List<FormattedCharSequence> messageLines = List.of();

    public SkinpackReminderScreen(Screen parent) {
        super(parent, s -> Panel.centered(s, 340, 236, 0, 24), CommonComponents.EMPTY);
        panelRecess = Panel.createPanel(this, p -> p.appearance(wily.legacy.util.LegacySprites.PANEL_RECESS, panel.getWidth() - 22, panel.getHeight() - 58), p -> p.pos(panel.getX() + 11, panel.getY() + 11));
    }

    @Override
    protected void panelInit() {
        super.panelInit();
        addRenderableOnly(panelRecess);
        panelRecess.init("panelRecess");
    }

    @Override
    protected void init() {
        super.init();
        int textWidth = panelRecess.getWidth() - 20;
        titleLines = font.split(TITLE, textWidth);
        messageLines = font.split(MESSAGE, textWidth);
        int buttonWidth = 98;
        int gap = 4;
        int totalWidth = buttonWidth * 2 + gap;
        int x = panel.getX() + (panel.getWidth() - totalWidth) / 2;
        int y = panel.getY() + panel.getHeight() - 28;
        addRenderableWidget(Button.builder(Component.translatable("legacy.menu.skinpack_reminder.open_store"), b -> minecraft.setScreen(new Legacy4JStoreScreen(parent, ContentManager.CATEGORIES))).bounds(x, y, buttonWidth, 20).build());
        addRenderableWidget(Button.builder(Component.translatable("gui.close"), b -> onClose()).bounds(x + buttonWidth + gap, y, buttonWidth, 20).build());
    }

    @Override
    public void render(GuiGraphics guiGraphics, int i, int j, float f) {
        super.render(guiGraphics, i, j, f);
        int textX = panelRecess.getX() + 10;
        int titleY = panelRecess.getY() + 10;
        int centerX = panelRecess.getX() + panelRecess.getWidth() / 2;
        int y = titleY;
        for (FormattedCharSequence line : titleLines) {
            guiGraphics.drawCenteredString(font, line, centerX, y, CommonColor.WIDGET_TEXT.get());
            y += 12;
        }
        y += 8;
        for (FormattedCharSequence line : messageLines) {
            guiGraphics.drawString(font, line, textX, y, CommonColor.GRAY_TEXT.get(), false);
            y += 12;
        }
    }
}
