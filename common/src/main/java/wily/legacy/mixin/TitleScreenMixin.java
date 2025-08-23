package wily.legacy.mixin;

import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.LogoRenderer;
import net.minecraft.client.gui.components.SplashRenderer;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.TitleScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import wily.legacy.Legacy4J;
import wily.legacy.Legacy4JClient;
import wily.legacy.client.ControlType;
import wily.legacy.client.controller.ControllerBinding;
import wily.legacy.client.controller.ControllerManager;
import wily.legacy.client.screen.*;
import wily.legacy.util.ScreenUtil;

import java.io.IOException;

@Mixin(TitleScreen.class)
public abstract class TitleScreenMixin extends Screen implements ControlTooltip.Event,RenderableVList.Access{
    @Shadow @Nullable private SplashRenderer splash;
    private RenderableVList renderableVList = new RenderableVList().layoutSpacing(l->5);

    protected TitleScreenMixin(Component component) {
        super(component);
    }

    @Inject(method = "<init>(ZLnet/minecraft/client/gui/components/LogoRenderer;)V", at = @At("RETURN"))
    public void init(boolean bl, LogoRenderer logoRenderer, CallbackInfo ci) {
        renderableVList.addRenderable(Button.builder(Component.translatable("legacy.menu.play_game"), (button) -> {
            if (minecraft.isDemo()){
                try {
                    LoadSaveScreen.loadWorld(this,minecraft,Legacy4JClient.currentWorldSource,Legacy4JClient.importSaveFile(minecraft.getResourceManager().getResourceOrThrow(new ResourceLocation(Legacy4J.MOD_ID,"tutorial/tutorial.mcsave")).open(), Legacy4JClient.currentWorldSource,"Tutorial"));
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
            }else minecraft.setScreen(new PlayGameScreen(this));
        }).build());
        renderableVList.addRenderable(Button.builder(Component.translatable("legacy.menu.mods"), b -> minecraft.setScreen(new ModsScreen(this))).build());
        renderableVList.addRenderable(Button.builder(Component.translatable("options.language"), b -> minecraft.setScreen(new LegacyLanguageScreen(this, this.minecraft.getLanguageManager()))).build());
        renderableVList.addRenderable(Button.builder(Component.translatable("menu.options"), b -> minecraft.setScreen(new HelpOptionsScreen(this))).build());
        renderableVList.addRenderable(Button.builder(Component.translatable("menu.quit"), (button) -> minecraft.setScreen(new ExitConfirmationScreen(this))).build());
    }


    @Inject(method = "init", at = @At("HEAD"), cancellable = true)
    protected void init(CallbackInfo ci) {
        ci.cancel();
        super.init();
        renderableVListInit();
    }

    @Override
    public RenderableVList getRenderableVList() {
        return renderableVList;
    }

    @Override
    public void renderableVListInit() {
        getRenderableVList().init(this,width / 2 - 112,this.height / 3 + 10,225,0);
    }


    @Inject(method = "added", at = @At("RETURN"))
    public void added(CallbackInfo ci) {
        ControlTooltip.Renderer.of(this).add(()-> ControlType.getActiveType().isKbm() ? ControlTooltip.getKeyIcon(InputConstants.KEY_X) : ControllerBinding.LEFT_BUTTON.bindingState.getIcon(),()->ControlTooltip.getAction("legacy.menu.choose_user"));
        this.splash = Minecraft.getInstance().getSplashManager().getSplash();
        ControllerManager.getHandler().init();
    }
    @Override
    public void renderBackground(GuiGraphics guiGraphics, int i, int j, float f) {
        ScreenUtil.renderDefaultBackground(guiGraphics,true,true, false);
    }

    @Override
    public boolean keyPressed(int i, int j, int k) {
        if (renderableVList.keyPressed(i,true)) return true;
        if (i == InputConstants.KEY_X){
            minecraft.setScreen(new ChooseUserScreen(this));
            return true;
        }
        return super.keyPressed(i, j, k);
    }

    @Inject(method = "render", at = @At("HEAD"), cancellable = true)
    public void render(GuiGraphics guiGraphics, int i, int j, float f, CallbackInfo ci) {
        ci.cancel();
        super.render(guiGraphics, i, j, f);

        if (this.splash != null) {
            Legacy4JClient.legacyFont = false;
            this.splash.render(guiGraphics, this.width, this.font, 255 << 24);
            Legacy4JClient.legacyFont = true;
        }
    }


}
