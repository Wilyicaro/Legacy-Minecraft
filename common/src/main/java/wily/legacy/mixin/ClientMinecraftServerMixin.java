package wily.legacy.mixin;

import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.profiling.ProfilerFiller;
import net.minecraft.world.level.storage.LevelStorageSource;
import org.objectweb.asm.Opcodes;
import org.slf4j.Logger;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import wily.legacy.Legacy4JClient;
import wily.legacy.client.CommonColor;
import wily.legacy.network.TopMessage;
import wily.legacy.util.ScreenUtil;

import java.util.function.BooleanSupplier;

@Mixin(MinecraftServer.class)
public abstract class ClientMinecraftServerMixin {

    @Shadow @Final public LevelStorageSource.LevelStorageAccess storageSource;

    @Shadow private int tickCount;
    @Shadow @Final private static Logger LOGGER;

    @Shadow private ProfilerFiller profiler;

    @Shadow public abstract boolean saveEverything(boolean bl, boolean bl2, boolean bl3);

    @Redirect(method = "tickServer", at = @At(value = "FIELD", target = "Lnet/minecraft/server/MinecraftServer;tickCount:I", opcode = Opcodes.GETFIELD, ordinal = 1))
    private int tickServer(MinecraftServer instance){
        return 1;
    }
    @Unique
    private int autoSaveInterval(){
        return 6000 * ScreenUtil.getLegacyOptions().autoSaveInterval().get();
    }
    @Inject(method = "tickServer", at = @At(value = "FIELD", target = "Lnet/minecraft/server/MinecraftServer;tickCount:I", opcode = Opcodes.PUTFIELD, ordinal = 0, shift = At.Shift.AFTER))
    private void tickServer(BooleanSupplier booleanSupplier, CallbackInfo ci){
        if (ScreenUtil.getLegacyOptions().autoSaveInterval().get() == 0) return;
        int t = autoSaveInterval() - (tickCount % autoSaveInterval());
        t = t == autoSaveInterval() ? 0 : t;
        if (ScreenUtil.getLegacyOptions().autoSaveInterval().get() > 0 && t <= 100 && t % 20 == 0) TopMessage.medium = t == 0 ? null : new TopMessage(Component.translatable("legacy.menu.autosave_countdown", t / 20),CommonColor.INVENTORY_GRAY_TEXT.get());
        if (this.tickCount % autoSaveInterval() == 0) {
            LOGGER.debug("Autosave started");
            this.profiler.push("save");
            this.saveEverything(true, false, false);
            this.profiler.pop();
            LOGGER.debug("Autosave finished");
        }
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
        Legacy4JClient.saveLevel(storageSource);
    }
}
