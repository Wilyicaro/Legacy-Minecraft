package wily.legacy.mixin.base.client.title;

import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import com.llamalad7.mixinextras.injector.v2.WrapWithCondition;
import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.LogoRenderer;
import net.minecraft.client.gui.components.SplashRenderer;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.TitleScreen;
import net.minecraft.client.renderer.PanoramaRenderer;
import net.minecraft.network.chat.Component;
//? if forge {
/*import net.minecraftforge.client.gui.TitleScreenModUpdateIndicator;
*///?} else if neoforge && <=1.20.4 {
/*import net.neoforged.neoforge.client.gui.TitleScreenModUpdateIndicator;
*///?}
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import wily.legacy.Legacy4J;
import wily.legacy.Legacy4JClient;
import wily.legacy.client.ControlType;
import wily.legacy.client.LegacyOptions;
import wily.legacy.client.controller.ControllerBinding;
import wily.legacy.client.screen.*;
import wily.legacy.client.screen.compat.WorldHostFriendsScreen;
import wily.legacy.util.ScreenUtil;

import java.io.IOException;
import java.util.function.BiConsumer;
import java.util.function.ObjIntConsumer;

@Mixin(TitleScreen.class)
public abstract class TitleScreenMixin extends Screen implements ControlTooltip.Event,RenderableVList.Access{
    @Shadow @Nullable private SplashRenderer splash;
    //? if forge || neoforge && <=1.20.4 {
    /*@Shadow private TitleScreenModUpdateIndicator modUpdateNotification;
    *///?}
    @Unique
    private RenderableVList renderableVList = new RenderableVList(this).layoutSpacing(l->5);

    protected TitleScreenMixin(Component component) {
        super(component);
    }

    @Inject(method = "<init>(ZLnet/minecraft/client/gui/components/LogoRenderer;)V", at = @At("RETURN"))
    public void init(boolean bl, LogoRenderer logoRenderer, CallbackInfo ci) {
        renderableVList.addRenderable(Button.builder(Component.translatable("legacy.menu.play_game"), (button) -> {
            if (minecraft.isDemo()){
                try {
                    LoadSaveScreen.loadWorld(this,minecraft,Legacy4JClient.getLevelStorageSource(),Legacy4JClient.importSaveFile(minecraft.getResourceManager().getResourceOrThrow(Legacy4J.createModLocation("tutorial/tutorial.mcsave")).open(), Legacy4JClient.getLevelStorageSource(),"Tutorial"));
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
            }else minecraft.setScreen(PlayGameScreen.createAndCheckNewerVersions(this));
        }).build());
        Button modButton;
        renderableVList.addRenderable(modButton = Button.builder(Component.translatable("legacy.menu.mods"), b -> minecraft.setScreen(new ModsScreen(this))).build());
        renderableVList.addRenderable(Button.builder(Component.translatable("options.language"), b -> minecraft.setScreen(new LegacyLanguageScreen(this, this.minecraft.getLanguageManager()))).build());
        renderableVList.addRenderable(Button.builder(Component.translatable("menu.options"), b -> minecraft.setScreen(new HelpAndOptionsScreen(this))).build());
        renderableVList.addRenderable(Button.builder(Component.translatable("menu.quit"), (button) -> minecraft.setScreen(new ExitConfirmationScreen(this))).build());
        //? if forge || neoforge && <=1.20.4 {
        /*this.modUpdateNotification = TitleScreenModUpdateIndicator.init((TitleScreen) (Object) this, modButton);
        *///?}
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
        getRenderableVList().init(width / 2 - 112,this.height / 3 + 10,225,0);
    }

    @Inject(method = "added", at = @At("RETURN"))
    public void added(CallbackInfo ci) {
        ControlTooltip.Renderer.of(this).add(()-> ControlType.getActiveType().isKbm() ? ControlTooltip.getKeyIcon(InputConstants.KEY_X) : ControllerBinding.LEFT_BUTTON.getIcon(),()-> ChooseUserScreen.CHOOSE_USER);
        if (PublishScreen.hasWorldHost()) ControlTooltip.Renderer.of(this).add(()-> ControlType.getActiveType().isKbm() ? ControlTooltip.getKeyIcon(InputConstants.KEY_O) : ControllerBinding.UP_BUTTON.getIcon(), ()-> WorldHostFriendsScreen.FRIENDS);
        if (splash == null) this.splash = Minecraft.getInstance().getSplashManager().getSplash();
    }

    @Inject(method = "removed", at = @At("RETURN"))
    public void removed(CallbackInfo ci) {
        splash = null;
    }

    @Override
    public boolean keyPressed(int i, int j, int k) {
        if (renderableVList.keyPressed(i)) return true;
        if (i == InputConstants.KEY_X){
            minecraft.setScreen(new ChooseUserScreen(this));
            return true;
        }
        if (i == InputConstants.KEY_O && PublishScreen.hasWorldHost()) {
            minecraft.setScreen(new WorldHostFriendsScreen(this));
            return true;
        }
        if (Legacy4JClient.keyLegacy4JSettings.matches(i,j)) {
            minecraft.setScreen(new Legacy4JSettingsScreen(this));
            return true;
        }
        return super.keyPressed(i, j, k);
    }

    //? if forge || neoforge {
    /*@WrapWithCondition(method = "render", at = @At(value = "INVOKE", target = /^? if neoforge {^//^"Lnet/neoforged/neoforge/internal/BrandingControl;forEachLine(ZZLjava/util/function/BiConsumer;)V"^//^?} else if <1.20.5 {^/"Lnet/minecraftforge/internal/BrandingControl;forEachLine(ZZLjava/util/function/BiConsumer;)V"/^?} else {^//^"Lnet/minecraftforge/internal/BrandingControl;forEachLine(ZZLjava/util/function/ObjIntConsumer;)V"^//^?}^/, remap = false))
    public boolean wrapVersionText(boolean includeMC, boolean reverse, /^? if forge && >=1.20.5 {^//^ObjIntConsumer<String>^//^?} else {^/BiConsumer<Integer, String>/^?}^/ lineConsumerr) {
        return LegacyOptions.titleScreenVersionText.get();
    }
    @WrapWithCondition(method = "render", at = @At(value = "INVOKE", target = /^? if neoforge {^//^"Lnet/neoforged/neoforge/internal/BrandingControl;forEachAboveCopyrightLine(Ljava/util/function/BiConsumer;)V"^//^?} else if <1.20.5 {^/"Lnet/minecraftforge/internal/BrandingControl;forEachAboveCopyrightLine(Ljava/util/function/BiConsumer;)V"/^?} else {^//^"Lnet/minecraftforge/internal/BrandingControl;forEachAboveCopyrightLine(Ljava/util/function/ObjIntConsumer;)V"^//^?}^/, remap = false))
    public boolean wrapBrandingOverCopyright(/^? if forge && >=1.20.5 {^//^ObjIntConsumer<String>^//^?} else {^/BiConsumer<Integer, String>/^?}^/ lineConsumer) {
        return LegacyOptions.titleScreenVersionText.get();
    }
    *///?} else {
    @WrapWithCondition(method = "render", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/GuiGraphics;drawString(Lnet/minecraft/client/gui/Font;Ljava/lang/String;III)I"))
    public boolean wrapVersionText(GuiGraphics instance, Font font, String string, int i, int j, int k) {
        return LegacyOptions.titleScreenVersionText.get();
    }
    //?}

    @ModifyReturnValue(method = "realmsNotificationsEnabled", at = @At("RETURN"))
    public boolean realmsNotificationsEnabled(boolean original) {
        return false;
    }

    //? if <1.20.5 {
    /*@WrapWithCondition(method = "render", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/renderer/PanoramaRenderer;render(FF)V"))
    public boolean render(PanoramaRenderer instance, float partialTick, float speed, GuiGraphics guiGraphics) {
        ScreenUtil.renderPanorama(guiGraphics, speed, partialTick);
        return false;
    }
    *///?}
}
