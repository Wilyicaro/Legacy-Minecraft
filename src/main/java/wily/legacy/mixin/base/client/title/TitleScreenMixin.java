package wily.legacy.mixin.base.client.title;

import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import com.llamalad7.mixinextras.injector.v2.WrapWithCondition;
import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.LogoRenderer;
import net.minecraft.client.gui.components.Renderable;
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
import wily.factoryapi.base.client.UIAccessor;
import wily.factoryapi.base.client.WidgetAccessor;
import wily.legacy.Legacy4J;
import wily.legacy.Legacy4JClient;
import wily.legacy.client.ContentManager;
import wily.legacy.client.ControlType;
import wily.legacy.client.LegacyOptions;
import wily.legacy.client.controller.ControllerBinding;
import wily.legacy.client.screen.*;
import wily.legacy.client.screen.compat.WorldHostFriendsScreen;
import wily.legacy.client.screen.globalleaderboards.GlobalLeaderboardsFeature;
import wily.legacy.util.LegacyComponents;
import wily.legacy.util.ScreenUtil;

import java.io.IOException;
import java.util.function.BiConsumer;
import java.util.function.ObjIntConsumer;

@Mixin(TitleScreen.class)
public abstract class TitleScreenMixin extends Screen implements ControlTooltip.Event,RenderableVList.Access{
    @Shadow @Nullable private SplashRenderer splash;
    //? if forge || neoforge && <=1.20.4 {
    /*@Shadow(remap = false) private TitleScreenModUpdateIndicator modUpdateNotification;
    *///?}
    @Unique
    private RenderableVList renderableVList = new RenderableVList(this).layoutSpacing(l -> LegacyOptions.getUIMode().isSD() ? 4 : 5);
    @Unique
    private int legacy$lastFocusedButtonIndex = -1;
    @Unique
    private String legacy$lastFocusedButtonMessage;

    protected TitleScreenMixin(Component component) {
        super(component);
    }

    @Unique
    private void rebuildMenuButtons() {
        renderableVList.renderables.clear();
        renderableVList.addRenderable(Button.builder(Component.translatable("legacy.menu.play_game"), (button) -> {
            if (minecraft.isDemo()){
                try {
                    LoadSaveScreen.loadWorld(this,minecraft,Legacy4JClient.getLevelStorageSource(),Legacy4JClient.importSaveFile(minecraft.getResourceManager().getResourceOrThrow(Legacy4J.createModLocation("tutorial/tutorial.mcsave")).open(), Legacy4JClient.getLevelStorageSource(),"Tutorial"));
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
            }else minecraft.setScreen(PlayGameScreen.createAndCheckNewerVersions(this));
        }).build());
        Button modButton = Button.builder(Component.translatable("legacy.menu.mods"), b -> minecraft.setScreen(new ModsScreen(this))).build();
        Button leaderboardsButton = Button.builder(Component.translatable("legacy.menu.leaderboards"), b -> minecraft.setScreen(LeaderboardsScreen.getOverallLeaderboardsScreenInstance(this))).build();
        if (LegacyOptions.legacySettingsMenus.get()) {
            renderableVList.addRenderable(leaderboardsButton);
        } else {
            renderableVList.addRenderable(GlobalLeaderboardsFeature.isOptedOut() ? modButton : leaderboardsButton);
            renderableVList.addRenderable(Button.builder(Component.translatable("options.language"), b -> minecraft.setScreen(new LegacyLanguageScreen(this, this.minecraft.getLanguageManager()))).build());
        }
        renderableVList.addRenderable(Button.builder(Component.translatable("menu.options"), b -> minecraft.setScreen(new HelpAndOptionsScreen(this))).build());
        renderableVList.addRenderable(Button.builder(Component.translatable("legacy.menu.store"), b -> minecraft.setScreen(new Legacy4JStoreScreen(this, ContentManager.supportedCategories()))).build());
        renderableVList.addRenderable(Button.builder(Component.translatable("menu.quit"), (button) -> minecraft.setScreen(new ExitConfirmationScreen(this))).build());
        //? if forge || neoforge && <=1.20.4 {
        /*if (!LegacyOptions.legacySettingsMenus.get() && GlobalLeaderboardsFeature.isOptedOut())
            this.modUpdateNotification = TitleScreenModUpdateIndicator.init((TitleScreen) (Object) this, modButton);
        *///?}
    }

    @Unique
    private void legacy$rememberFocusedButton() {
        if (getFocused() instanceof AbstractWidget widget) {
            legacy$lastFocusedButtonIndex = renderableVList.renderables.indexOf(widget);
            legacy$lastFocusedButtonMessage = widget.getMessage().getString();
        }
    }

    @Unique
    private void legacy$restoreFocusedButton() {
        if (legacy$lastFocusedButtonMessage != null) {
            for (Renderable renderable : renderableVList.renderables) {
                if (renderable instanceof AbstractWidget widget && legacy$lastFocusedButtonMessage.equals(widget.getMessage().getString())) {
                    renderableVList.focusRenderable(renderable);
                    return;
                }
            }
        }
        if (legacy$lastFocusedButtonIndex >= 0 && legacy$lastFocusedButtonIndex < renderableVList.renderables.size()) renderableVList.focusRenderable(renderableVList.renderables.get(legacy$lastFocusedButtonIndex));
    }

    @Inject(method = "<init>(ZLnet/minecraft/client/gui/components/LogoRenderer;)V", at = @At("RETURN"))
    public void init(boolean bl, LogoRenderer logoRenderer, CallbackInfo ci) {
        rebuildMenuButtons();
    }


    @Inject(method = "init", at = @At("HEAD"), cancellable = true)
    protected void init(CallbackInfo ci) {
        ci.cancel();
        legacy$rememberFocusedButton();
        rebuildMenuButtons();
        super.init();
        renderableVListInit();
        legacy$restoreFocusedButton();
    }

    @Override
    public RenderableVList getRenderableVList() {
        return renderableVList;
    }

    @Override
    public void initRenderableVListEntry(RenderableVList renderableVList, Renderable renderable) {
        if (renderable instanceof AbstractWidget widget) {
            //? if <=1.20.1 {
            /*((WidgetAccessor) widget).setHeight(UIAccessor.of(this).getInteger("buttonsHeight", 20));
            *///?} else {
            widget.setHeight(UIAccessor.of(this).getInteger("buttonsHeight", 20));
            //?}
        }
    }

    @Override
    public void renderableVListInit() {
        UIAccessor accessor = UIAccessor.of(this);
        int listWidth = accessor.getInteger("renderableVList.width", 225);
        getRenderableVList().init(
                accessor.getInteger("renderableVList.x", width / 2 - listWidth / 2),
                accessor.getInteger("renderableVList.y", this.height / 3 + 5),
                listWidth,
                accessor.getInteger("renderableVList.height", 0));
    }

    @Inject(method = "added", at = @At("RETURN"))
    public void added(CallbackInfo ci) {
        if (LegacyOptions.legacySettingsMenus.get())
            ControlTooltip.Renderer.of(this).add(ControlTooltip.PRESS::get, ()-> LegacyComponents.SELECT);
        else
            ControlTooltip.Renderer.of(this).add(()-> ControlType.getActiveType().isKbm() ? ControlTooltip.getKeyIcon(InputConstants.KEY_X) : ControllerBinding.LEFT_BUTTON.getIcon(),()-> ChooseUserScreen.CHOOSE_USER);
        if (PublishScreen.hasWorldHost()) ControlTooltip.Renderer.of(this).add(()-> ControlType.getActiveType().isKbm() ? ControlTooltip.getKeyIcon(InputConstants.KEY_O) : ControllerBinding.UP_BUTTON.getIcon(), ()-> WorldHostFriendsScreen.FRIENDS);
        if (splash == null) this.splash = Minecraft.getInstance().getSplashManager().getSplash();
    }

    @Inject(method = "removed", at = @At("RETURN"))
    public void removed(CallbackInfo ci) {
        legacy$rememberFocusedButton();
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

    //? if forge || neoforge && <=1.20.4 {
    /*@WrapWithCondition(method = "render", at = @At(value = "INVOKE", target = /^? if forge {^/"Lnet/minecraftforge/client/gui/TitleScreenModUpdateIndicator;render(Lnet/minecraft/client/gui/GuiGraphics;IIF)V"/^?} else {^//^"Lnet/neoforged/neoforge/client/gui/TitleScreenModUpdateIndicator;render(Lnet/minecraft/client/gui/GuiGraphics;IIF)V"^//^?}^/, remap = false))
    public boolean legacy$renderModUpdateNotification(TitleScreenModUpdateIndicator instance, GuiGraphics guiGraphics, int i, int j, float f) {
        return instance != null;
    }
    *///?}

    //? if forge || neoforge {
    /*
    @WrapWithCondition(method = "render", at = @At(value = "INVOKE", target = /^? if neoforge {^//^"Lnet/neoforged/neoforge/internal/BrandingControl;forEachLine(ZZLjava/util/function/BiConsumer;)V"^//^?} else if <1.20.5 {^//^"Lnet/minecraftforge/internal/BrandingControl;forEachLine(ZZLjava/util/function/BiConsumer;)V"^//^?} else {^/"Lnet/minecraftforge/internal/BrandingControl;forEachLine(ZZLjava/util/function/ObjIntConsumer;)V"/^?}^/, remap = false))
    public boolean wrapVersionText(boolean includeMC, boolean reverse, /^? if forge && >=1.20.5 {^//^ObjIntConsumer<String>^//^?} else {^/BiConsumer<Integer, String>/^?}^/ lineConsumerr) {
        return LegacyOptions.titleScreenVersionText.get();
    }
    @WrapWithCondition(method = "render", at = @At(value = "INVOKE", target = /^? if neoforge {^//^"Lnet/neoforged/neoforge/internal/BrandingControl;forEachAboveCopyrightLine(Ljava/util/function/BiConsumer;)V"^//^?} else if <1.20.5 {^//^"Lnet/minecraftforge/internal/BrandingControl;forEachAboveCopyrightLine(Ljava/util/function/BiConsumer;)V"^//^?} else {^/"Lnet/minecraftforge/internal/BrandingControl;forEachAboveCopyrightLine(Ljava/util/function/ObjIntConsumer;)V"/^?}^/, remap = false))
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
