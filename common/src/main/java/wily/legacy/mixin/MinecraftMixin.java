package wily.legacy.mixin;

import com.mojang.blaze3d.platform.Window;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.Options;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.toasts.AdvancementToast;
import net.minecraft.client.gui.components.toasts.Toast;
import net.minecraft.client.gui.components.toasts.ToastComponent;
import net.minecraft.client.gui.screens.PauseScreen;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.TitleScreen;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.multiplayer.ClientPacketListener;
import net.minecraft.client.multiplayer.MultiPlayerGameMode;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.sounds.SoundManager;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ServerboundPlayerCommandPacket;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BedBlock;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import wily.legacy.Legacy4J;
import wily.legacy.Legacy4JClient;
import wily.legacy.client.LegacyOptions;
import wily.legacy.client.LegacyTipManager;
import wily.legacy.client.screen.*;
import wily.legacy.network.CommonNetwork;
import wily.legacy.network.ServerPlayerMissHitPacket;
import wily.legacy.util.ScreenUtil;

import java.util.List;
import java.util.function.Function;

@Mixin(Minecraft.class)
public abstract class MinecraftMixin {
    Vec3 lastPlayerBlockUsePos = null;
    @Shadow protected abstract void updateScreenAndTick(Screen screen);

    @Shadow @Nullable public ClientLevel level;

    @Shadow public Options options;

    @Shadow public abstract void setScreen(@Nullable Screen screen);

    @Shadow @Nullable public LocalPlayer player;

    @Shadow @Final public Font font;
    @Shadow @Nullable public Screen screen;

    @Shadow public abstract Window getWindow();

    @Shadow @Nullable public abstract ClientPacketListener getConnection();

    @Shadow @Nullable public HitResult hitResult;

    @Shadow @Nullable public MultiPlayerGameMode gameMode;
    private boolean inventoryKeyLastPressed = false;
    private int inventoryKeyHold = 0;

    @Shadow private int rightClickDelay;

    @Shadow public abstract ToastComponent getToasts();

    @Shadow @Final public Gui gui;

    private Minecraft self(){
        return (Minecraft)(Object)this;
    }
    @Inject(method = "close", at = @At("HEAD"))
    private void stop(CallbackInfo ci){
        Legacy4JClient.clientStopping(self());
    }
    @Inject(method = "pickBlock", at = @At("HEAD"), cancellable = true)
    private void pickBlock(CallbackInfo ci){
        if (player != null && hitResult != null && hitResult.getType() == HitResult.Type.ENTITY && !player.getAbilities().instabuild) {
            Entity entity = ((EntityHitResult) this.hitResult).getEntity();
            if (entity.getPickResult() != null) {
                int i= player.getInventory().findSlotMatchingItem(entity.getPickResult());
                if (i != -1) {
                    if (Inventory.isHotbarSlot(i)) player.getInventory().selected = i;
                    else gameMode.handlePickItem(i);
                }
            }
            ci.cancel();
        }
    }
    @Inject(method = "handleKeybinds", at = @At("HEAD"))
    private void handleKeybinds(CallbackInfo ci) {
        if (!options.keyUse.isDown()) lastPlayerBlockUsePos = null;
    }
    @Inject(method = "startAttack", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/player/LocalPlayer;resetAttackStrengthTicker()V"))
    private void startAttack(CallbackInfoReturnable<Boolean> cir) {
        CommonNetwork.sendToServer(new ServerPlayerMissHitPacket());
    }
    @Inject(method = "startUseItem", at = @At("HEAD"), cancellable = true)
    private void startUseItem(CallbackInfo ci){
        if (player != null && player.isSleeping()){
            ClientPacketListener clientPacketListener = player.connection;
            clientPacketListener.send(new ServerboundPlayerCommandPacket(player, ServerboundPlayerCommandPacket.Action.STOP_SLEEPING));
            ci.cancel();
        }
        if (lastPlayerBlockUsePos != null){
            if ((Math.abs(lastPlayerBlockUsePos.x - player.position().x) >= 1 || Math.abs(lastPlayerBlockUsePos.y - player.position().y) >= 1 || Math.abs(lastPlayerBlockUsePos.z - player.position().z) >= 1)) lastPlayerBlockUsePos = lastPlayerBlockUsePos.subtract(Mth.clamp(lastPlayerBlockUsePos.x - player.position().x,-1,1),Mth.clamp(lastPlayerBlockUsePos.y - player.position().y,-1,1), Mth.clamp(lastPlayerBlockUsePos.z - player.position().z,-1,1));
            else ci.cancel();
        }
    }
    @Inject(method = "startUseItem", at = @At("RETURN"))
    private void startUseItemReturn(CallbackInfo ci){
        if (player.getAbilities().flying && player.isSprinting()) rightClickDelay = -1;
    }
    @Redirect(method = "startUseItem", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/multiplayer/MultiPlayerGameMode;useItemOn(Lnet/minecraft/client/player/LocalPlayer;Lnet/minecraft/world/InteractionHand;Lnet/minecraft/world/phys/BlockHitResult;)Lnet/minecraft/world/InteractionResult;"))
    private InteractionResult startUseItemReturn(MultiPlayerGameMode instance, LocalPlayer localPlayer, InteractionHand arg, BlockHitResult arg2){
        if (((LegacyOptions)options).legacyCreativeBlockPlacing().get() && rightClickDelay == 4 && player.getAbilities().instabuild && ControlTooltip.canPlace(self(),arg)) {
            if (lastPlayerBlockUsePos == null) lastPlayerBlockUsePos = player.position();
            rightClickDelay = 0;
        }
        if (level.getBlockState(arg2.getBlockPos()).getBlock() instanceof BedBlock || player.getAbilities().flying && player.isSprinting()) rightClickDelay = -1;
        return instance.useItemOn(localPlayer,arg,arg2);
    }
    @Redirect(method = "tick", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/player/LocalPlayer;isSleeping()Z"))
    private boolean tick(LocalPlayer instance){
        return false;
    }
    @Redirect(method = "handleKeybinds", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/KeyMapping;consumeClick()Z", ordinal = 4))
    private boolean handleKeybindsInventoryKey(KeyMapping instance){
        if (!instance.consumeClick()) {
            return inventoryKeyLastPressed && !instance.isDown() && !(inventoryKeyLastPressed = false);
        }
        AdvancementToast toast = getToasts().getToast(AdvancementToast.class, Toast.NO_TOKEN);
        if (toast == null) return true;
        inventoryKeyHold++;;
        if (!(inventoryKeyLastPressed = inventoryKeyHold < 10)){
            getToasts().clear();
            inventoryKeyHold = 0;
            setScreen(new LegacyAdvancementsScreen(null,getConnection().getAdvancements()));
        }
        return false;
    }

    @Redirect(method = "handleKeybinds", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/Minecraft;setScreen(Lnet/minecraft/client/gui/screens/Screen;)V", ordinal = 1))
    private void handleKeybinds(Minecraft instance, Screen screen){
        if (screen instanceof ReplaceableScreen s) s.setCanReplace(false);
        setScreen(screen);
    }
    @ModifyArg(method = "handleKeybinds", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/Minecraft;setScreen(Lnet/minecraft/client/gui/screens/Screen;)V", ordinal = 2))
    private Screen handleKeybinds(Screen arg){
        return new LegacyAdvancementsScreen(null,getConnection().getAdvancements());
    }
    @Inject(method = "setScreen",at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/screens/Screen;added()V"))
    private void addedScreen(Screen screen, CallbackInfo ci){
        ControlTooltip.Event.of(screen).setupControlTooltips();
        LegacyTipManager.tipDiffPercentage = 0;
    }
    @Inject(method = "setScreen",at = @At("HEAD"), cancellable = true)
    public void setScreen(Screen screen, CallbackInfo ci) {
        Screen replacement = Legacy4JClient.getReplacementScreen(screen);
        if (replacement != screen) {
            ci.cancel();
            setScreen(replacement);
            return;
        }
        if (Minecraft.getInstance().screen == null && Minecraft.getInstance().level != null && screen != null && (screen instanceof PauseScreen || !screen.isPauseScreen()) ) ScreenUtil.playSimpleUISound(SoundEvents.UI_BUTTON_CLICK.value(),1.0f);
        if (screen == null) ControlTooltip.Event.of(gui).setupControlTooltips();
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
        LegacyLoadingScreen s = new LegacyLoadingScreen(od || lastOd ? Component.translatable("legacy.menu." + (lastOd ? "leaving" : "entering"), ScreenUtil.getDimensionName((lastOd ? this.level : level).dimension())) : Component.empty(), Component.empty());
        if (od || lastOd) s.genericLoading = true;
        updateScreenAndTick(s);
    }

    private boolean isOtherDimension(Level level){
        return level != null && level.dimension() !=Level.OVERWORLD;
    }
    @ModifyArg(method = "resizeDisplay",at = @At(value = "INVOKE", target = "Lcom/mojang/blaze3d/platform/Window;setGuiScale(D)V"))
    public double resizeDisplay(double d) {
        int h = (ScreenUtil.getLegacyOptions().autoResolution().get() ? ScreenUtil.getStandardHeight() : getWindow().getHeight());
        return h / 360d * getTweakedHeightScale(h);
    }
    @Unique
    public double getTweakedHeightScale(int height) {
        if (ScreenUtil.getLegacyOptions().autoResolution().get()){
            if (height == 1080) return 0.999623452;
            else if (height % 720 != 0) return 1.001d;

            return 1d;
        }
        return (1.125 - ScreenUtil.getLegacyOptions().interfaceResolution().get() / 4);
    }
    @Inject(method = "addInitialScreens", at = @At("HEAD"))
    private void addInitialScreens(List<Function<Runnable, Screen>> list, CallbackInfo ci) {
        list.add(r-> new ConfirmationScreen(new TitleScreen(),275,130,Component.empty(), Component.translatable("legacy.menu.autoSave_message")){
            protected void addButtons() {
                transparentBackground = false;
                messageYOffset = 68;
                renderableVList.addRenderable(okButton = Button.builder(Component.translatable("gui.ok"), b-> {if (okAction.test(b)) onClose();}).build());
            }
            @Override
            public void renderableVListInit(){
                panel.y+=25;
                renderableVList.init(this,panel.x + (panel.width - 220) / 2, panel.y + panel.height - 40,220,0);
            }

            @Override
            public boolean shouldCloseOnEsc() {
                return false;
            }

            public void render(GuiGraphics guiGraphics, int i, int j, float f) {
                super.render(guiGraphics, i, j, f);
                ScreenUtil.drawAutoSavingIcon(guiGraphics,panel.x + 127, panel.y + 36);
            }
        });
    }
}
