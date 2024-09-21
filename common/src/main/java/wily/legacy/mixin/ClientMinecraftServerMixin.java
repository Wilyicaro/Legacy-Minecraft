package wily.legacy.mixin;

import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.storage.LevelStorageSource;
import org.objectweb.asm.Opcodes;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import wily.legacy.Legacy4JClient;
import wily.legacy.client.CommonColor;
import wily.legacy.network.TopMessage;
import wily.legacy.util.ScreenUtil;

import java.util.concurrent.CompletableFuture;
import java.util.function.BooleanSupplier;

@Mixin(MinecraftServer.class)
public abstract class ClientMinecraftServerMixin {

    @Shadow @Final public LevelStorageSource.LevelStorageAccess storageSource;

    @Shadow private int ticksUntilAutosave;

    @Shadow private volatile boolean isSaving;

    @Shadow public abstract Iterable<ServerLevel> getAllLevels();

    @Inject(method = "computeNextAutosaveInterval", at = @At("RETURN"), cancellable = true)
    private void tickServer(CallbackInfoReturnable<Integer> cir){
        cir.setReturnValue(ScreenUtil.getLegacyOptions().autoSaveInterval().get() > 0 && !Minecraft.getInstance().isDemo() ? ScreenUtil.getLegacyOptions().autoSaveInterval().get() * cir.getReturnValue() : 1);
    }
    @Inject(method = "tickServer", at = @At(value = "FIELD", target = "Lnet/minecraft/server/MinecraftServer;ticksUntilAutosave:I", opcode = Opcodes.PUTFIELD, ordinal = 0, shift = At.Shift.AFTER))
    private void tickServer(BooleanSupplier booleanSupplier, CallbackInfo ci){
        if (ScreenUtil.getLegacyOptions().autoSaveInterval().get() > 0 && ticksUntilAutosave >= 0 && ticksUntilAutosave <= 100) {
            if (ticksUntilAutosave % 20 == 0) TopMessage.medium = Component.translatable("legacy.menu.autosave_countdown", ticksUntilAutosave / 20).withColor(CommonColor.INVENTORY_GRAY_TEXT.get());
        }else TopMessage.medium = null;
    }
    @Redirect(method = "tickServer", at = @At(value = "FIELD", target = "Lnet/minecraft/server/MinecraftServer;ticksUntilAutosave:I", opcode = Opcodes.GETFIELD, ordinal = 1))
    private int tickServer(MinecraftServer instance){
        return ScreenUtil.getLegacyOptions().autoSaveInterval().get() > 0 ? ticksUntilAutosave : 1;
    }
    @Inject(method = "stopServer", at = @At("RETURN"))
    private void stopServer(CallbackInfo ci){
        if (Legacy4JClient.saveExit) {
            Legacy4JClient.saveExit = false;
            Legacy4JClient.saveLevel(storageSource);
        }
    }
    @Inject(method = "saveEverything", at = @At("RETURN"))
    public void saveEverything(boolean bl, boolean bl2, boolean bl3, CallbackInfoReturnable<Boolean> cir) {
        CompletableFuture.runAsync(()->{
            isSaving = true;
            Iterable<ServerLevel> levels = getAllLevels();
            levels.forEach(l->l.noSave = true);
            Legacy4JClient.saveLevel(storageSource);
            levels.forEach(l->l.noSave = false);
            isSaving = false;
        });
    }
    @Inject(method = "stopServer", at = @At("HEAD"))
    private void stopServerHead(CallbackInfo ci){
        while (isSaving) {
            Thread.onSpinWait();
        }
    }
}
