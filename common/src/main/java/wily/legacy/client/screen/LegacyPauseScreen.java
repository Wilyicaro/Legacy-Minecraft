package wily.legacy.client.screen;

import com.mojang.realmsclient.RealmsMainScreen;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.GenericDirtMessageScreen;
import net.minecraft.client.gui.screens.PauseScreen;
import net.minecraft.client.gui.screens.TitleScreen;
import net.minecraft.client.gui.screens.achievement.StatsScreen;
import net.minecraft.client.gui.screens.advancements.AdvancementsScreen;
import net.minecraft.client.gui.screens.multiplayer.JoinMultiplayerScreen;
import net.minecraft.client.multiplayer.ServerData;
import net.minecraft.network.chat.Component;
import wily.legacy.util.ScreenUtil;

public class LegacyPauseScreen extends PauseScreen {
    protected final RenderableVList renderableVList = new RenderableVList().layoutSpacing(l->5);
    protected boolean updateAutoSaveIndicator;
    public LegacyPauseScreen(boolean bl) {
        super(bl);
        renderableVList.addRenderables(Button.builder(Component.translatable("menu.returnToGame"), button -> {
            this.minecraft.setScreen(null);
            this.minecraft.mouseHandler.grabMouse();
        }).build(),Button.builder(Component.translatable("menu.options"), button -> this.minecraft.setScreen(new HelpOptionsScreen(this))).build()
                ,Button.builder(Component.translatable("gui.stats"), button -> this.minecraft.setScreen(new StatsScreen(this, this.minecraft.player.getStats()))).build()
                ,Button.builder(Component.translatable("gui.advancements"), button -> this.minecraft.setScreen(new AdvancementsScreen(this.minecraft.getConnection().getAdvancements()))).build()
        );
        minecraft = Minecraft.getInstance();
        if (minecraft.level != null && minecraft.hasSingleplayerServer())
            renderableVList.addRenderable(Button.builder(Component.translatable("legacy.menu.save"), button -> {
                minecraft.getSingleplayerServer().saveEverything(false,false,false);
                updateAutoSaveIndicator = true;
            }).build());
        renderableVList.addRenderable(Button.builder(Component.translatable("menu.quit"), button -> minecraft.setScreen(new ExitConfirmationScreen(this))).build());
    }


    @Override
    public void renderBackground(GuiGraphics guiGraphics, int i, int j, float f) {
        ScreenUtil.renderDefaultBackground(guiGraphics);
    }

    @Override
    public void render(GuiGraphics guiGraphics, int i, int j, float f) {
        super.render(guiGraphics, i, j, f);
        if (updateAutoSaveIndicator){
            updateAutoSaveIndicator = false;
            minecraft.gui.autosaveIndicatorValue = 1.0F;
        }
        if (minecraft.gui.autosaveIndicatorValue > 0.002){
            minecraft.gui.renderSavingIndicator(guiGraphics);
        }
    }

    @Override
    protected void init() {
        renderableVList.init(this,width / 2 - 112,this.height / 3 + 10,225,0);
    }
}
