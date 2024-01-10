package wily.legacy.client.screen;

import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.components.AbstractButton;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.resources.language.LanguageManager;
import net.minecraft.network.chat.Component;
import wily.legacy.util.ScreenUtil;

public class LegacyLanguageScreen extends PanelVListScreen{
    public static final Component WARNING_LABEL = Component.translatable("options.languageAccuracyWarning");
    protected String selectedLang;
    public LegacyLanguageScreen(Screen parent, LanguageManager manager) {
        super(parent, 290, 240, Component.translatable("controls.keybinds.title"));
        renderableVList.layoutSpacing(l->0);
        manager.getLanguages().forEach(((s, languageInfo) -> {
            AbstractButton langButton;
            renderableVList.addRenderable(langButton = new AbstractButton(0,0,260,20,languageInfo.toComponent()) {
                @Override
                public void onPress() {
                    selectedLang = s;
                }
                @Override
                protected void updateWidgetNarration(NarrationElementOutput narrationElementOutput) {
                    defaultButtonNarrationText(narrationElementOutput);
                }
            });
            if (manager.getSelected().equals(s)) setFocused(langButton);
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
        addRenderableOnly(((guiGraphics, i, j, f) -> ScreenUtil.renderPanelRecess(guiGraphics,panel.x + 6, panel.y + 24, panel.width - 12, panel.height - 34,2f)));
        getRenderableVList().init(this,panel.x + 10,panel.y + 30,panel.width - 20,panel.height - 30);
        addRenderableWidget(minecraft.options.forceUnicodeFont().createButton(minecraft.options,panel.x + 10, panel.y + 10, panel.width - 20));
    }
}
