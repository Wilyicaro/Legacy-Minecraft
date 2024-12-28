package wily.legacy.client.screen;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractButton;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.resources.language.LanguageManager;
import net.minecraft.network.chat.Component;
import wily.factoryapi.base.client.FactoryGuiGraphics;
import wily.legacy.util.LegacySprites;

import java.util.Locale;

public class LegacyLanguageScreen extends PanelVListScreen {
    public static final Component WARNING_LABEL = Component.translatable("options.languageAccuracyWarning");
    protected String selectedLang;
    public LegacyLanguageScreen(Screen parent, LanguageManager manager) {
        super(parent, s->Panel.centered( s,255, 240,0,24), Component.translatable("controls.keybinds.title"));
        String autoCode = getSystemLanguageCode();
        renderableVList.addRenderable(new AbstractButton(0,0,260,20,Component.translatable("legacy.menu.system_language")) {
            @Override
            protected void renderWidget(GuiGraphics guiGraphics, int i, int j, float f) {
                super.renderWidget(guiGraphics, i, j, f);
                if (manager.getSelected().equals(autoCode)) setFocused(true);
            }

            @Override
            public void onPress() {
                selectedLang = autoCode;
            }
            @Override
            protected void updateWidgetNarration(NarrationElementOutput narrationElementOutput) {
                defaultButtonNarrationText(narrationElementOutput);
            }
        });
        manager.getLanguages().forEach(((s, languageInfo) -> renderableVList.addRenderable(new AbstractButton(0,0,260,20,languageInfo.toComponent()) {
            @Override
            protected void renderWidget(GuiGraphics guiGraphics, int i, int j, float f) {
                super.renderWidget(guiGraphics, i, j, f);
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
        })));
    }
    public static String getSystemLanguageCode(){
        String auto = Locale.getDefault().toString().toLowerCase(Locale.ENGLISH);
        return Minecraft.getInstance().getLanguageManager().getLanguages().containsKey(auto) ? auto : "en_us";
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
        addRenderableOnly(((guiGraphics, i, j, f) -> FactoryGuiGraphics.of(guiGraphics).blitSprite(LegacySprites.PANEL_RECESS,panel.x + 6, panel.y + 24, panel.width - 12, panel.height - 34)));
        getRenderableVList().init(panel.x + 10,panel.y + 30,panel.width - 20,panel.height - 46);
        addRenderableWidget(minecraft.options.forceUnicodeFont().createButton(minecraft.options,panel.x + 10, panel.y + 10, panel.width - 20));
    }
}
