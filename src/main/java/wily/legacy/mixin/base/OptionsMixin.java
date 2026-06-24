package wily.legacy.mixin.base;

import com.google.common.collect.ImmutableList;
import net.minecraft.client.Minecraft;
import net.minecraft.client.Options;
import net.minecraft.server.packs.repository.PackRepository;
import net.minecraft.world.level.block.Blocks;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyArg;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import wily.legacy.Legacy4J;
import wily.legacy.Legacy4JClient;
import wily.legacy.client.GlobalPacks;
import wily.legacy.client.LegacyOptions;
import wily.legacy.client.PackAlbum;
import wily.legacy.skins.skin.DownloadedSkinPackStore;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.function.BooleanSupplier;

@Mixin(Options.class)
public abstract class OptionsMixin {
    private static final List<String> MANAGED_SKIN_PACKS = List.of("Legacy Custom Skinpacks", "Legacy Downloaded Skinpacks");

    @Shadow protected Minecraft minecraft;
    @Shadow public List<String> resourcePacks;
    @Shadow public List<String> incompatibleResourcePacks;

    @ModifyArg(method = "<init>", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/KeyMapping;<init>(Ljava/lang/String;ILjava/lang/String;)V", ordinal = 5),index = 0)
    protected String initKeyCraftingName(String string) {
        return "legacy.key.inventory";
    }

    @ModifyArg(method = "<init>", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/KeyMapping;<init>(Ljava/lang/String;ILjava/lang/String;)V", ordinal = 5),index = 1)
    protected int initKeyCrafting(int i) {
        return 73;
    }

    @ModifyArg(method = "<init>", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/OptionInstance;<init>(Ljava/lang/String;Lnet/minecraft/client/OptionInstance$TooltipSupplier;Lnet/minecraft/client/OptionInstance$CaptionBasedToString;Lnet/minecraft/client/OptionInstance$ValueSet;Ljava/lang/Object;Ljava/util/function/Consumer;)V", ordinal = /*? if >=1.21.2 {*//*7*//*?} else {*/6/*?}*/),index = 4)
    protected Object initChatSpacingOption(Object object) {
        return 1.0d;
    }

    @ModifyArg(method = "<init>", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/ToggleKeyMapping;<init>(Ljava/lang/String;ILjava/lang/String;Ljava/util/function/BooleanSupplier;)V", ordinal = 0),index = 3)
    protected BooleanSupplier initKeyShift(BooleanSupplier booleanSupplier) {
        return ()-> (minecraft == null || minecraft.player == null || (!minecraft.player.getAbilities().flying && minecraft.player.getVehicle() == null && (!minecraft.player.isInWater() || minecraft.player.onGround()) && !minecraft.player./*? if >=1.21 {*/getInBlockState/*?} else {*//*getFeetBlockState*//*?}*/().is(Blocks.SCAFFOLDING))) && (booleanSupplier.getAsBoolean() && !Legacy4JClient.controllerManager.isControllerTheLastInput() || LegacyOptions.controllerToggleCrouch.get() && Legacy4JClient.controllerManager.isControllerTheLastInput());
    }

    @ModifyArg(method = "<init>", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/ToggleKeyMapping;<init>(Ljava/lang/String;ILjava/lang/String;Ljava/util/function/BooleanSupplier;)V", ordinal = 1),index = 3)
    protected BooleanSupplier initKeySprint(BooleanSupplier booleanSupplier) {
        return ()-> booleanSupplier.getAsBoolean() && !Legacy4JClient.controllerManager.isControllerTheLastInput() || LegacyOptions.controllerToggleSprint.get() && Legacy4JClient.controllerManager.isControllerTheLastInput();
    }

    @Inject(method = "loadSelectedResourcePacks",at = @At("HEAD"), cancellable = true)
    private void loadSelectedResourcePacks(PackRepository packRepository, CallbackInfo ci){
        PackAlbum.init();
        GlobalPacks.STORAGE.load();
        List<String> savedResourcePacks = List.copyOf(resourcePacks);
        List<String> savedIncompatibleResourcePacks = List.copyOf(incompatibleResourcePacks);
        if (Legacy4J.isNewerVersion("1.8.0.2518.5", LegacyOptions.lastLoadedVersion.get()) && !LegacyOptions.lastLoadedVersion.isEmpty() && !GlobalPacks.globalResources.get().list().contains("legacy:legacy_resources"))
            GlobalPacks.globalResources.set(GlobalPacks.globalResources.get().withPacks(ImmutableList.<String>builder().addAll(GlobalPacks.globalResources.get().list()).add("legacy:legacy_resources").build()));
        GlobalPacks.globalResources.get().applyPacks(packRepository, PackAlbum.getDefaultResourceAlbum().packs());
        enableDownloadedSkinPack();
        restoreManagedSkinPacks(packRepository, savedResourcePacks, savedIncompatibleResourcePacks);
        PackAlbum.updateSavedResourcePacks();
        ci.cancel();
    }

    private void enableDownloadedSkinPack() {
        try {
            DownloadedSkinPackStore.enableResourcePack(minecraft);
        } catch (IOException ignored) {
        }
    }

    private void restoreManagedSkinPacks(PackRepository packRepository, List<String> savedResourcePacks, List<String> savedIncompatibleResourcePacks) {
        List<String> selected = new ArrayList<>(packRepository.getSelectedIds());
        boolean changed = false;
        for (String packName : MANAGED_SKIN_PACKS) {
            String fileId = "file/" + packName;
            if (!savedResourcePacks.contains(packName) && !savedResourcePacks.contains(fileId) && !savedIncompatibleResourcePacks.contains(packName) && !savedIncompatibleResourcePacks.contains(fileId)) continue;
            String resolved = packRepository.getPack(fileId) != null ? fileId : packRepository.getPack(packName) != null ? packName : null;
            if (resolved == null || selected.contains(resolved)) continue;
            selected.add(resolved);
            changed = true;
        }
        if (changed) packRepository.setSelected(selected);
    }

}
