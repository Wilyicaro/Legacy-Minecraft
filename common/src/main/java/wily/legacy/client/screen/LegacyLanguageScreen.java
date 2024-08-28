package wily.legacy.client.screen;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.gui.components.AbstractButton;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.resources.language.LanguageManager;
import net.minecraft.network.chat.Component;
import wily.legacy.client.LegacyGuiGraphics;
import wily.legacy.util.LegacySprites;
import wily.legacy.util.ScreenUtil;

public class LegacyLanguageScreen extends PanelVListScreen{
    public static final Component WARNING_LABEL = Component.translatable("options.languageAccuracyWarning");
    protected String selectedLang;
    public LegacyLanguageScreen(Screen parent, LanguageManager manager) {
        super(parent, 255, 240, Component.translatable("controls.keybinds.title"));
        manager.getLanguages().forEach(((s, languageInfo) -> {
            renderableVList.addRenderable(new AbstractButton(0,0,260,20,languageInfo.toComponent()) {
                @Override
                protected void renderWidget(PoseStack poseStack, int i, int j, float f) {
                    super.renderWidget(poseStack, i, j, f);
                    if (manager.getSelected().equals(s)) setFocused(true);
                }

                @Override
                public void onPress() {
                    selectedLang = s;
                }
                @Override
                protected void updateWidgetNarration(NarrationElementOutput narrationElementOutput) {
                    defaultButtonNarrationText(narrationElementOutput);
                }
            });
        }));
    }

    @Override
    public void onClose() {
        if (selectedLang != null && !minecraft.getLanguageManager().getSelected().equals(selectedLang)) {
            minecraft.getLanguageManager().setSelected(selectedLang);
            minecraft.options.languageCode = selectedLang;
            this.minecraft.reloadResourcePacks();
            minecraft.options.save();
        }
        super.onClose();
    }

    @Override
    protected void init() {
        panel.init();
        addRenderableOnly(panel);
        addRenderableOnly(((poseStack, i, j, f) -> LegacyGuiGraphics.of(poseStack).blitSprite(LegacySprites.PANEL_RECESS,panel.x + 6, panel.y + 24, panel.width - 12, panel.height - 34)));
        getRenderableVList().init(this,panel.x + 10,panel.y + 30,panel.width - 20,panel.height - 28);
        addRenderableWidget(minecraft.options.forceUnicodeFont().createButton(minecraft.options,panel.x + 10, panel.y + 10, panel.width - 20));
    }
}
