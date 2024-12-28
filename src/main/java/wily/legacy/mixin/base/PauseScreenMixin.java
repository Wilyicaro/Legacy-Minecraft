package wily.legacy.mixin.base;

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
import wily.factoryapi.base.client.UIDefinition;
import wily.legacy.Legacy4JClient;
import wily.legacy.client.LegacyOption;
import wily.legacy.client.screen.*;
import wily.legacy.util.LegacyComponents;
import wily.legacy.util.ScreenUtil;

import java.util.Collections;
import java.util.List;

@Mixin(PauseScreen.class)
public class PauseScreenMixin extends Screen implements ControlTooltip.Event,RenderableVList.Access{
    protected PauseScreenMixin(Component component) {
        super(component);
    }
    protected RenderableVList renderableVList;
    private List<RenderableVList> renderableVLists;
    @Override
    public boolean keyPressed(int i, int j, int k) {
        if (renderableVList.keyPressed(i)) return true;
        return super.keyPressed(i, j, k);
    }
    @Unique
    private void setAutoSave(int autoSave, Button button){
        LegacyOption.autoSaveInterval.set(autoSave);
        minecraft.options.save();
        button.setMessage(LegacyOption.autoSaveInterval.get() > 0 ? LegacyComponents.DISABLE_AUTO_SAVE : LegacyComponents.SAVE_GAME);
    }

    @Inject(method = "<init>", at = @At("RETURN"))
    private void initScreen(CallbackInfo ci){
        renderableVList = new RenderableVList(this).layoutSpacing(l->5);
        renderableVLists = Collections.singletonList(renderableVList);
        renderableVList.addRenderables(Button.builder(Component.translatable("menu.returnToGame"), button -> {
                    this.minecraft.setScreen(null);
                    this.minecraft.mouseHandler.grabMouse();
                }).build(),Button.builder(Component.translatable("menu.options"), button -> this.minecraft.setScreen(new HelpAndOptionsScreen(this))).build()
                ,Button.builder(Component.translatable("legacy.menu.leaderboards"), button -> this.minecraft.setScreen(new LeaderboardsScreen(this))).build()
                ,Button.builder(Component.translatable("gui.advancements"), button -> this.minecraft.setScreen(new LegacyAdvancementsScreen(this))).build()
        );
        minecraft = Minecraft.getInstance();
        if (minecraft.level != null && minecraft.hasSingleplayerServer())
            renderableVList.addRenderable(Button.builder(LegacyOption.autoSaveInterval.get() > 0 && !minecraft.isDemo() ? LegacyComponents.DISABLE_AUTO_SAVE : LegacyComponents.SAVE_GAME, button -> minecraft.setScreen(new ConfirmationScreen(this,LegacyOption.autoSaveInterval.get() > 0 ? LegacyComponents.DISABLE_AUTO_SAVE : LegacyComponents.SAVE_GAME,LegacyOption.autoSaveInterval.get() > 0 ? LegacyComponents.DISABLE_AUTO_SAVE_MESSAGE : LegacyComponents.SAVE_GAME_MESSAGE, b->{
                if (LegacyOption.autoSaveInterval.get() > 0){
                    setAutoSave(0,button);
                    minecraft.setScreen(PauseScreenMixin.this);
                }else{
                    Legacy4JClient.manualSave = Legacy4JClient.retakeWorldIcon = true;
                    minecraft.setScreen(new ConfirmationScreen(PauseScreenMixin.this,LegacyComponents.ENABLE_AUTO_SAVE,LegacyComponents.ENABLE_AUTO_SAVE_MESSAGE,b1-> {
                        setAutoSave(1,button);
                        minecraft.setScreen(PauseScreenMixin.this);
                    }));
                }
            }))).build());
        renderableVList.addRenderable(Button.builder(Component.translatable("menu.quit"), button -> minecraft.setScreen(new ExitConfirmationScreen(this))).build());
    }

    //? if >1.20.1 {
    @Inject(method = "renderBackground",at = @At("HEAD"), cancellable = true)
    public void renderBackground(GuiGraphics guiGraphics, int i, int j, float f, CallbackInfo ci) {
        ci.cancel();
        ScreenUtil.renderDefaultBackground(UIDefinition.Accessor.of(this), guiGraphics);
    }
    //?}

    @Inject(method = "render",at = @At("HEAD"), cancellable = true)
    public void render(GuiGraphics guiGraphics, int i, int j, float f, CallbackInfo ci) {
        ci.cancel();
        //? if <=1.20.1
        /*ScreenUtil.renderDefaultBackground(UIDefinition.Accessor.of(this), guiGraphics);*/
        super.render(guiGraphics, i, j, f);
    }

    @Inject(method = "init",at = @At("HEAD"), cancellable = true)
    public void init(CallbackInfo ci) {
        ci.cancel();
        renderableVListInit();
        if (minecraft.isDemo()) ((Button)renderableVList.renderables.get(4)).active = false;
        else if (minecraft.level != null && minecraft.hasSingleplayerServer()) setAutoSave(LegacyOption.autoSaveInterval.get(),(Button)renderableVList.renderables.get(4));

    }

    @Override
    public void renderableVListInit() {
        renderableVList.init(width / 2 - 112,this.height / 3 + 10,225,0);
    }

    @Override
    public RenderableVList getRenderableVList() {
        return renderableVList;
    }

    @Override
    public List<RenderableVList> getRenderableVLists() {
        return renderableVLists;
    }
}
