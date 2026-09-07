package wily.legacy.mixin.base.client;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.worldselection.FileFixerProgressScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.util.worldupdate.UpgradeProgress;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import wily.factoryapi.base.client.UIAccessor;
import wily.legacy.client.LegacyOptions;
import wily.legacy.client.screen.LegacyLoading;

@Mixin(FileFixerProgressScreen.class)
public abstract class FileFixerProgressScreenMixin extends Screen implements LegacyLoading {
    @Shadow
    @Final
    private UpgradeProgress upgradeProgress;
    @Shadow
    private Button cancelButton;

    protected FileFixerProgressScreenMixin(Component title) {
        super(title);
    }

    @Inject(method = "init", at = @At("TAIL"))
    private void legacy$positionCancelButton(CallbackInfo ci) {
        if (LegacyOptions.legacyLoadingAndConnecting.get()) cancelButton.setY(height - 25);
    }

    @Inject(method = "extractRenderState", at = @At("HEAD"), cancellable = true)
    private void legacy$renderUpgradeProgress(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick, CallbackInfo ci) {
        if (!LegacyOptions.legacyLoadingAndConnecting.get()) return;

        UpgradeProgress.FileFixStats stats = upgradeProgress.getTypeFileFixStats();
        UpgradeProgress.Type type = upgradeProgress.getType();
        Component stage = Component.translatable("upgradeWorld.info.scanning");
        if (stats.totalOperations() > 0 && type != null) {
            stage = type == UpgradeProgress.Type.REGIONS ? Component.translatable("upgradeWorld.progress.type.region") : type.label();
        }
        getLoadingRenderer().prepareRender(minecraft, UIAccessor.of(this), getTitle(), stage, stats.getProgress(), false);
        getLoadingRenderer().extractRenderState(graphics, mouseX, mouseY, partialTick);
        super.extractRenderState(graphics, mouseX, mouseY, partialTick);
        ci.cancel();
    }
}
