package wily.legacy.mixin.base.client.pause;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.PauseScreen;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.network.chat.Component;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import wily.factoryapi.base.client.UIAccessor;
import wily.legacy.client.LegacyOptions;
import wily.legacy.client.LegacySaveCache;
import wily.legacy.client.screen.*;
import wily.legacy.util.LegacyComponents;
import wily.legacy.util.client.LegacyRenderUtil;

import java.util.Collections;
import java.util.List;

@Mixin(PauseScreen.class)
public class PauseScreenMixin extends Screen implements ControlTooltip.Event, RenderableVList.Access {
    @Unique
    protected RenderableVList renderableVList;
    @Unique
    private List<RenderableVList> renderableVLists;
    private Button leaderboardsButton;
    private Button saveButton;
    protected PauseScreenMixin(Component component) {
        super(component);
    }

    @Override
    public boolean keyPressed(KeyEvent keyEvent) {
        if (renderableVList.keyPressed(keyEvent.key())) return true;
        return super.keyPressed(keyEvent);
    }

    @Unique
    private void setAutoSave(int autoSave, Button button) {
        LegacyOptions.autoSaveInterval.set(autoSave);
        LegacyOptions.autoSaveInterval.save();
        button.setMessage(LegacyOptions.autoSaveInterval.get() > 0 ? LegacyComponents.DISABLE_AUTO_SAVE : LegacyComponents.SAVE_GAME);
    }

    @Inject(method = "<init>", at = @At("RETURN"))
    private void initScreen(CallbackInfo ci) {
        renderableVList = new RenderableVList(this).layoutSpacing(l -> LegacyOptions.getUIMode().isSD() ? 4 : 5);
        renderableVLists = Collections.singletonList(renderableVList);
        renderableVList.addRenderables(
                Button.builder(Component.translatable("menu.returnToGame"), button -> {
                    this.minecraft.setScreen(null);
                    this.minecraft.mouseHandler.grabMouse();
                }).build(),
                Button.builder(Component.translatable("menu.options"), button -> this.minecraft.setScreen(new HelpAndOptionsScreen(this))).build(),
                leaderboardsButton = Button.builder(Component.empty(), button -> this.minecraft.setScreen(LeaderboardsScreen.getActualLeaderboardsScreenInstance(this))).build(),
                Button.builder(Component.translatable("gui.advancements"), button -> this.minecraft.setScreen(LegacyAdvancementsScreen.getActualAdvancementsScreenInstance(this))).build()
        );
        minecraft = Minecraft.getInstance();
        if (LegacySaveCache.hasSaveSystem(minecraft))
            renderableVList.addRenderable(saveButton = Button.builder(LegacyOptions.autoSaveInterval.get() > 0 ? LegacyComponents.DISABLE_AUTO_SAVE : LegacyComponents.SAVE_GAME, button -> minecraft.setScreen(new ConfirmationScreen(this, LegacyOptions.autoSaveInterval.get() > 0 ? LegacyComponents.DISABLE_AUTO_SAVE : LegacyComponents.SAVE_GAME, LegacyOptions.autoSaveInterval.get() > 0 ? LegacyComponents.DISABLE_AUTO_SAVE_MESSAGE : LegacyComponents.SAVE_GAME_MESSAGE, b -> {
                if (LegacyOptions.autoSaveInterval.get() > 0) {
                    setAutoSave(0, button);
                    minecraft.setScreen(PauseScreenMixin.this);
                } else {
                    LegacySaveCache.manualSave = LegacySaveCache.retakeWorldIcon = true;
                    minecraft.setScreen(new ConfirmationScreen(PauseScreenMixin.this, LegacyComponents.ENABLE_AUTO_SAVE, LegacyComponents.ENABLE_AUTO_SAVE_MESSAGE, b1 -> {
                        setAutoSave(1, button);
                        minecraft.setScreen(PauseScreenMixin.this);
                    }));
                }
            }))).build());
        renderableVList.addRenderable(Button.builder(Component.translatable("menu.quit"), button -> minecraft.setScreen(new ExitConfirmationScreen(this))).build());
    }

    //? if >1.20.1 {
    @Inject(method = "renderBackground", at = @At("HEAD"), cancellable = true)
    public void renderBackground(GuiGraphics guiGraphics, int i, int j, float f, CallbackInfo ci) {
        ci.cancel();
        LegacyRenderUtil.renderDefaultBackground(UIAccessor.of(this), guiGraphics);
    }
    //?}

    @Inject(method = "render", at = @At("HEAD"), cancellable = true)
    public void render(GuiGraphics guiGraphics, int i, int j, float f, CallbackInfo ci) {
        ci.cancel();
        //? if <=1.20.1
        /*ScreenUtil.renderDefaultBackground(UIAccessor.of(this), guiGraphics);*/
        super.render(guiGraphics, i, j, f);
    }

    @Inject(method = "init", at = @At("HEAD"), cancellable = true)
    public void init(CallbackInfo ci) {
        ci.cancel();
        renderableVListInit();
        if (saveButton != null) {
            if (minecraft.isDemo()) saveButton.active = false;
            else if (LegacySaveCache.hasSaveSystem(minecraft))
                setAutoSave(LegacyOptions.autoSaveInterval.get(), saveButton);
        }

        if (leaderboardsButton != null)
            leaderboardsButton.setMessage(LegacyOptions.legacyLeaderboards.get() ? Component.translatable("legacy.menu.leaderboards") : Component.translatable("gui.stats"));
    }

    @Override
    public void renderableVListInit() {
        initRenderableVListHeight(20);
        renderableVList.init(width / 2 - 112, this.height / 3 + 5, 225, 0);
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
