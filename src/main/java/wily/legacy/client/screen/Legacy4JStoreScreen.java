package wily.legacy.client.screen;

import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractButton;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.util.FormattedCharSequence;
import wily.legacy.client.CommonColor;
import wily.legacy.client.ContentManager;
import wily.legacy.util.LegacySprites;
import wily.legacy.util.ScreenUtil;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

public class Legacy4JStoreScreen extends PanelVListScreen {
    private static final Component TITLE = Component.translatable("legacy.menu.store_title");
    private static final Component NO_CONTENT = Component.translatable("legacy.menu.store_no_content");
    private static final int LIST_HEIGHT = 162;

    private final Panel panelRecess;
    private final Map<String, CompletableFuture<List<ContentManager.Pack>>> prefetchedIndexes = new HashMap<>();
    private boolean warnNoContent;
    private boolean loading;

    public Legacy4JStoreScreen(Screen parent, List<ContentManager.Category> categories) {
        super(parent, s -> Panel.createPanel(s,
            p -> p.appearance(310, 232),
            p -> p.pos(p.centeredLeftPos(s), p.centeredTopPos(s) + 26)
        ), TITLE);
        panelRecess = Panel.createPanel(this,
            p -> p.appearance(LegacySprites.PANEL_RECESS, panel.getWidth() - 22, panel.getHeight() - 40),
            p -> p.pos(panel.getX() + 11, panel.getY() + 31)
        );
        renderableVList.layoutSpacing(l -> 0);
        if (categories.isEmpty()) {
            warnNoContent = true;
            return;
        }
        for (ContentManager.Category category : categories) {
            prefetchCategory(category);
            addMenuButton(category.title(), () -> openCategory(category));
        }
    }

    private CompletableFuture<List<ContentManager.Pack>> prefetchCategory(ContentManager.Category category) {
        return prefetchedIndexes.computeIfAbsent(category.id(), id -> ContentManager.fetchIndex(category));
    }

    private void openCategory(ContentManager.Category category) {
        if (loading) return;
        CompletableFuture<List<ContentManager.Pack>> future = prefetchCategory(category);
        if (!future.isDone()) loading = true;
        future.whenComplete((packs, error) -> minecraft.execute(() -> {
            loading = false;
            minecraft.setScreen(new Legacy4JContentListScreen(this, category, packs == null ? List.of() : packs));
        }));
    }

    private void addMenuButton(Component name, Runnable action) {
        renderableVList.addRenderable(new StoreButton(0, 0, 288, 30, name, action));
    }

    @Override
    public void tick() {
        super.tick();
        if (!warnNoContent) return;
        warnNoContent = false;
        minecraft.setScreen(ConfirmationScreen.createInfoScreen(this, TITLE, Component.translatable("legacy.menu.store_no_content_message")));
    }

    @Override
    protected void panelInit() {
        super.panelInit();
        addRenderableOnly(panelRecess);
        panelRecess.init("panelRecess");
    }

    @Override
    public Component getTitle() {
        return getRenderableVList().renderables.isEmpty() ? NO_CONTENT : super.getTitle();
    }

    @Override
    public void renderableVListInit() {
        addRenderableOnly((guiGraphics, i, j, f) -> {
            int y = panelRecess.y + 8;
            for (FormattedCharSequence line : font.split(getTitle(), panelRecess.width - 20)) {
                guiGraphics.drawString(font, line, panel.x + (panel.width - font.width(line)) / 2, y, CommonColor.INVENTORY_GRAY_TEXT.get(), false);
                y += 12;
            }
        });
        getRenderableVList().init("renderableVList", panelRecess.x + 10, panelRecess.y + 21, panelRecess.width - 20, LIST_HEIGHT);
    }

    @Override
    public void renderDefaultBackground(GuiGraphics guiGraphics, int i, int j, float f) {
        ScreenUtil.renderDefaultBackground(accessor, guiGraphics, false);
        ScreenUtil.renderLogo(guiGraphics);
    }

    @Override
    public void render(GuiGraphics guiGraphics, int i, int j, float f) {
        super.render(guiGraphics, i, j, f);
        if (loading) {
            ScreenUtil.drawGenericLoading(guiGraphics, panel.x + (panel.width - 75) / 2, panel.y + 25 + (panel.height - 35 - 75) / 2);
        }
    }

    private static class StoreButton extends AbstractButton {
        private final Runnable action;

        StoreButton(int x, int y, int width, int height, Component message, Runnable action) {
            super(x, y, width, height, message);
            this.action = action;
        }

        @Override
        public void onPress() {
            action.run();
        }

        @Override
        protected void renderScrollingString(GuiGraphics guiGraphics, Font font, int i, int color) {
            ScreenUtil.renderScrollingString(guiGraphics, font, getMessage(), getX() + 12, getY(), getX() + getWidth() - 8, getY() + getHeight(), color, true);
        }

        @Override
        protected void updateWidgetNarration(NarrationElementOutput narrationElementOutput) {
            defaultButtonNarrationText(narrationElementOutput);
        }
    }
}
