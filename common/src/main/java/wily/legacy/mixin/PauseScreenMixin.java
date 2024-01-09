package wily.legacy.mixin;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.PauseScreen;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.achievement.StatsScreen;
import net.minecraft.client.gui.screens.advancements.AdvancementsScreen;
import net.minecraft.network.chat.Component;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import wily.legacy.client.screen.ExitConfirmationScreen;
import wily.legacy.client.screen.HelpOptionsScreen;
import wily.legacy.client.screen.RenderableVList;
import wily.legacy.util.ScreenUtil;

@Mixin(PauseScreen.class)
public class PauseScreenMixin extends Screen {
    protected PauseScreenMixin(Component component) {
        super(component);
    }
    protected RenderableVList renderableVList;
    protected boolean updateAutoSaveIndicator;

    @Override
    public boolean keyPressed(int i, int j, int k) {
        if (renderableVList.keyPressed(i,j,k)) return true;
        return super.keyPressed(i, j, k);
    }

    @Inject(method = "<init>", at = @At("RETURN"))
    private void initScreen(CallbackInfo ci){
        renderableVList = new RenderableVList().layoutSpacing(l->5);
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
    public void init() {
        renderableVList.init(this,width / 2 - 112,this.height / 3 + 10,225,0);
    }
}
