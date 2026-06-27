package wily.legacy.mixin.base;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import com.llamalad7.mixinextras.injector.v2.WrapWithCondition;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Local;
import com.mojang.blaze3d.platform.Window;
import com.mojang.realmsclient.client.RealmsClient;
import net.minecraft.Util;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.Options;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.gui.components.toasts.AdvancementToast;
import net.minecraft.client.gui.components.toasts.Toast;
import net.minecraft.client.gui.screens.*;
import net.minecraft.client.main.GameConfig;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.multiplayer.ClientPacketListener;
import net.minecraft.client.multiplayer.MultiPlayerGameMode;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.renderer.ItemInHandRenderer;
import net.minecraft.client.sounds.MusicManager;
import net.minecraft.client.sounds.SoundManager;
//? if >=1.21.2 {
/*import net.minecraft.core.component.DataComponents;
*///?}
import net.minecraft.core.BlockPos;
import net.minecraft.network.protocol.game.ServerboundPlayerCommandPacket;
import net.minecraft.server.packs.resources.ReloadInstance;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
//? if >=1.20.5 {
import net.minecraft.world.entity.EquipmentSlot;
//?}
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.TamableAnimal;
import net.minecraft.world.entity.animal.Animal;
import net.minecraft.world.entity.animal.Cat;
import net.minecraft.world.entity.animal.Parrot;
//? if <1.21.5 {
import net.minecraft.world.entity.animal.Sheep;
import net.minecraft.world.entity.animal.Wolf;
//?} else {
/*import net.minecraft.world.entity.animal.sheep.Sheep;
import net.minecraft.world.entity.animal.wolf.Wolf;
*///?}
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.DyeColor;
//? if >=1.20.5 && <1.21.2 {
import net.minecraft.world.item.Equipable;
//?}
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.ShieldItem;
import net.minecraft.world.item.ShearsItem;
import net.minecraft.world.item.SpawnEggItem;
import net.minecraft.world.level.block.BedBlock;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.block.BellBlock;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.ButtonBlock;
import net.minecraft.world.level.block.RedStoneOreBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import wily.factoryapi.FactoryAPI;
import wily.factoryapi.FactoryAPIClient;
import wily.factoryapi.base.client.MinecraftAccessor;
import wily.factoryapi.base.network.CommonNetwork;
import wily.legacy.Legacy4J;
import wily.legacy.Legacy4JClient;
import wily.legacy.client.*;
import wily.legacy.client.SoundManagerAccessor;
import wily.legacy.client.screen.*;
import wily.legacy.entity.LegacyShieldPlayer;
import wily.legacy.init.LegacyGameRules;
import wily.legacy.network.ServerPlayerMissHitPayload;
import wily.legacy.network.ServerPlayerShieldPausePayload;
import wily.legacy.util.LegacyBlockProtection;
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
    private int legacy$shieldPauseSyncCooldown = 0;
    private boolean legacy$dropKeyDown = false;
    @Unique
    private InteractionHand legacy$suppressedUseAnimationHand;

    @Shadow private int rightClickDelay;

    @Shadow @Final public Gui gui;

    @Shadow @Final private SoundManager soundManager;

    @Shadow public abstract boolean isPaused();



    @Unique
    Screen oldScreen;

    @Unique public float realtimeDeltaTickResidual;

    @Unique public long lastMillis;

    private Minecraft self(){
        return (Minecraft)(Object)this;
    }

    @Unique
    private void legacy$pauseShield() {
        if (player != null && LegacyGameRules.getSidedBooleanGamerule(player, LegacyGameRules.LEGACY_SHIELD_CONTROLS)) {
            boolean usingShield = player.isUsingItem() && player.getUseItem().getItem() instanceof ShieldItem;
            if (usingShield && Legacy4JClient.hasModOnServer() && legacy$shieldPauseSyncCooldown == 0) {
                CommonNetwork.sendToServer(new ServerPlayerShieldPausePayload());
                legacy$shieldPauseSyncCooldown = 1;
            }
            ((LegacyShieldPlayer)player).pauseShield(LegacyShieldPlayer.SHIELD_PAUSE_TICKS);
            if (usingShield && gameMode != null) gameMode.releaseUsingItem(player);
        }
    }

    //? if forge {
    /*@Redirect(method = "<init>", at = @At(value = "INVOKE", target = "Lnet/minecraftforge/fml/loading/ImmediateWindowHandler;loadingOverlay(Ljava/util/function/Supplier;Ljava/util/function/Supplier;Ljava/util/function/Consumer;Z)Ljava/util/function/Supplier;", remap = false))
    private Supplier<Overlay> init(Supplier<Minecraft> mc, Supplier<ReloadInstance> ri, Consumer<Optional<Throwable>> ex, boolean fade){
        return ()-> new LoadingOverlay(mc.get(),ri.get(),ex,fade);
    }
    *///?} elif neoforge && <1.21.5 {
    /*@Redirect(method = "<init>", at = @At(value = "INVOKE", target = "Lnet/neoforged/fml/loading/ImmediateWindowHandler;loadingOverlay(Ljava/util/function/Supplier;Ljava/util/function/Supplier;Ljava/util/function/Consumer;Z)Ljava/util/function/Supplier;", remap = false))
    private Supplier<Overlay> init(Supplier<Minecraft> mc, Supplier<ReloadInstance> ri, Consumer<Optional<Throwable>> ex, boolean fade){
        return ()-> new LoadingOverlay(mc.get(),ri.get(),ex,fade);
    }
    *///?} elif neoforge {
    /*@Redirect(method = "<init>", at = @At(value = "INVOKE", target = "Lnet/neoforged/neoforge/client/ClientHooks;createLoadingOverlay(Lnet/minecraft/client/Minecraft;Lnet/minecraft/server/packs/resources/ReloadInstance;Ljava/util/function/Consumer;Z)Lnet/minecraft/client/gui/screens/Overlay;", remap = false))
    private Overlay init(Minecraft minecraft, ReloadInstance reloadInstance, Consumer consumer, boolean b){
        return new LoadingOverlay(minecraft, reloadInstance, consumer, b);
    }
    *///?}

    //? if <1.21.4 {
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
    //?}

    @Inject(method = "handleKeybinds", at = @At("HEAD"))
    private void handleKeybinds(CallbackInfo ci) {
        if (legacy$shieldPauseSyncCooldown > 0) legacy$shieldPauseSyncCooldown--;
        legacy$handleDropKey();
        if (player != null && screen == null && player.isUsingItem() && player.getUseItem().getItem() instanceof ShieldItem && LegacyGameRules.getSidedBooleanGamerule(player, LegacyGameRules.LEGACY_SHIELD_CONTROLS) && (options.keyAttack.isDown() || options.keyUse.isDown())) {
            legacy$pauseShield();
        }
        if (!options.keyUse.isDown()) lastPlayerBlockUsePos = null;
        if (player != null && LegacyGameRules.getSidedBooleanGamerule(player, LegacyGameRules.LEGACY_OFFHAND_LIMITS) && !Legacy4J.canGoInLceOffhand(player.getMainHandItem())) {
            while (options.keySwapOffhand.consumeClick()) {
            }
        }
    }

    @WrapWithCondition(method = "handleKeybinds", slice = @Slice(from = @At(value = "FIELD", target = "Lnet/minecraft/client/Options;keyDrop:Lnet/minecraft/client/KeyMapping;"), to = @At(value = "FIELD", target = "Lnet/minecraft/client/Options;keyChat:Lnet/minecraft/client/KeyMapping;")), at = @At(value = "INVOKE", target = "Lnet/minecraft/client/player/LocalPlayer;swing(Lnet/minecraft/world/InteractionHand;)V"))
    private boolean handleDropSwing(LocalPlayer player, InteractionHand hand) {
        return false;
    }

    @Unique
    private void legacy$handleDropKey() {
        if (player == null) {
            legacy$dropKeyDown = false;
            return;
        }

        boolean clicked = false;
        boolean down = options.keyDrop.isDown();
        while (options.keyDrop.consumeClick()) {
            clicked = true;
        }
        if (screen == null && !player.isSpectator() && !down && (legacy$dropKeyDown || clicked)) {
            player.drop(Screen.hasControlDown());
        }
        legacy$dropKeyDown = down;
    }

    @Inject(method = "continueAttack", at = @At("HEAD"))
    private void continueAttack(boolean bl, CallbackInfo ci) {
        if (bl) legacy$pauseShield();
    }

    //? if >=1.21.5 {
    /*@Inject(method = "pick", at = @At("RETURN"))
    private void pick(float tickDelta, CallbackInfo ci) {
        if (level == null || player == null || !(hitResult instanceof BlockHitResult blockHit)) return;
        if (LegacyBlockProtection.blocksNetherPortalBreak(level.getBlockState(blockHit.getBlockPos()))) {
            hitResult = legacy$pickThroughNetherPortal(tickDelta);
        }
    }

    @Unique
    private HitResult legacy$pickThroughNetherPortal(float tickDelta) {
        Vec3 from = player.getEyePosition(tickDelta);
        Vec3 to = from.add(player.getViewVector(tickDelta).scale(player.blockInteractionRange()));
        return level.clip(new ClipContext(from, to, ClipContext.Block.OUTLINE, ClipContext.Fluid.NONE, player) {
            @Override
            public VoxelShape getBlockShape(BlockState state, BlockGetter level, BlockPos pos) {
                if (LegacyBlockProtection.blocksNetherPortalBreak(state)) return Shapes.empty();
                return super.getBlockShape(state, level, pos);
            }
        });
    }
    *///?}

    @Inject(method = "startAttack", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/player/LocalPlayer;resetAttackStrengthTicker()V"))
    private void startAttack(CallbackInfoReturnable<Boolean> cir) {
        CommonNetwork.sendToServer(new ServerPlayerMissHitPayload());
    }
    @Inject(method = "startAttack", at = @At("HEAD"))
    private void startAttackHead(CallbackInfoReturnable<Boolean> cir) {
        legacy$pauseShield();
    }

    @WrapWithCondition(method = "startUseItem", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/player/LocalPlayer;swing(Lnet/minecraft/world/InteractionHand;)V"))
    private boolean startUseItemSwing(LocalPlayer player, InteractionHand hand) {
        return !legacy$suppressedUseAnimation(hand);
    }

    @WrapWithCondition(method = "startUseItem", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/renderer/ItemInHandRenderer;itemUsed(Lnet/minecraft/world/InteractionHand;)V"))
    private boolean startUseItemItemUsed(ItemInHandRenderer renderer, InteractionHand hand) {
        return !legacy$suppressedUseAnimation(hand);
    }

    @WrapOperation(method = "startUseItem", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/multiplayer/MultiPlayerGameMode;interactAt(Lnet/minecraft/world/entity/player/Player;Lnet/minecraft/world/entity/Entity;Lnet/minecraft/world/phys/EntityHitResult;Lnet/minecraft/world/InteractionHand;)Lnet/minecraft/world/InteractionResult;"))
    private InteractionResult startUseItemInteractAt(MultiPlayerGameMode gameMode, Player player, Entity entity, EntityHitResult hit, InteractionHand hand, Operation<InteractionResult> original) {
        boolean suppress = legacy$suppressesUseAnimation(hand);
        if (suppress) legacy$suppressedUseAnimationHand = hand;
        InteractionResult result = original.call(gameMode, player, entity, hit, hand);
        legacy$rememberServerUseAnimation(hand, suppress, result);
        return result;
    }

    @WrapOperation(method = "startUseItem", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/multiplayer/MultiPlayerGameMode;interact(Lnet/minecraft/world/entity/player/Player;Lnet/minecraft/world/entity/Entity;Lnet/minecraft/world/InteractionHand;)Lnet/minecraft/world/InteractionResult;"))
    private InteractionResult startUseItemInteract(MultiPlayerGameMode gameMode, Player player, Entity entity, InteractionHand hand, Operation<InteractionResult> original) {
        boolean suppress = legacy$suppressesUseAnimation(hand);
        if (suppress) legacy$suppressedUseAnimationHand = hand;
        InteractionResult result = original.call(gameMode, player, entity, hand);
        legacy$rememberServerUseAnimation(hand, suppress, result);
        return result;
    }

    @WrapOperation(method = "startUseItem", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/multiplayer/MultiPlayerGameMode;useItemOn(Lnet/minecraft/client/player/LocalPlayer;Lnet/minecraft/world/InteractionHand;Lnet/minecraft/world/phys/BlockHitResult;)Lnet/minecraft/world/InteractionResult;"))
    private InteractionResult startUseItemUseOn(MultiPlayerGameMode gameMode, LocalPlayer player, InteractionHand hand, BlockHitResult hit, Operation<InteractionResult> original) {
        boolean suppress = legacy$suppressesUseAnimation(hand);
        BlockState state = level == null ? null : level.getBlockState(hit.getBlockPos());
        ItemStack item = player.getItemInHand(hand).copy();
        if (suppress) legacy$suppressedUseAnimationHand = hand;
        InteractionResult result = original.call(gameMode, player, hand, hit);
        legacy$rememberServerUseAnimation(hand, suppress, result);
        legacy$playMissingUseAnimation(player, hand, item, state, result, suppress);
        return result;
    }

    @Unique
    private void legacy$playMissingUseAnimation(LocalPlayer player, InteractionHand hand, ItemStack item, BlockState state, InteractionResult result, boolean suppress) {
        if (suppress || state == null || legacy$resultSwingsClient(result)) return;
        if (legacy$triesInvalidPainting(item, result) || legacy$triesActiveButton(state, result) || legacy$triesInvalidBell(state, result)) player.swing(hand);
    }

    @Unique
    private boolean legacy$resultSwingsClient(InteractionResult result) {
        //? if <1.21.2 {
        return result.shouldSwing();
        //?} else {
        /*return result instanceof InteractionResult.Success success && success.swingSource() == InteractionResult.SwingSource.CLIENT;
        *///?}
    }

    @Unique
    private boolean legacy$resultIsSuccess(InteractionResult result) {
        //? if <1.21.2 {
        return result == InteractionResult.SUCCESS;
        //?} else {
        /*return result instanceof InteractionResult.Success;
        *///?}
    }

    @Unique
    private boolean legacy$resultIsFail(InteractionResult result) {
        //? if <1.21.2 {
        return result == InteractionResult.FAIL;
        //?} else {
        /*return result instanceof InteractionResult.Fail;
        *///?}
    }

    @Unique
    private boolean legacy$resultIsPass(InteractionResult result) {
        //? if <1.21.2 {
        return result == InteractionResult.PASS;
        //?} else {
        /*return result instanceof InteractionResult.Pass;
        *///?}
    }

    @Unique
    private boolean legacy$triesInvalidPainting(ItemStack item, InteractionResult result) {
        return item.getItem() instanceof HangingEntityItemAccessor hanging && hanging.getType() == EntityType.PAINTING;
    }

    @Unique
    private boolean legacy$triesActiveButton(BlockState state, InteractionResult result) {
        return state.getBlock() instanceof ButtonBlock && state.getValue(ButtonBlock.POWERED) && !legacy$resultIsFail(result);
    }

    @Unique
    private boolean legacy$triesInvalidBell(BlockState state, InteractionResult result) {
        return state.getBlock() instanceof BellBlock && legacy$resultIsPass(result);
    }

    @Unique
    private void legacy$rememberServerUseAnimation(InteractionHand hand, boolean suppress, InteractionResult result) {
        if (suppress && !legacy$resultIsFail(result)) LegacyInteractionAnimations.suppressServerSwing(hand);
    }

    @Unique
    private boolean legacy$suppressedUseAnimation(InteractionHand hand) {
        return legacy$suppressedUseAnimationHand == hand || legacy$suppressesUseAnimation(hand);
    }

    @Unique
    private boolean legacy$suppressesUseAnimation(InteractionHand hand) {
        if (player == null || level == null || hitResult == null) return false;
        ItemStack item = player.getItemInHand(hand);
        if (hitResult.getType() == HitResult.Type.ENTITY && hitResult instanceof EntityHitResult entityHit) return legacy$suppressesEntityUseAnimation(entityHit.getEntity(), item);
        return hitResult.getType() == HitResult.Type.BLOCK && hitResult instanceof BlockHitResult blockHit && level.getBlockState(blockHit.getBlockPos()).getBlock() instanceof RedStoneOreBlock;
    }

    @Unique
    private boolean legacy$suppressesEntityUseAnimation(Entity entity, ItemStack item) {
        DyeColor color = Legacy4J.getDyeColorOrNull(item.getItem());
        if (item.getItem() instanceof SpawnEggItem && entity instanceof Mob && entity.getType() == legacy$getSpawnEggType(item)) return true;
        if (entity instanceof Wolf wolf && !wolf.isTame() && item.is(Items.BONE)) return true;
        if (entity instanceof Sheep sheep) {
            if (item.getItem() instanceof ShearsItem && sheep.isAlive() && !sheep.isSheared()) return true;
            if (color != null && sheep.getColor() != color) return true;
        }
        if (legacy$canToggleTamedSitting(entity, item)) return true;
        return entity instanceof Animal animal && animal.isFood(item);
    }

    @Unique
    private EntityType<?> legacy$getSpawnEggType(ItemStack item) {
        return ((SpawnEggItem)item.getItem()).getType(/*? if >=1.21.4 {*//*level.registryAccess(), *//*?}*/item/*? if <1.20.5 {*//*.getTag()*//*?}*/);
    }

    @Unique
    private boolean legacy$canToggleTamedSitting(Entity entity, ItemStack item) {
        if (!(entity instanceof TamableAnimal animal) || !animal.isTame() || !animal.isOwnedBy(player)) return false;
        if (!(entity instanceof Wolf || entity instanceof Cat || entity instanceof Parrot)) return false;
        if (legacy$canDyeCollar(entity, player.getMainHandItem()) || legacy$canDyeCollar(entity, player.getOffhandItem())) return false;
        if (legacy$canEquipWolfArmor(entity, item)) return false;
        return !(entity instanceof Parrot parrot) || parrot.onGround() && !player.isPassenger();
    }

    @Unique
    private boolean legacy$canDyeCollar(Entity entity, ItemStack item) {
        DyeColor color = Legacy4J.getDyeColorOrNull(item.getItem());
        if (color == null) return false;
        if (entity instanceof Wolf wolf) return wolf.getCollarColor() != color;
        return entity instanceof Cat cat && cat.getCollarColor() != color;
    }

    @Unique
    private boolean legacy$canEquipWolfArmor(Entity entity, ItemStack item) {
        if (!(entity instanceof Wolf wolf)) return false;
        //? if <1.20.5 {
        /*return false;
        *///?} else if <1.21.2 {
        return item.getItem() instanceof Equipable equipable && equipable.getEquipmentSlot() == EquipmentSlot.BODY && wolf.getItemBySlot(EquipmentSlot.BODY).isEmpty();
        //?} else {
        /*return item.has(DataComponents.EQUIPPABLE) && item.get(DataComponents.EQUIPPABLE).slot() == EquipmentSlot.BODY && wolf.getItemBySlot(EquipmentSlot.BODY).isEmpty();
        *///?}
    }

    @Inject(method = "startUseItem", at = @At("HEAD"), cancellable = true)
    private void startUseItem(CallbackInfo ci){
        legacy$suppressedUseAnimationHand = null;
        legacy$pauseShield();
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
        legacy$suppressedUseAnimationHand = null;
    }

    @Inject(method = "startUseItem", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/multiplayer/MultiPlayerGameMode;useItemOn(Lnet/minecraft/client/player/LocalPlayer;Lnet/minecraft/world/InteractionHand;Lnet/minecraft/world/phys/BlockHitResult;)Lnet/minecraft/world/InteractionResult;"))
    private void startUseItemCreativeBlockPlacing(CallbackInfo ci, @Local InteractionHand hand, @Local BlockHitResult hit){
        if (LegacyOptions.legacyCreativeBlockPlacing.get() && rightClickDelay == 4 && player.getAbilities().instabuild && ControlTooltip.canPlace(self(), player.getItemInHand(hand), hand)) {
            if (lastPlayerBlockUsePos == null) lastPlayerBlockUsePos = player.position();
            rightClickDelay = 0;
        }
        if (level.getBlockState(hit.getBlockPos()).getBlock() instanceof BedBlock || player.getAbilities().flying && player.isSprinting()) rightClickDelay = -1;
    }

    @Inject(method = "startUseItem", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/multiplayer/MultiPlayerGameMode;useItemOn(Lnet/minecraft/client/player/LocalPlayer;Lnet/minecraft/world/InteractionHand;Lnet/minecraft/world/phys/BlockHitResult;)Lnet/minecraft/world/InteractionResult;", shift = At.Shift.AFTER))
    private void rememberConduitRotation(CallbackInfo ci, @Local InteractionHand hand, @Local BlockHitResult hit) {
        if (!(player.getItemInHand(hand).getItem() instanceof BlockItem blockItem) || blockItem.getBlock() != Blocks.CONDUIT) return;
        BlockPlaceContext context = new BlockPlaceContext(player, hand, player.getItemInHand(hand), hit);
        if (level.getBlockState(context.getClickedPos()).is(Blocks.CONDUIT)) ConduitRotationCache.remember(level, context.getClickedPos(), player.getYRot());
    }

    @ModifyExpressionValue(method = "tick", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/player/LocalPlayer;isSleeping()Z"))
    private boolean tick(boolean original){
        return false;
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
    private void noSoundTick(SoundManager instance, boolean bl) {}

    @WrapWithCondition(method = "tick", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/sounds/MusicManager;tick()V"))
    private boolean tickMusicManager(MusicManager instance) {
        return LegacyMusicFader.musicManagerShouldTick;
    }

    @Inject(method = /*? if <1.20.3 {*//*"clearLevel(Lnet/minecraft/client/gui/screens/Screen;)V"*//*?} else if <1.21 {*//*"clearClientLevel"*//*?} else {*/"disconnect(Lnet/minecraft/client/gui/screens/Screen;Z)V"/*?}*/, at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/Gui;onDisconnected()V"))
    private void disconnectFadeMusic(CallbackInfo ci) {
        ConduitRotationCache.clear();
        SoundManagerAccessor.of(this.soundManager).fadeAllMusic();
    }

    @Redirect(method = "handleKeybinds", slice = @Slice(from = @At(value = "FIELD", target = "Lnet/minecraft/client/Options;keyInventory:Lnet/minecraft/client/KeyMapping;"), to = @At(value = "FIELD", target = "Lnet/minecraft/client/Options;keyAdvancements:Lnet/minecraft/client/KeyMapping;")), at = @At(value = "INVOKE", target = "Lnet/minecraft/client/KeyMapping;consumeClick()Z"))
    private boolean handleKeybindsInventoryKey(KeyMapping instance){
        boolean clicked = instance.consumeClick();
        if (clicked && !Legacy4JClient.consumeKeyboardToggleKeyPress(instance)) clicked = false;
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
        screen.focusRenderable(r-> r instanceof LegacyAdvancementsScreen.AdvancementButton b && b.id.equals(AdvancementToastAccessor.of(toast).getAdvancementId()), i-> screen.getTabList().tabButtons.get(i).onPress());
    }

    @WrapWithCondition(method = "handleKeybinds", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/Minecraft;setScreen(Lnet/minecraft/client/gui/screens/Screen;)V", ordinal = 1))
    private boolean handleKeybinds(Minecraft instance, Screen screen){
        if (screen instanceof ReplaceableScreen s) s.setCanReplace(false);
        return true;
    }
    @ModifyArg(method = "handleKeybinds", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/Minecraft;setScreen(Lnet/minecraft/client/gui/screens/Screen;)V", ordinal = 2))
    private Screen handleKeybinds(Screen arg){
        return new LegacyAdvancementsScreen(null);
    }

    @WrapWithCondition(method = "setScreen",at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/screens/Screen;removed()V"))
    private boolean removedScreen(Screen instance, Screen newScreen){
        return !(newScreen instanceof OverlayPanelScreen s) || s.parent != instance;
    }

    @Inject(method = "setScreen",at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/screens/Screen;added()V"))
    private void addedScreen(Screen screen, CallbackInfo ci){
        ControlTooltip.Event.of(screen).setupControlTooltips();
        ControlTooltip.Renderer.SCREEN_EVENT.invoker.accept(screen,ControlTooltip.Event.of(screen).getControlTooltips());
        LegacyTipManager.resetTipOffset(true);
    }

    @WrapWithCondition(method = "setScreen",at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/screens/Screen;init(Lnet/minecraft/client/Minecraft;II)V"))
    private boolean initScreen(Screen instance, Minecraft minecraft, int i, int j){
        if (oldScreen instanceof OverlayPanelScreen s && s.parent == instance) {
            instance.resize(minecraft, i, j);
            return false;
        }
        return true;
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
            ScreenUtil.lastGui = Util.getMillis();
            ControlTooltip.Event.of(gui).setupControlTooltips();
            ControlTooltip.Renderer.GUI_EVENT.invoker.accept(gui,ControlTooltip.Event.of(gui).getControlTooltips());
        }
    }

    @WrapWithCondition(method = "updateScreenAndTick",at = @At(value = "INVOKE",target = "Lnet/minecraft/client/sounds/SoundManager;stop()V"))
    public boolean updateScreenAndTick(SoundManager instance) {
        SoundManagerAccessor.of(instance).stopAllSound();
        return false;
    }

    @WrapWithCondition(method = "pauseGame",at = @At(value = "INVOKE",target = "Lnet/minecraft/client/sounds/SoundManager;pause()V"))
    public boolean pauseGame(SoundManager instance) {
        return false;
    }

    @ModifyArg(method = "setLevel",at = @At(value = "INVOKE",target = "Lnet/minecraft/client/Minecraft;updateScreenAndTick(Lnet/minecraft/client/gui/screens/Screen;)V"))
    public Screen setLevelLoadingScreen(Screen arg, @Local(argsOnly = true) ClientLevel newLevel) {
        return LegacyOptions.legacyLoadingAndConnecting.get() ? LegacyLoadingScreen.getDimensionChangeScreen(level, newLevel) : arg;
    }

    //? if >1.20.1 {
    @ModifyVariable(method = "buildInitialScreens", at = @At(value = "STORE"))
    private Runnable addInitialScreens(Runnable run) {
        return ()-> {
            run.run();
            if (screen != null) setScreen(ScreenUtil.getInitialScreen());
        };
    }
    //?} else {
    /*@Inject(method = "setInitialScreen", at = @At("HEAD"), cancellable = true)
    private void addInitialScreens(RealmsClient realmsClient, ReloadInstance reloadInstance, GameConfig.QuickPlayData quickPlayData, CallbackInfo ci) {
        if (!quickPlayData.isEnabled()) {
               ci.cancel();
               setScreen(ScreenUtil.getInitialScreen());
        }
    }
    *///?}
}
