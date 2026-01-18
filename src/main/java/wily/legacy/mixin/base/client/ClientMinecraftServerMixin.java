package wily.legacy.mixin.base.client;

import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import com.mojang.datafixers.DataFixer;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.Services;
import net.minecraft.server.WorldStem;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.progress.LevelLoadListener;
import net.minecraft.server.packs.repository.PackRepository;
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
import wily.legacy.client.CommonColor;
import wily.legacy.client.LegacyOptions;
import wily.legacy.client.LegacySaveCache;
import wily.legacy.client.screen.LegacyLoadingScreen;
import wily.legacy.network.TopMessage;

import java.net.Proxy;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.function.BooleanSupplier;

@Mixin(MinecraftServer.class)
public abstract class ClientMinecraftServerMixin {

    @Shadow
    @Final
    public LevelStorageSource.LevelStorageAccess storageSource;

    @Shadow
    private volatile boolean isSaving;
    @Shadow
    @Final
    private Executor executor;
    //? if >1.20.2 {
    @Shadow
    private int ticksUntilAutosave;

    @Shadow
    public abstract Iterable<ServerLevel> getAllLevels();

    @Inject(method = "<init>", at = @At("RETURN"))
    private void init(Thread thread, LevelStorageSource.LevelStorageAccess levelStorageAccess, PackRepository packRepository, WorldStem worldStem, Proxy proxy, DataFixer dataFixer, Services services, LevelLoadListener levelLoadListener, CallbackInfo ci) {
        ticksUntilAutosave *= Math.max(1, LegacyOptions.autoSaveInterval.get());
    }

    @ModifyReturnValue(method = "computeNextAutosaveInterval", at = @At("RETURN"))
    private int tickServer(int original) {
        return original * Math.max(1, LegacyOptions.autoSaveInterval.get());
    }

    @Inject(method = "tickServer", at = @At(value = "FIELD", target = "Lnet/minecraft/server/MinecraftServer;ticksUntilAutosave:I", opcode = Opcodes.PUTFIELD, ordinal = 0, shift = At.Shift.AFTER))
    private void tickServer(BooleanSupplier booleanSupplier, CallbackInfo ci) {
        if (LegacyOptions.autoSaveInterval.get() == 0 || Minecraft.getInstance().isDemo()) {
            ticksUntilAutosave++;
            return;
        }
        if (!LegacyOptions.autoSaveCountdown.get()) return;

        if (ticksUntilAutosave >= 20 && ticksUntilAutosave <= 120) {
            if (ticksUntilAutosave % 20 == 0)
                TopMessage.setMedium(new TopMessage(Component.translatable("legacy.menu.autoSave_countdown", ticksUntilAutosave / 20 - 1), CommonColor.INVENTORY_GRAY_TEXT.get(), 21, false, false, false));
        }

        if (ticksUntilAutosave <= 0) {
            Minecraft minecraft = Minecraft.getInstance();
            if (minecraft.screen == null) minecraft.execute(LegacyLoadingScreen::startFakeAutoSave);
            else ticksUntilAutosave++;
        }
    }

    @Redirect(method = "tickServer", at = @At(value = "FIELD", target = "Lnet/minecraft/server/MinecraftServer;ticksUntilAutosave:I", opcode = Opcodes.GETFIELD, ordinal = 1))
    private int tickServer(MinecraftServer instance) {
        return LegacyOptions.autoSaveInterval.get() > 0 && !Minecraft.getInstance().isDemo() ? ticksUntilAutosave : 1;
    }

    //?} else {
    /*@Shadow private int tickCount;

    @Shadow @Final private static Logger LOGGER;

    @Shadow private ProfilerFiller profiler;


    @Shadow public abstract boolean saveEverything(boolean bl, boolean bl2, boolean bl3);

    @Redirect(method = "tickServer", at = @At(value = "FIELD", target = "Lnet/minecraft/server/MinecraftServer;tickCount:I", opcode = Opcodes.GETFIELD, ordinal = 1))
    private int tickServer(MinecraftServer instance){
        return 1;
    }
    @Unique
    private int autoSaveInterval(){
        return 6000 * LegacyOptions.autoSaveInterval.get();
    }
    @Inject(method = "tickServer", at = @At(value = "FIELD", target = "Lnet/minecraft/server/MinecraftServer;tickCount:I", opcode = Opcodes.PUTFIELD, ordinal = 0, shift = At.Shift.AFTER))
    private void tickServer(BooleanSupplier booleanSupplier, CallbackInfo ci){
        int t = LegacyOptions.autoSaveInterval.get() == 0 ? 0 : autoSaveInterval() - (tickCount % autoSaveInterval());
        t = t == autoSaveInterval() ? 0 : t;
        if (LegacyOptions.autoSaveCountdown.get() && LegacyOptions.autoSaveInterval.get() > 0 && t >= 0 && t <= 100) {
            if (t % 20 == 0) TopMessage.setMedium(new TopMessage(Component.translatable("legacy.menu.autoSave_countdown", t / 20), CommonColor.INVENTORY_GRAY_TEXT.get(), 21, false, false, false));
        }
        if (LegacyOptions.autoSaveInterval.get() == 0 || Minecraft.getInstance().isDemo()) return;
        if (this.tickCount % autoSaveInterval() == 0) {
            LOGGER.debug("Autosave started");
            this.profiler.push("save");
            this.saveEverything(true, false, false);
            this.profiler.pop();
            LOGGER.debug("Autosave finished");
        }
    }
    *///?}
    @Inject(method = "stopServer", at = @At("RETURN"))
    private void stopServer(CallbackInfo ci) {
        if (LegacySaveCache.saveExit) {
            LegacySaveCache.saveExit = false;
            LegacySaveCache.saveLevel(storageSource);
        }
    }

    @Inject(method = "saveEverything", at = @At("RETURN"))
    public void saveEverything(boolean bl, boolean bl2, boolean bl3, CallbackInfoReturnable<Boolean> cir) {
        if (!LegacySaveCache.isCurrentWorldSource(storageSource)) return;
        CompletableFuture.runAsync(() -> {
            isSaving = true;
            Iterable<ServerLevel> levels = getAllLevels();
            levels.forEach(l -> l.noSave = true);
            LegacySaveCache.saveLevel(storageSource);
            levels.forEach(l -> l.noSave = false);
            isSaving = false;
        }, executor);
    }

    @Inject(method = "stopServer", at = @At("HEAD"))
    private void stopServerHead(CallbackInfo ci) {
        while (isSaving) {
            Thread.onSpinWait();
        }
    }
}
