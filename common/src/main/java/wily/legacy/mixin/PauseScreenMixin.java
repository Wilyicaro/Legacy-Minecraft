package wily.legacy.mixin;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.PauseScreen;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.achievement.StatsScreen;
import net.minecraft.client.gui.screens.advancements.AdvancementsScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import wily.legacy.Legacy4JClient;
import wily.legacy.client.screen.*;
import wily.legacy.util.ScreenUtil;

@Mixin(PauseScreen.class)
public class PauseScreenMixin extends Screen {
    private Component SAVE_GAME = Component.translatable("legacy.menu.save");
    private Component SAVE_GAME_MESSAGE = Component.translatable("legacy.menu.save_message");
    private Component ENABLE_AUTO_SAVE = Component.translatable("legacy.menu.enable_autosave");
    private Component ENABLE_AUTO_SAVE_MESSAGE = Component.translatable("legacy.menu.enable_autosave_message");
    private Component DISABLE_AUTO_SAVE = Component.translatable("legacy.menu.disable_autosave");
    private Component DISABLE_AUTO_SAVE_MESSAGE = Component.translatable("legacy.menu.disable_autosave_message");
    protected PauseScreenMixin(Component component) {
        super(component);
    }
    protected RenderableVList renderableVList;
    protected boolean updateAutoSaveIndicator;
    private boolean inited = false;
    private ControlTooltip.Renderer controlTooltipRenderer = ControlTooltip.defaultScreen(this);

    @Override
    public boolean keyPressed(int i, int j, int k) {
        if (renderableVList.keyPressed(i,true)) return true;
        return super.keyPressed(i, j, k);
    }
    public void setAutoSave(boolean autoSave, Button button){
        ScreenUtil.getLegacyOptions().autoSave().set(autoSave);
        minecraft.options.save();
        minecraft.setScreen(PauseScreenMixin.this);
        button.setMessage(ScreenUtil.getLegacyOptions().autoSave().get() ? DISABLE_AUTO_SAVE : SAVE_GAME);
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
            renderableVList.addRenderable(Button.builder(ScreenUtil.getLegacyOptions().autoSave().get() ? DISABLE_AUTO_SAVE : SAVE_GAME, button -> minecraft.setScreen(new ConfirmationScreen(this,ScreenUtil.getLegacyOptions().autoSave().get() ? DISABLE_AUTO_SAVE: SAVE_GAME,ScreenUtil.getLegacyOptions().autoSave().get() ? DISABLE_AUTO_SAVE_MESSAGE : SAVE_GAME_MESSAGE, b->{
                if (ScreenUtil.getLegacyOptions().autoSave().get()){
                  setAutoSave(false,button);
                }else{
                    Legacy4JClient.manualSave = Legacy4JClient.retakeWorldIcon = updateAutoSaveIndicator = true;
                    minecraft.setScreen(new ConfirmationScreen(PauseScreenMixin.this,ENABLE_AUTO_SAVE,ENABLE_AUTO_SAVE_MESSAGE,b1-> {
                        setAutoSave(true,button);
                    }));
                }
            }))).build());
        renderableVList.addRenderable(Button.builder(Component.translatable("menu.quit"), button -> minecraft.setScreen(new ExitConfirmationScreen(this))).build());
    }
    public void renderBackground(GuiGraphics guiGraphics, int i, int j, float f) {
        ScreenUtil.renderDefaultBackground(guiGraphics);
        controlTooltipRenderer.render(guiGraphics, i, j, f);
    }

    @Override
    public void render(GuiGraphics guiGraphics, int i, int j, float f) {
        super.render(guiGraphics, i, j, f);
        if (!inited){
            ScreenUtil.playSimpleUISound(SoundEvents.UI_BUTTON_CLICK.value(),1.0f);
            inited = true;
        }
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
