package wily.legacy.client.screen;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.Renderable;
import net.minecraft.util.FormattedCharSequence;
import wily.legacy.client.ContentManager;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import wily.factoryapi.base.client.UIAccessor;
import wily.legacy.client.CommonColor;
import wily.legacy.skins.client.preview.PlayerSkinWidget;
import wily.legacy.util.LegacySprites;
import wily.legacy.util.client.LegacyRenderUtil;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

public class Legacy4JStoreScreen extends PanelVListScreen implements ControlTooltip.Event {

    private static final Component TITLE_LABEL = Component.translatable("legacy.menu.store_title");
    private static final Component STORE_NO_CONTENT = Component.translatable("legacy.menu.store_no_content");
    private static final int LIST_HEIGHT = 162;
    private final Panel panelRecess;
    private final Map<String, CompletableFuture<List<ContentManager.Pack>>> prefetchedIndexes = new HashMap<>();
    private boolean warnNoContent = false;
    public boolean isLoading = false;

    public Legacy4JStoreScreen(Screen parent, List<ContentManager.Category> categories) {
        super(s -> Panel.createPanel(s, 
                p -> p.appearance(310, 232),
                p -> p.pos(p.centeredLeftPos(s), p.centeredTopPos(s) + 26)),
                TITLE_LABEL
        );
        panelRecess = Panel.createPanel(this,
                p -> p.appearance(LegacySprites.PANEL_RECESS, panel.getWidth() - 22, panel.getHeight() - 40),
                p -> p.pos(panel.getX() + 11, panel.getY() + 31));
        this.parent = parent;
        renderableVList.layoutSpacing(l -> 0);        

        if (categories.isEmpty()) {
            warnNoContent = true;
            return;
        }

        // Loop through the categories provided and create buttons
        for (ContentManager.Category category : categories) {
            prefetchCategory(category);
            addMenuButton(category.title(), b -> {
                if (this.isLoading) return;
                CompletableFuture<List<ContentManager.Pack>> future = prefetchCategory(category);
                if (!future.isDone()) this.isLoading = true;
                future.thenAccept(packs -> {
                    minecraft.execute(() -> {
                        this.isLoading = false;
                        minecraft.setScreen(new Legacy4JContentListScreen(this, category, packs));
                    });
                }).exceptionally(ex -> {
                    minecraft.execute(() -> this.isLoading = false);
                    return null;
                });
            });
        }
    }

    private CompletableFuture<List<ContentManager.Pack>> prefetchCategory(ContentManager.Category category) {
        return prefetchedIndexes.computeIfAbsent(category.id(), id -> ContentManager.fetchIndex(category));
    }

    @Override
    public void tick() {
        super.tick();

        if (warnNoContent) {
            minecraft.setScreen(ConfirmationScreen.createInfoScreen(this, TITLE_LABEL, Component.translatable("legacy.menu.store_no_content_message")));
            warnNoContent = false;
        }
    }

    @Override
    protected void panelInit() {
        super.panelInit();
        addRenderableOnly(panelRecess);
        panelRecess.init("panelRecess");
    }

    private void addMenuButton(Component name, Button.OnPress onPress) {
        renderableVList.addRenderable(new LeftAlignedButton(324, 30, name, onPress));
    }

    @Override
    public void initRenderableVListEntry(RenderableVList renderableVList, Renderable renderable) {
        if (renderable instanceof AbstractWidget widget)
            widget.setHeight(accessor.getInteger("buttonsHeight", 30));
    }

    @Override
    public Component getTitle() {
        return getRenderableVList().renderables.isEmpty() ? STORE_NO_CONTENT : super.getTitle();
    }

    @Override
    public void renderableVListInit() {
        addRenderableOnly((guiGraphics, i, j, f) -> {
            int y = panelRecess.y + 8;
            for (FormattedCharSequence formattedCharSequence : font.split(getTitle(), getRenderableVList().listWidth - 10)) {
                guiGraphics.drawString(font, formattedCharSequence, panel.getX() + (panel.getWidth() - font.width(formattedCharSequence)) / 2, y, CommonColor.GRAY_TEXT.get(), false);
                y += 12;
            }
        });
        getRenderableVList().init("renderableVList", panelRecess.getX() + 10, panelRecess.getY() + 21, panelRecess.getWidth() - 20, LIST_HEIGHT);
    }

    @Override
    public void renderDefaultBackground(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        LegacyRenderUtil.renderDefaultBackground(UIAccessor.of(this), guiGraphics, false);
        LegacyRenderUtil.renderLogo(guiGraphics);
        panel.render(guiGraphics, mouseX, mouseY, partialTick);

        if (isLoading) {
            LegacyRenderUtil.drawGenericLoading(guiGraphics, 
                panel.getX() + (panel.getWidth() - 75) / 2, 
                panel.getY() + 25 + (panel.getHeight() - 35 - 75) / 2
            );
        }
    }

    private static class LeftAlignedButton extends Button {
        public LeftAlignedButton(int width, int height, Component message, OnPress onPress) {
            super(0, 0, width, height, message, onPress, DEFAULT_NARRATION);
        }

        @Override
        protected void renderContents(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
            renderDefaultSprite(graphics);
            Font font = Minecraft.getInstance().font;
            int textY = getY() + (getHeight() - font.lineHeight) / 2 + 1;
            String text = PlayerSkinWidget.clipText(font, getMessage().getString(), Math.max(0, getWidth() - 24));
            graphics.drawString(font, text, getX() + 12, textY, LegacyRenderUtil.getDefaultTextColor(!isHoveredOrFocused()), false);
        }
    }
}
