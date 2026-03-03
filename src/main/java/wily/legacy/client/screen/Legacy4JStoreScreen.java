package wily.legacy.client.screen;

import wily.legacy.api.ContentCategory;
import wily.legacy.client.ContentManager;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.LogoRenderer;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.network.chat.Component;
import wily.factoryapi.base.client.UIAccessor;
import wily.legacy.client.CommonColor;
import wily.legacy.client.LegacyOptions;
import wily.legacy.client.screen.ControlTooltip;
import wily.legacy.client.screen.Panel;
import wily.legacy.client.screen.PanelVListScreen;
import wily.legacy.util.LegacySprites;
import wily.legacy.util.client.LegacyRenderUtil;

import java.util.List;

public class Legacy4JStoreScreen extends PanelVListScreen implements ControlTooltip.Event {
    
    public boolean isLoading = false;
    private static final Component TITLE_LABEL = Component.translatable("legacy.menu.store_title");
    private final LogoRenderer logoRenderer = new LogoRenderer(false);

    public Legacy4JStoreScreen(Screen parent, List<ContentCategory> categories) {
        super(s -> Panel.createPanel(s, 
                p -> p.appearance(372, 249), 
                p -> p.pos(p.centeredLeftPos(s), p.centeredTopPos(s) + 17)), 
                Component.empty()
        );
        this.parent = parent;
        renderableVList.layoutSpacing(l -> 0);        
        
        // Loop through the categories provided and create buttons
        for (ContentCategory category : categories) {
            addMenuButton(category.title(), b -> {
                this.isLoading = true;
                ContentManager.fetchIndex(category.indexUrl()).thenAccept(packs -> {
                    minecraft.execute(() -> {
                        this.isLoading = false;
                        minecraft.setScreen(new Legacy4JContentListScreen(this, category, packs));
                    });
                });
            });
        }
    }

    private void addMenuButton(Component name, Button.OnPress onPress) {
        renderableVList.addRenderable(new LeftAlignedButton(324, 36, name, onPress)); 
    }

    @Override
    public void renderableVListInit() {
        initRenderableVListHeight(36);
        
        addRenderableOnly((guiGraphics, i, j, f) -> {
            guiGraphics.blitSprite(
                RenderPipelines.GUI_TEXTURED, 
                LegacySprites.PANEL_RECESS, 
                panel.getX() + 14, 
                panel.getY() + 10, 
                panel.getWidth() - 29,
                panel.getHeight() - 20
            );
            
            UIAccessor accessor = UIAccessor.of(this);
            guiGraphics.pose().pushMatrix();
            
            float textScale = 1.2f;            
            int scaledTextWidth = (int)(font.width(TITLE_LABEL) * textScale);
            int centeredX = panel.getX() + (panel.getWidth() - scaledTextWidth) / 2;             
            guiGraphics.pose().translate(
                accessor.getInteger("titleLabel.x", centeredX), 
                accessor.getInteger("titleLabel.y", panel.getY() + 19)
            );            
            guiGraphics.pose().scale(textScale, textScale);
            
            if (LegacyOptions.getUIMode().isSD()) guiGraphics.pose().scale(0.5f, 0.5f);
            
            guiGraphics.drawString(font, TITLE_LABEL, 0, 0, CommonColor.INVENTORY_GRAY_TEXT.get(), false);
            guiGraphics.pose().popMatrix();
        });

        getRenderableVList().init("renderableVList", panel.getX() + 24, panel.getY() + 32, 324, 180);
    }

    @Override
    public void renderDefaultBackground(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        LegacyRenderUtil.renderDefaultBackground(UIAccessor.of(this), guiGraphics, false);
        logoRenderer.renderLogo(guiGraphics, this.width, 1.0f);
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
        public void renderString(GuiGraphics guiGraphics, Font font, int color) {
            int textY = this.getY() + (this.getHeight() - font.lineHeight) / 2 + 1;
            guiGraphics.drawString(font, this.getMessage(), this.getX() + 12, textY, color, true);
        }
    }
}