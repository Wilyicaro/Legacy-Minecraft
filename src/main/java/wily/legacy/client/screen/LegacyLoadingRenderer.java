package wily.legacy.client.screen;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Renderable;
import net.minecraft.client.renderer.texture.SpriteContents;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.FontDescription;
import net.minecraft.resources.ResourceLocation;
import wily.factoryapi.base.ArbitrarySupplier;
import wily.factoryapi.base.client.FactoryGuiGraphics;
import wily.factoryapi.base.client.UIAccessor;
import wily.legacy.Legacy4JClient;
import wily.legacy.client.CommonColor;
import wily.legacy.client.LegacyTip;
import wily.legacy.util.client.LegacyFontUtil;
import wily.legacy.util.client.LegacyRenderUtil;

import static wily.legacy.util.LegacySprites.LOADING_BACKGROUND;
import static wily.legacy.util.LegacySprites.LOADING_BAR;

public class LegacyLoadingRenderer implements Renderable {
    private static final LegacyLoadingRenderer INSTANCE = new LegacyLoadingRenderer();

    public Minecraft minecraft;
    public float progress;
    public Component loadingHeader;
    public Component loadingStage;
    public boolean genericLoading;
    public UIAccessor accessor;
    private float lastTrackerVisibleProgress = -1.0F;

    public static LegacyLoadingRenderer getInstance() {
        return INSTANCE;
    }

    public void prepareRender(Minecraft minecraft, UIAccessor accessor, Component loadingHeader, Component loadingStage, float progress, boolean genericLoading) {
        this.minecraft = minecraft;
        this.accessor = accessor;
        this.loadingHeader = accessor.getElementValue("loadingHeader.component", loadingHeader, Component.class);
        this.loadingStage = accessor.getElementValue("loadingStage.component", loadingStage, Component.class);
        float resolvedProgress = accessor.getFloat("progress", progress);
        if (CreateWorldLoadingTracker.isActive()) {
            if (resolvedProgress >= 0.0F) {
                lastTrackerVisibleProgress = resolvedProgress;
            } else if (lastTrackerVisibleProgress >= 0.0F) {
                resolvedProgress = lastTrackerVisibleProgress;
            }
        } else {
            lastTrackerVisibleProgress = resolvedProgress >= 0.0F ? resolvedProgress : -1.0F;
        }
        this.progress = resolvedProgress;
        this.genericLoading = accessor.getBoolean("genericLoading", genericLoading);
    }

    public void prepareRender(Minecraft minecraft, UIAccessor accessor) {
        prepareRender(minecraft, accessor, loadingHeader, loadingStage, progress, genericLoading);
    }

    public void renderBackground(GuiGraphics guiGraphics, int i, int j, float f) {
        LegacyRenderUtil.renderDefaultBackground(accessor, guiGraphics, true, true, false);
    }

    public void renderForeground(GuiGraphics guiGraphics, int i, int j, float f) {
        int width = guiGraphics.guiWidth();
        int height = guiGraphics.guiHeight();
        ArbitrarySupplier<ResourceLocation> fontOverride = accessor.getElement("fontOverride", ResourceLocation.class);
        int steppedProgress = progress < 0 ? -1 : Math.max(0, Math.min(100, Math.round(progress * 100.0f)));

        if (!genericLoading) {
            if (steppedProgress >= 0) {
                int loadingBarX = accessor.getInteger("loadingBar.x", width / 2 - 160);
                int loadingBarY = accessor.getInteger("loadingBar.y", height / 2 + 15);
                if (loadingStage != null) {
                    LegacyFontUtil.applySmallerFont(fontOverride.map(FontDescription.Resource::new).orElse(FontDescription.DEFAULT), b -> {
                        int stageWidth = minecraft.font.width(loadingStage);
                        int stageX = accessor.getInteger("loadingStage.x", loadingBarX + (320 - stageWidth) / 2);
                        int stageY = accessor.getInteger("loadingStage.y", loadingBarY - 10);
                        guiGraphics.drawString(minecraft.font, loadingStage, stageX, stageY, CommonColor.STAGE_TEXT.get());
                    });
                }
                try (SpriteContents contents = FactoryGuiGraphics.getSprites().getSprite(LOADING_BACKGROUND).contents()) {
                    FactoryGuiGraphics.of(guiGraphics).blitSprite(LOADING_BACKGROUND, loadingBarX, loadingBarY, 320, 320 * contents.height() / contents.width());
                }
                if (steppedProgress > 0) {
                    try (SpriteContents contents = FactoryGuiGraphics.getSprites().getSprite(LOADING_BAR).contents()) {
                        int fillWidth = Math.min(318, Math.round(318.0f * steppedProgress / 100.0f));
                        FactoryGuiGraphics.of(guiGraphics).blitSprite(LOADING_BAR, 318, 318 * contents.height() / contents.width(), 0, 0, loadingBarX + 1, loadingBarY + 1, 0, fillWidth, 318 * contents.height() / contents.width());
                    }
                }
                LegacyTip tip = Legacy4JClient.legacyTipManager.getLoadingTip();
                if (tip != null) {
                    tip.setX(accessor.getInteger("loadingTip.x", (width - tip.width) / 2));
                    tip.setY(accessor.getInteger("loadingTip.y", loadingBarY + 10 + ((height - (loadingBarY + 10)) - tip.height) / 2));
                    tip.render(guiGraphics, i, j, f);
                }
            }
        } else LegacyRenderUtil.drawGenericLoading(guiGraphics, (width - 75) / 2, height / 2);

        if (loadingHeader != null) {
            LegacyFontUtil.applySmallerFont(fontOverride.map(FontDescription.Resource::new).orElse(FontDescription.DEFAULT), b -> {
                guiGraphics.pose().pushMatrix();
                float scaleX = accessor.getFloat("loadingHeader.scaleX", 2.0f);
                guiGraphics.pose().translate(accessor.getFloat("loadingHeader.x", (width - minecraft.font.width(loadingHeader) * scaleX) / 2), accessor.getFloat("loadingHeader.y", height / 2 - 23));
                guiGraphics.pose().scale(scaleX, accessor.getFloat("loadingHeader.scaleY", 2.0f));
                LegacyRenderUtil.drawOutlinedString(guiGraphics, minecraft.font, loadingHeader, 0, 0, CommonColor.TITLE_TEXT.get(), CommonColor.TITLE_TEXT_OUTLINE.get(), accessor.getFloat("loadingHeader.outline", 0.5f));
                guiGraphics.pose().popMatrix();
            });
        }
    }

    @Override
    public void render(GuiGraphics guiGraphics, int i, int j, float f) {
        renderBackground(guiGraphics, i, j, f);
        renderForeground(guiGraphics, i, j, f);
    }
}
