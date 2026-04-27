package wily.legacy.client.screen;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractButton;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.InputWithModifiers;
import net.minecraft.client.resources.language.LanguageManager;
import net.minecraft.network.chat.Component;
import wily.factoryapi.base.client.FactoryGuiGraphics;
import wily.legacy.client.LegacyOptions;
import wily.legacy.util.LegacySprites;

import java.util.Locale;

public class LegacyLanguageScreen extends PanelVListScreen {
    public static final Component WARNING_LABEL = Component.translatable("options.languageAccuracyWarning");
    protected final Panel panelRecess;
    private final LanguageManager manager;
    protected String selectedLang;

    public LegacyLanguageScreen(Screen parent, LanguageManager manager) {
        super(parent, s -> Panel.centered(s, 255, 240, 0, 24), Component.translatable("controls.keybinds.title"));
        this.manager = manager;
        renderableVList.addRenderable(new LanguageButton(0, 0, 260, 20, Component.translatable("legacy.menu.system_language"), getSystemLanguageCode()));
        manager.getLanguages().forEach(((s, languageInfo) -> renderableVList.addRenderable(new LanguageButton(0, 0, 260, 20, languageInfo.toComponent(), s))));
        panelRecess = Panel.createPanel(this, p -> p.appearance(LegacySprites.PANEL_RECESS, panel.width - 12, panel.height - 34), p -> p.pos(panel.x + 6, panel.y + 24));
    }

    public static String getSystemLanguageCode() {
        String auto = Locale.getDefault().toString().toLowerCase(Locale.ENGLISH);
        return Minecraft.getInstance().getLanguageManager().getLanguage(auto) != null ? auto : "en_us";
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
    protected void panelInit() {
        super.panelInit();
        panelRecess.init("panelRecess");
        addRenderableOnly(panelRecess);
        addRenderableWidget(LegacyConfigWidgets.createWidget(LegacyOptions.of(minecraft.options.forceUnicodeFont()), panel.x + 10, panel.y + 10, panel.width - 20, v -> {
        }));
    }

    @Override
    public void renderableVListInit() {
        getRenderableVList().init(panel.x + 10, panel.y + 30, panel.width - 20, panel.height - 46);
    }

    public class LanguageButton extends Button.Plain {
        public final String lang;

        protected LanguageButton(int i, int j, int k, int l, Component component, String lang) {
            super(i, j, k, l, component, b -> selectedLang = lang, DEFAULT_NARRATION);
            this.lang = lang;
        }

        @Override
        protected void renderContents(GuiGraphics guiGraphics, int i, int j, float f) {
            super.renderContents(guiGraphics, i, j, f);
            if (manager.getSelected().equals(lang)) setFocused(true);
        }
    }
}
