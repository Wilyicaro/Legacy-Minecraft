package wily.legacy.mixin;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.PauseScreen;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import wily.legacy.Legacy4JClient;
import wily.legacy.client.screen.*;
import wily.legacy.util.ScreenUtil;

@Mixin(PauseScreen.class)
public class PauseScreenMixin extends Screen implements ControlTooltip.Event{
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
    @Override
    public boolean keyPressed(int i, int j, int k) {
        if (renderableVList.keyPressed(i,true)) return true;
        return super.keyPressed(i, j, k);
    }
    @Unique
    private void setAutoSave(int autoSave, Button button){
        ScreenUtil.getLegacyOptions().autoSaveInterval().set(autoSave);
        minecraft.options.save();
        button.setMessage(ScreenUtil.getLegacyOptions().autoSaveInterval().get() > 0 ? DISABLE_AUTO_SAVE : SAVE_GAME);
    }

    @Inject(method = "<init>", at = @At("RETURN"))
    private void initScreen(CallbackInfo ci){
        renderableVList = new RenderableVList().layoutSpacing(l->5);
        renderableVList.addRenderables(Button.builder(Component.translatable("menu.returnToGame"), button -> {
                    this.minecraft.setScreen(null);
                    this.minecraft.mouseHandler.grabMouse();
                }).build(),Button.builder(Component.translatable("menu.options"), button -> this.minecraft.setScreen(new HelpOptionsScreen(this))).build()
                ,Button.builder(Component.translatable("legacy.menu.leaderboards"), button -> this.minecraft.setScreen(new LeaderboardsScreen(this))).build()
                ,Button.builder(Component.translatable("gui.advancements"), button -> this.minecraft.setScreen(new LegacyAdvancementsScreen(this,this.minecraft.getConnection().getAdvancements()))).build()
        );
        minecraft = Minecraft.getInstance();
        if (minecraft.level != null && minecraft.hasSingleplayerServer())
            renderableVList.addRenderable(Button.builder(ScreenUtil.getLegacyOptions().autoSaveInterval().get() > 0 ? DISABLE_AUTO_SAVE : SAVE_GAME, button -> minecraft.setScreen(new ConfirmationScreen(this,ScreenUtil.getLegacyOptions().autoSaveInterval().get() > 0 ? DISABLE_AUTO_SAVE: SAVE_GAME,ScreenUtil.getLegacyOptions().autoSaveInterval().get() > 0 ? DISABLE_AUTO_SAVE_MESSAGE : SAVE_GAME_MESSAGE, b->{
                if (ScreenUtil.getLegacyOptions().autoSaveInterval().get() > 0){
                    setAutoSave(0,button);
                    minecraft.setScreen(PauseScreenMixin.this);
                }else{
                    Legacy4JClient.manualSave = Legacy4JClient.retakeWorldIcon = true;
                    minecraft.setScreen(new ConfirmationScreen(PauseScreenMixin.this,ENABLE_AUTO_SAVE,ENABLE_AUTO_SAVE_MESSAGE,b1-> {
                        setAutoSave(1,button);
                        minecraft.setScreen(PauseScreenMixin.this);
                    }));
                }
            }))).build());
        renderableVList.addRenderable(Button.builder(Component.translatable("menu.quit"), button -> minecraft.setScreen(new ExitConfirmationScreen(this))).build());
    }
    @Inject(method = "render",at = @At("HEAD"), cancellable = true)
    public void render(GuiGraphics guiGraphics, int i, int j, float f, CallbackInfo ci) {
        ci.cancel();
        ScreenUtil.renderDefaultBackground(guiGraphics);
        super.render(guiGraphics, i, j, f);
    }

    @Inject(method = "init",at = @At("HEAD"), cancellable = true)
    public void init(CallbackInfo ci) {
        ci.cancel();
        renderableVList.init(this,width / 2 - 112,this.height / 3 + 10,225,0);
        if (minecraft.level != null && minecraft.hasSingleplayerServer()) setAutoSave(ScreenUtil.getLegacyOptions().autoSaveInterval().get(),(Button)renderableVList.renderables.get(4));
    }
}
