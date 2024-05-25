package wily.legacy.mixin;

import com.mojang.blaze3d.platform.Window;
import net.minecraft.client.Minecraft;
import net.minecraft.client.Options;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.MultiLineLabel;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.multiplayer.ClientPacketListener;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.sounds.SoundManager;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ServerboundPlayerCommandPacket;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyArg;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import wily.legacy.client.screen.*;
import wily.legacy.util.ScreenUtil;

import java.util.List;
import java.util.function.Function;

@Mixin(Minecraft.class)
public abstract class MinecraftMixin {
    @Shadow protected abstract void updateScreenAndTick(Screen screen);

    @Shadow @Nullable public ClientLevel level;

    @Shadow public Options options;

    @Shadow public abstract void setScreen(@Nullable Screen screen);

    @Shadow @Nullable public LocalPlayer player;

    @Shadow @Final public Font font;
    @Shadow @Nullable public Screen screen;

    @Shadow public abstract Window getWindow();

    @Shadow @Nullable public abstract ClientPacketListener getConnection();

    @Inject(method = "startUseItem", at = @At("HEAD"))
    private void startUseItem(CallbackInfo ci){
        if (player != null && player.isSleeping()){
            ClientPacketListener clientPacketListener = player.connection;
            clientPacketListener.send(new ServerboundPlayerCommandPacket(player, ServerboundPlayerCommandPacket.Action.STOP_SLEEPING));
        }
    }
    @Redirect(method = "tick", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/player/LocalPlayer;isSleeping()Z"))
    private boolean tick(LocalPlayer instance){
        return false;
    }

    @ModifyArg(method = "handleKeybinds", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/Minecraft;setScreen(Lnet/minecraft/client/gui/screens/Screen;)V", ordinal = 2))
    private Screen handleKeybinds(Screen arg){
        return new LegacyAdvancementsScreen(null,getConnection().getAdvancements());
    }

    @Redirect(method = "handleKeybinds", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/Minecraft;setScreen(Lnet/minecraft/client/gui/screens/Screen;)V", ordinal = 1))
    private void handleKeybinds(Minecraft instance, Screen screen){
        if (screen instanceof ReplaceableScreen s) s.setCanReplace(false);
        setScreen(screen);
    }
    @Redirect(method = "updateScreenAndTick",at = @At(value = "INVOKE",target = "Lnet/minecraft/client/sounds/SoundManager;stop()V"))
    public void updateScreenAndTick(SoundManager instance) {

    }
    @Redirect(method = "pauseGame",at = @At(value = "INVOKE",target = "Lnet/minecraft/client/sounds/SoundManager;pause()V"))
    public void pauseGame(SoundManager instance) {

    }
    @Redirect(method = "setLevel",at = @At(value = "INVOKE",target = "Lnet/minecraft/client/Minecraft;updateScreenAndTick(Lnet/minecraft/client/gui/screens/Screen;)V"))
    public void setLevelLoadingScreen(Minecraft instance, Screen screen, ClientLevel level) {
        boolean lastOd = isOtherDimension(this.level);
        boolean od = isOtherDimension(level);
        LegacyLoadingScreen s = new LegacyLoadingScreen(od || lastOd ? Component.translatable("legacy.menu." + (lastOd ? "leaving" : "entering"), getDimensionName((lastOd ? this.level : level).dimension())) : Component.empty(), Component.empty());
        if (od || lastOd) s.genericLoading = true;
        updateScreenAndTick(s);
    }
    private Component getDimensionName(ResourceKey<Level> dimension){
        String s = dimension.location().toLanguageKey("dimension");
        return Component.translatable(ScreenUtil.hasTip(s) ? s : "dimension.minecraft");
    }
    private boolean isOtherDimension(Level level){
        return level != null && level.dimension() !=Level.OVERWORLD;
    }
    @ModifyArg(method = "resizeDisplay",at = @At(value = "INVOKE", target = "Lcom/mojang/blaze3d/platform/Window;calculateScale(IZ)I"))
    public int resizeDisplay(int j) {
        return 0;
    }

    @ModifyArg(method = "resizeDisplay",at = @At(value = "INVOKE", target = "Lcom/mojang/blaze3d/platform/Window;setGuiScale(D)V"))
    public double resizeDisplay(double d) {
        return (getWindow().getHeight() / 360d) * getTweakedHeightScale(getWindow().getHeight());
    }
    @Unique
    public double getTweakedHeightScale(int height) {
        if (ScreenUtil.getLegacyOptions().autoResolution().get()){
            if (height == 1080) return 0.999623452;
            if (height % 720 != 0) return 1.001d;
            return 1d;
        }
        return (1.125 - ScreenUtil.getLegacyOptions().interfaceResolution().get() / 4);
    }
    @Inject(method = "addInitialScreens", at = @At("HEAD"))
    private void addInitialScreens(List<Function<Runnable, Screen>> list, CallbackInfo ci) {
        list.add(r-> new ConfirmationScreen(new MainMenuScreen(),275,190,Component.empty(), MultiLineLabel.create(font,Component.translatable("legacy.menu.autoSave_message"),243),b-> true){
            protected void initButtons() {
                controlTooltipRenderer.tooltips.clear();
                messageYOffset = 68;
                transparentBackground = false;
                okButton = addRenderableWidget(Button.builder(Component.translatable("gui.ok"), b-> {if (okAction.test(b)) onClose();}).bounds(panel.x + (panel.width - 220) / 2, panel.y + panel.height - 40,220,20).build());
            }

            public void render(GuiGraphics guiGraphics, int i, int j, float f) {
                super.render(guiGraphics, i, j, f);
                ScreenUtil.drawAutoSavingIcon(guiGraphics,panel.x + 127, panel.y + 36);
            }
        });
    }
}
