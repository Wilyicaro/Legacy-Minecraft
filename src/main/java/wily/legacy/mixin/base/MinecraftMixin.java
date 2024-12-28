package wily.legacy.mixin.base;

import com.mojang.blaze3d.platform.Window;
import com.mojang.realmsclient.client.RealmsClient;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.Options;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.toasts.AdvancementToast;
import net.minecraft.client.gui.components.toasts.Toast;
import net.minecraft.client.gui.screens.*;
import net.minecraft.client.main.GameConfig;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.multiplayer.ClientPacketListener;
import net.minecraft.client.multiplayer.MultiPlayerGameMode;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.sounds.SoundManager;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ServerboundPlayerCommandPacket;
import net.minecraft.server.packs.resources.ReloadInstance;
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
import org.spongepowered.asm.mixin.gen.Accessor;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import wily.factoryapi.FactoryAPIClient;
import wily.factoryapi.base.client.MinecraftAccessor;
import wily.factoryapi.base.network.CommonNetwork;
import wily.legacy.Legacy4JClient;
import wily.legacy.client.AdvancementToastAccessor;
import wily.legacy.client.LegacyOption;
import wily.legacy.client.LegacyTipManager;
import wily.legacy.client.screen.*;
import wily.legacy.network.ServerPlayerMissHitPayload;
import wily.legacy.util.LegacyComponents;
import wily.legacy.util.ScreenUtil;

import java.util.List;
import java.util.Optional;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;

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

    @Shadow @Final public Gui gui;

    @Unique
    Screen oldScreen;

    private Minecraft self(){
        return (Minecraft)(Object)this;
    }

    //? if forge {
    /*@Redirect(method = "<init>", at = @At(value = "INVOKE", target = "Lnet/minecraftforge/fml/loading/ImmediateWindowHandler;loadingOverlay(Ljava/util/function/Supplier;Ljava/util/function/Supplier;Ljava/util/function/Consumer;Z)Ljava/util/function/Supplier;", remap = false))
    private Supplier<Overlay> init(Supplier<Minecraft> mc, Supplier<ReloadInstance> ri, Consumer<Optional<Throwable>> ex, boolean fade){
        return ()-> new LoadingOverlay(mc.get(),ri.get(),ex,fade);
    }
    *///?} elif neoforge {
    /*@Redirect(method = "<init>", at = @At(value = "INVOKE", target = "Lnet/neoforged/fml/loading/ImmediateWindowHandler;loadingOverlay(Ljava/util/function/Supplier;Ljava/util/function/Supplier;Ljava/util/function/Consumer;Z)Ljava/util/function/Supplier;", remap = false))
    private Supplier<Overlay> init(Supplier<Minecraft> mc, Supplier<ReloadInstance> ri, Consumer<Optional<Throwable>> ex, boolean fade){
        return ()-> new LoadingOverlay(mc.get(),ri.get(),ex,fade);
    }
    *///?}

    //? if <1.20.5 {
    /*@Accessor
    public abstract float getPausePartialTick();
    *///?}

    //? if <1.21.4 {
    /*@Inject(method = "pickBlock", at = @At("HEAD"), cancellable = true)
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
    *///?}

    @Inject(method = "handleKeybinds", at = @At("HEAD"))
    private void handleKeybinds(CallbackInfo ci) {
        if (!options.keyUse.isDown()) lastPlayerBlockUsePos = null;
    }
    @Inject(method = "startAttack", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/player/LocalPlayer;resetAttackStrengthTicker()V"))
    private void startAttack(CallbackInfoReturnable<Boolean> cir) {
        CommonNetwork.sendToServer(new ServerPlayerMissHitPayload());
    }
    @Inject(method = "startUseItem", at = @At("HEAD"), cancellable = true)
    private void startUseItem(CallbackInfo ci){
        if (player != null && player.isSleeping()){
            ClientPacketListener clientPacketListener = player.connection;
            clientPacketListener.send(new ServerboundPlayerCommandPacket(player, ServerboundPlayerCommandPacket.Action.STOP_SLEEPING));
            ci.cancel();
        }
        if (lastPlayerBlockUsePos != null){
            if ((Math.abs(lastPlayerBlockUsePos.x - player.position().x) >= 1 || Math.abs(lastPlayerBlockUsePos.y - player.position().y) >= 1 || Math.abs(lastPlayerBlockUsePos.z - player.position().z) >= 1)) lastPlayerBlockUsePos = lastPlayerBlockUsePos.subtract(Mth.clamp(lastPlayerBlockUsePos.x - player.position().x,-1,1),Mth.clamp(lastPlayerBlockUsePos.y - player.position().y,-1,1),Mth.clamp(lastPlayerBlockUsePos.z - player.position().z,-1,1));
            else ci.cancel();
        }
    }
    @Inject(method = "startUseItem", at = @At("RETURN"))
    private void startUseItemReturn(CallbackInfo ci){
        if (player.getAbilities().flying && player.isSprinting()) rightClickDelay = -1;
    }
    @Redirect(method = "startUseItem", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/multiplayer/MultiPlayerGameMode;useItemOn(Lnet/minecraft/client/player/LocalPlayer;Lnet/minecraft/world/InteractionHand;Lnet/minecraft/world/phys/BlockHitResult;)Lnet/minecraft/world/InteractionResult;"))
    private InteractionResult startUseItemReturn(MultiPlayerGameMode instance, LocalPlayer localPlayer, InteractionHand arg, BlockHitResult arg2){
        if (LegacyOption.legacyCreativeBlockPlacing.get() && rightClickDelay == 4 && player.getAbilities().instabuild && ControlTooltip.canPlace(self(),arg)) {
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
        AdvancementToast toast = FactoryAPIClient.getToasts().getToast(AdvancementToast.class, Toast.NO_TOKEN);
        if (toast == null) return true;
        inventoryKeyHold++;;
        if (!(inventoryKeyLastPressed = inventoryKeyHold < 10)){
            FactoryAPIClient.getToasts().clear();
            inventoryKeyHold = 0;
            LegacyAdvancementsScreen screen = new LegacyAdvancementsScreen(null);
            setScreen(screen);
            screen.focusRenderable(r-> r instanceof LegacyAdvancementsScreen.AdvancementButton b && b.id.equals(AdvancementToastAccessor.of(toast).getAdvancementId()), i-> screen.getTabList().tabButtons.get(i).onPress());
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
        return new LegacyAdvancementsScreen(null);
    }
    @Redirect(method = "setScreen",at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/screens/Screen;removed()V"))
    private void removedScreen(Screen instance, Screen newScreen){
        if (newScreen instanceof OverlayPanelScreen s && s.parent == instance) return;
        instance.removed();
    }
    @Inject(method = "setScreen",at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/screens/Screen;added()V"))
    private void addedScreen(Screen screen, CallbackInfo ci){
        ControlTooltip.Event.of(screen).setupControlTooltips();
        ControlTooltip.Renderer.SCREEN_EVENT.invoker.accept(screen,ControlTooltip.Event.of(screen).getControlTooltips());
        LegacyTipManager.tipDiffPercentage = 0;
    }
    @Redirect(method = "setScreen",at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/screens/Screen;init(Lnet/minecraft/client/Minecraft;II)V"))
    private void initScreen(Screen instance, Minecraft minecraft, int i, int j){
        if (oldScreen instanceof OverlayPanelScreen s && s.parent == instance) instance.resize(minecraft,i,j);
        else instance.init(minecraft,i,j);
    }
    @Inject(method = "setScreen",at = @At("HEAD"), cancellable = true)
    public void setScreen(Screen screen, CallbackInfo ci) {
        oldScreen = this.screen;
        Screen replacement = Legacy4JClient.getReplacementScreen(screen);
        if (replacement != screen) {
            ci.cancel();
            setScreen(replacement);
            return;
        }
        if (Minecraft.getInstance().screen == null && Minecraft.getInstance().level != null && screen != null && (screen instanceof PauseScreen || !screen.isPauseScreen())) ScreenUtil.playSimpleUISound(SoundEvents.UI_BUTTON_CLICK.value(),1.0f);
        if (screen == null && level != null) {
            ControlTooltip.Event.of(gui).setupControlTooltips();
            ControlTooltip.Renderer.GUI_EVENT.invoker.accept(gui,ControlTooltip.Event.of(gui).getControlTooltips());
        }
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
        if (od || lastOd) s.setGenericLoading(true);
        updateScreenAndTick(s);
    }

    private boolean isOtherDimension(Level level){
        return level != null && level.dimension() !=Level.OVERWORLD;
    }
    @ModifyArg(method = "resizeDisplay",at = @At(value = "INVOKE", target = "Lcom/mojang/blaze3d/platform/Window;setGuiScale(D)V"))
    public double resizeDisplay(double d) {
        int h = (LegacyOption.autoResolution.get() ? ScreenUtil.getStandardHeight() : getWindow().getHeight());
        return h / 360d * getTweakedHeightScale(h);
    }
    @Unique
    public double getTweakedHeightScale(int height) {
        if (LegacyOption.autoResolution.get()){
            if (height == 1080) return 0.999623452;
            else if (height % 720 != 0) return 1.001d;

            return 1d;
        }
        return (1.125 - LegacyOption.interfaceResolution.get() / 4);
    }
    //? if >1.20.1 {
    @Inject(method = "addInitialScreens", at = @At("HEAD"))
    private void addInitialScreens(List<Function<Runnable, Screen>> list, CallbackInfo ci) {
        list.add(r-> ConfirmationScreen.createSaveInfoScreen(new TitleScreen()));
    }
    //?} else {
    /*@Inject(method = "setInitialScreen", at = @At("HEAD"), cancellable = true)
    private void addInitialScreens(RealmsClient realmsClient, ReloadInstance reloadInstance, GameConfig.QuickPlayData quickPlayData, CallbackInfo ci) {
        ci.cancel();
        FactoryAPIClient.SECURE_EXECUTOR.executeNowIfPossible(() -> setScreen(ConfirmationScreen.createSaveInfoScreen(new TitleScreen())), MinecraftAccessor.getInstance()::hasGameLoaded);
    }
    *///?}
}
