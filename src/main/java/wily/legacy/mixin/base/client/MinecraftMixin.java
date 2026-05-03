package wily.legacy.mixin.base.client;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import com.llamalad7.mixinextras.injector.v2.WrapWithCondition;
import com.llamalad7.mixinextras.sugar.Local;
import com.mojang.blaze3d.platform.InputConstants;
import com.mojang.blaze3d.platform.Window;
import net.minecraft.util.Util;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.Options;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.gui.components.toasts.AdvancementToast;
import net.minecraft.client.gui.components.toasts.Toast;
import net.minecraft.client.gui.screens.*;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.main.GameConfig;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.multiplayer.ClientPacketListener;
import net.minecraft.client.multiplayer.MultiPlayerGameMode;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.sounds.MusicManager;
import net.minecraft.client.sounds.SoundManager;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.protocol.game.ServerboundPlayerCommandPacket;
import net.minecraft.server.packs.resources.ReloadInstance;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ShieldItem;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.BedBlock;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;
import org.objectweb.asm.Opcodes;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import wily.factoryapi.FactoryAPIClient;
import wily.factoryapi.base.network.CommonNetwork;
import wily.legacy.Legacy4JClient;
import wily.legacy.client.*;
import wily.legacy.client.SoundManagerAccessor;
import wily.legacy.client.screen.*;
import wily.legacy.entity.LegacyShieldPlayer;
import wily.legacy.init.LegacyGameRules;
import wily.legacy.network.ServerPlayerMissHitPayload;
import wily.legacy.network.ServerPlayerShieldPausePayload;
import wily.legacy.util.LegacyItemUtil;
import wily.legacy.util.client.LegacyGuiElements;
import wily.legacy.util.client.LegacyRenderUtil;
import wily.legacy.util.client.LegacySoundUtil;

import java.util.Optional;
import java.util.function.Consumer;
import java.util.function.Supplier;

@Mixin(Minecraft.class)
public abstract class MinecraftMixin {
    @Shadow
    @Nullable
    public ClientLevel level;
    @Shadow
    public Options options;
    @Shadow
    @Nullable
    public LocalPlayer player;
    @Shadow
    @Final
    public Font font;
    @Shadow
    @Nullable
    public Screen screen;
    @Shadow
    @Nullable
    public HitResult hitResult;
    @Shadow
    @Nullable
    public MultiPlayerGameMode gameMode;
    @Shadow
    @Final
    public Gui gui;
    @Unique
    public float realtimeDeltaTickResidual;
    @Unique
    public long lastMillis;
    Vec3 lastPlayerBlockUsePos = null;
    @Unique
    Screen oldScreen;
    private boolean inventoryKeyLastPressed = false;
    private int inventoryKeyHold = 0;
    private int legacy$shieldPauseSyncCooldown = 0;
    @Shadow
    private int rightClickDelay;
    @Shadow
    @Final
    private SoundManager soundManager;

    @Shadow
    public abstract void setScreen(@Nullable Screen screen);

    @Shadow
    public abstract Window getWindow();

    @Shadow
    @Nullable
    public abstract ClientPacketListener getConnection();

    @Shadow
    public abstract boolean isPaused();

    @Unique
    private void legacy$pauseShield() {
        if (player != null && LegacyGameRules.getSidedBooleanGamerule(player, LegacyGameRules.LEGACY_SHIELD_CONTROLS)) {
            boolean usingShield = player.isUsingItem() && player.getUseItem().getItem() instanceof ShieldItem;
            if (usingShield && Legacy4JClient.hasModOnServer() && legacy$shieldPauseSyncCooldown == 0) {
                CommonNetwork.sendToServer(new ServerPlayerShieldPausePayload());
                legacy$shieldPauseSyncCooldown = 1;
            }
            ((LegacyShieldPlayer) player).pauseShield(LegacyShieldPlayer.SHIELD_PAUSE_TICKS);
            if (usingShield && gameMode != null) gameMode.releaseUsingItem(player);
        }
    }

    private Minecraft self() {
        return (Minecraft) (Object) this;
    }

    //? if forge {
    /*@Redirect(method = "<init>", at = @At(value = "INVOKE", target = "Lnet/minecraftforge/fml/loading/ImmediateWindowHandler;loadingOverlay(Ljava/util/function/Supplier;Ljava/util/function/Supplier;Ljava/util/function/Consumer;Z)Ljava/util/function/Supplier;", remap = false))
    private Supplier<Overlay> init(Supplier<Minecraft> mc, Supplier<ReloadInstance> ri, Consumer<Optional<Throwable>> ex, boolean fade) {
        return ()-> new LoadingOverlay(mc.get(),ri.get(),ex,fade);
    }
    *///?} elif neoforge && <1.21.5 {
    /*@Redirect(method = "<init>", at = @At(value = "INVOKE", target = "Lnet/neoforged/fml/loading/ImmediateWindowHandler;loadingOverlay(Ljava/util/function/Supplier;Ljava/util/function/Supplier;Ljava/util/function/Consumer;Z)Ljava/util/function/Supplier;", remap = false))
    private Supplier<Overlay> init(Supplier<Minecraft> mc, Supplier<ReloadInstance> ri, Consumer<Optional<Throwable>> ex, boolean fade) {
        return ()-> new LoadingOverlay(mc.get(),ri.get(),ex,fade);
    }
    *///?} elif neoforge && <26.1 {
    /*@Redirect(method = "<init>", at = @At(value = "INVOKE", target = "Lnet/neoforged/neoforge/client/ClientHooks;createLoadingOverlay(Lnet/minecraft/client/Minecraft;Lnet/minecraft/server/packs/resources/ReloadInstance;Ljava/util/function/Consumer;Z)Lnet/minecraft/client/gui/screens/Overlay;", remap = false))
    private Overlay init(Minecraft minecraft, ReloadInstance reloadInstance, Consumer consumer, boolean b) {
        return new LoadingOverlay(minecraft, reloadInstance, consumer, b);
    }
    *///?}

    @Inject(method = "handleKeybinds", at = @At("HEAD"))
    private void handleKeybinds(CallbackInfo ci) {
        if (legacy$shieldPauseSyncCooldown > 0) legacy$shieldPauseSyncCooldown--;
        if (player != null && screen == null && player.isUsingItem() && player.getUseItem().getItem() instanceof ShieldItem && LegacyGameRules.getSidedBooleanGamerule(player, LegacyGameRules.LEGACY_SHIELD_CONTROLS) && (options.keyAttack.isDown() || options.keyUse.isDown())) {
            legacy$pauseShield();
        }
        if (!options.keyUse.isDown()) lastPlayerBlockUsePos = null;
        if (player != null && LegacyGameRules.getSidedBooleanGamerule(player, LegacyGameRules.LEGACY_OFFHAND_LIMITS) && !LegacyItemUtil.canGoInLceOffhand(player.getMainHandItem())) {
            while (options.keySwapOffhand.consumeClick()) {
            }
        }
    }

    @Inject(method = "continueAttack", at = @At("HEAD"))
    private void continueAttack(boolean bl, CallbackInfo ci) {
        if (bl) legacy$pauseShield();
    }

    @Inject(method = "startAttack", at = @At("HEAD"), cancellable = true)
    private void startAttackHead(CallbackInfoReturnable<Boolean> cir) {
        legacy$pauseShield();
        if (player != null && player.swinging && player.swingingArm == InteractionHand.MAIN_HAND && player.getMainHandItem().has(DataComponents.PIERCING_WEAPON))
            cir.setReturnValue(false);
    }

    @Inject(method = "startAttack", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/player/LocalPlayer;resetAttackStrengthTicker()V"))
    private void startAttack(CallbackInfoReturnable<Boolean> cir) {
        if (Legacy4JClient.hasModOnServer()) CommonNetwork.sendToServer(new ServerPlayerMissHitPayload());
    }

    @Inject(method = "startUseItem", at = @At("HEAD"), cancellable = true)
    private void startUseItem(CallbackInfo ci) {
        legacy$pauseShield();
        if (player != null && player.isSleeping()) {
            ClientPacketListener clientPacketListener = player.connection;
            clientPacketListener.send(new ServerboundPlayerCommandPacket(player, ServerboundPlayerCommandPacket.Action.STOP_SLEEPING));
            ci.cancel();
        }
        if (lastPlayerBlockUsePos != null) {
            if ((Math.abs(lastPlayerBlockUsePos.x - player.position().x) >= 1 || Math.abs(lastPlayerBlockUsePos.y - player.position().y) >= 1 || Math.abs(lastPlayerBlockUsePos.z - player.position().z) >= 1))
                lastPlayerBlockUsePos = lastPlayerBlockUsePos.subtract(Mth.clamp(lastPlayerBlockUsePos.x - player.position().x, -1, 1), Mth.clamp(lastPlayerBlockUsePos.y - player.position().y, -1, 1), Mth.clamp(lastPlayerBlockUsePos.z - player.position().z, -1, 1));
            else ci.cancel();
        }
    }

    @Inject(method = "startUseItem", at = @At("RETURN"))
    private void startUseItemReturn(CallbackInfo ci) {
        if (player.getAbilities().flying && player.isSprinting()) rightClickDelay = -1;
    }

    @Inject(method = "startUseItem", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/multiplayer/MultiPlayerGameMode;useItemOn(Lnet/minecraft/client/player/LocalPlayer;Lnet/minecraft/world/InteractionHand;Lnet/minecraft/world/phys/BlockHitResult;)Lnet/minecraft/world/InteractionResult;"))
    private void startUseItemCreativeBlockPlacing(CallbackInfo ci, @Local InteractionHand hand, @Local BlockHitResult hit) {
        if (LegacyOptions.legacyCreativeBlockPlacing.get() && rightClickDelay == 4 && player.getAbilities().instabuild && ControlTooltip.canPlace(self(), player.getItemInHand(hand), hand)) {
            if (lastPlayerBlockUsePos == null) lastPlayerBlockUsePos = player.position();
            rightClickDelay = 0;
        }
        if (level.getBlockState(hit.getBlockPos()).getBlock() instanceof BedBlock || player.getAbilities().flying && player.isSprinting())
            rightClickDelay = -1;
    }

    @Inject(method = "startUseItem", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/multiplayer/MultiPlayerGameMode;useItemOn(Lnet/minecraft/client/player/LocalPlayer;Lnet/minecraft/world/InteractionHand;Lnet/minecraft/world/phys/BlockHitResult;)Lnet/minecraft/world/InteractionResult;", shift = At.Shift.AFTER))
    private void rememberConduitRotation(CallbackInfo ci, @Local InteractionHand hand, @Local BlockHitResult hit) {
        if (!(player.getItemInHand(hand).getItem() instanceof BlockItem blockItem) || blockItem.getBlock() != Blocks.CONDUIT) return;
        BlockPlaceContext context = new BlockPlaceContext(player, hand, player.getItemInHand(hand), hit);
        if (level.getBlockState(context.getClickedPos()).is(Blocks.CONDUIT)) ConduitRotationCache.remember(level, context.getClickedPos(), player.getYRot());
    }

    @ModifyExpressionValue(method = "tick", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/player/LocalPlayer;isSleeping()Z"))
    private boolean tick(boolean original) {
        return false;
    }

    @ModifyReturnValue(method = "isWindowActive", at = @At("RETURN"))
    private boolean isWindowActive(boolean original) {
        return original || LegacyOptions.unfocusedInputs.get();
    }

    @Inject(method = "runTick", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/sounds/SoundManager;updateSource(Lnet/minecraft/client/Camera;)V"))
    public void runSoundTick(boolean bl, CallbackInfo ci) {
        float deltaTicks = FactoryAPIClient.getPartialTick();
        realtimeDeltaTickResidual += deltaTicks;
        int i = (int) realtimeDeltaTickResidual;
        realtimeDeltaTickResidual -= i;
        if (Util.getMillis() - lastMillis > 50 || i > 0) lastMillis = Util.getMillis();
        if (Util.getMillis() - lastMillis > 60 && i == 0) i = 1;
        for (int j = 0; j < Math.min(10, i); ++j) {
            soundManager.tick(this.isPaused());
            LegacyMusicFader.tick();
        }
    }

    @Redirect(method = "tick", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/sounds/SoundManager;tick(Z)V"))
    private void noSoundTick(SoundManager instance, boolean bl) {
    }

    @WrapWithCondition(method = "tick", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/sounds/MusicManager;tick()V"))
    private boolean tickMusicManager(MusicManager instance) {
        return LegacyMusicFader.musicManagerShouldTick;
    }

    @Inject(method = /*? if <1.20.3 {*//*"clearLevel(Lnet/minecraft/client/gui/screens/Screen;)V"*//*?} else if <1.21 {*//*"clearClientLevel"*//*?} else {*/"disconnect(Lnet/minecraft/client/gui/screens/Screen;ZZ)V"/*?}*/, at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/Gui;onDisconnected()V"))
    private void disconnectFadeMusic(Screen disconnectScreen, boolean retainDownloadedResourcePacks, boolean updateLevelInEngines, CallbackInfo ci) {
        ConduitRotationCache.clear();
        SoundManagerAccessor.of(this.soundManager).fadeAllMusic();
    }

    @Redirect(method = "handleKeybinds", slice = @Slice(from = @At(value = "FIELD", target = "Lnet/minecraft/client/Options;keyInventory:Lnet/minecraft/client/KeyMapping;"), to = @At(value = "FIELD", target = "Lnet/minecraft/client/Options;keyAdvancements:Lnet/minecraft/client/KeyMapping;")), at = @At(value = "INVOKE", target = "Lnet/minecraft/client/KeyMapping;consumeClick()Z"))
    private boolean handleKeybindsInventoryKey(KeyMapping instance) {
        boolean clicked = instance.consumeClick();
        AdvancementToast toast = FactoryAPIClient.getToasts().getToast(AdvancementToast.class, Toast.NO_TOKEN);
        if (toast == null) {
            if (inventoryKeyLastPressed && !instance.isDown()) {
                inventoryKeyHold = 0;
                inventoryKeyLastPressed = false;
                return true;
            }
            inventoryKeyHold = 0;
            inventoryKeyLastPressed = false;
            return clicked;
        }

        if (instance.isDown() && (clicked || inventoryKeyLastPressed)) {
            inventoryKeyLastPressed = true;
            if (++inventoryKeyHold >= 10) {
                openToastAdvancement(toast);
                inventoryKeyLastPressed = false;
                inventoryKeyHold = 0;
            }
            return false;
        }

        if (inventoryKeyLastPressed) {
            inventoryKeyHold = 0;
            inventoryKeyLastPressed = false;
            return true;
        }

        return clicked;
    }

    @Unique
    private void openToastAdvancement(AdvancementToast toast) {
        FactoryAPIClient.getToasts().clear();
        LegacyAdvancementsScreen screen = new LegacyAdvancementsScreen(null);
        setScreen(screen);
        screen.focusRenderable(r -> r instanceof LegacyAdvancementsScreen.AdvancementButton b && b.id.equals(AdvancementToastAccessor.of(toast).getAdvancementId()), i -> screen.getTabList().tabButtons.get(i).onPress(new KeyEvent(InputConstants.KEY_RETURN, 0, 0)));
    }

    @WrapWithCondition(method = "handleKeybinds", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/Minecraft;setScreen(Lnet/minecraft/client/gui/screens/Screen;)V", ordinal = 1))
    private boolean handleKeybinds(Minecraft instance, Screen screen) {
        if (screen instanceof ReplaceableScreen s) s.setCanReplace(false);
        return true;
    }

    @ModifyArg(method = "handleKeybinds", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/Minecraft;setScreen(Lnet/minecraft/client/gui/screens/Screen;)V", ordinal = 2))
    private Screen handleKeybinds(Screen arg) {
        return LegacyOptions.legacyAdvancements.get() ? new LegacyAdvancementsScreen(null) : arg;
    }

    @WrapWithCondition(method = "setScreen", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/screens/Screen;removed()V"))
    private boolean removedScreen(Screen instance, Screen newScreen) {
        return !(newScreen instanceof OverlayPanelScreen s) || s.parent != instance;
    }

    @Inject(method = "setScreen", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/screens/Screen;added()V"))
    private void addedScreen(Screen screen, CallbackInfo ci) {
        ControlTooltip.Event.of(screen).setupControlTooltips();
        ControlTooltip.Renderer.SCREEN_EVENT.invoker.accept(screen, ControlTooltip.Event.of(screen).getControlTooltips());
        LegacyTipManager.resetTipOffset(true);
    }

    @WrapWithCondition(method = "setScreen", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/screens/Screen;init(II)V"))
    private boolean initScreen(Screen instance, int i, int j) {
        if (oldScreen instanceof OverlayPanelScreen s && s.parent == instance) {
            instance.resize(i, j);
            return false;
        }
        return true;
    }

    @Inject(method = "resizeGui", at = @At("RETURN"))
    private void resizeDisplay(CallbackInfo ci) {
        LegacyTipManager.rebuildActual();
        LegacyTipManager.rebuildActualLoading();
        gui.getChat().rescaleChat();
    }

    @ModifyArg(method = "disconnect(Lnet/minecraft/client/gui/screens/Screen;ZZ)V", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/Minecraft;setScreenAndShow(Lnet/minecraft/client/gui/screens/Screen;)V"))
    private Screen changeGenericScreen(Screen arg, @Local(argsOnly = true) Screen disconnectScreen) {
        return disconnectScreen instanceof LegacyLoadingScreen ? disconnectScreen : arg;
    }

    @Inject(method = "setScreen", at = @At("HEAD"), cancellable = true)
    public void setScreen(Screen screen, CallbackInfo ci) {
        oldScreen = this.screen;
        Screen replacement = Legacy4JClient.getReplacementScreen(screen);
        if (replacement != screen) {
            ci.cancel();
            setScreen(replacement);
            return;
        }
        if (Minecraft.getInstance().screen == null && Minecraft.getInstance().level != null && screen != null && (screen instanceof PauseScreen || !screen.isPauseScreen()))
            LegacySoundUtil.playSimpleUISound(SoundEvents.UI_BUTTON_CLICK.value(), 1.0f);
        if (screen == null && level != null) {
            LegacyGuiElements.lastGui = Util.getMillis();
            ControlTooltip.Event.of(gui).setupControlTooltips();
            ControlTooltip.Renderer.GUI_EVENT.invoker.accept(gui, ControlTooltip.Event.of(gui).getControlTooltips());
        }
    }

    @WrapWithCondition(method = "runTick(Z)V", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/sounds/SoundManager;pauseAllExcept([Lnet/minecraft/sounds/SoundSource;)V"))
    public boolean pauseGame(SoundManager instance, SoundSource[] soundSources) {
        return false;
    }

    @WrapWithCondition(method = "updateLevelInEngines(Lnet/minecraft/client/multiplayer/ClientLevel;Z)V", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/sounds/SoundManager;stop()V"))
    public boolean updateScreenAndTick(SoundManager instance) {
        SoundManagerAccessor.of(instance).stopAllSound();
        return false;
    }

    @ModifyVariable(method = "buildInitialScreens", at = @At(value = "STORE"))
    private Runnable addInitialScreens(Runnable run) {
        return () -> {
            run.run();
            if (screen != null) setScreen(LegacyRenderUtil.getInitialScreen());
        };
    }
}
