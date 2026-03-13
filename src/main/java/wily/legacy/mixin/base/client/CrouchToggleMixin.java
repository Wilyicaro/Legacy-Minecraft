package wily.legacy.mixin.base.client;

import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.ToggleKeyMapping;
import net.minecraft.world.level.block.Blocks;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import wily.legacy.client.LegacyOptions;

import java.util.function.BooleanSupplier;

@Mixin(ToggleKeyMapping.class)
public abstract class CrouchToggleMixin extends KeyMapping {
    @Shadow @Final private BooleanSupplier needsToggle;
    @Shadow @Final private boolean shouldRestore;
    @Shadow private boolean releasedByScreenWhenDown;
    @Unique
    private boolean legacy$crouchToggleActive;
    @Unique
    private boolean legacy$restoreCrouchAfterScreenClose;

    protected CrouchToggleMixin(String string, InputConstants.Type type, int i, KeyMapping.Category category) {
        super(string, type, i, category);
    }

    @Inject(method = "setDown", at = @At("TAIL"))
    private void setDown(boolean down, CallbackInfo ci) {
        if (!legacy$isShiftKey()) return;
        if (needsToggle.getAsBoolean()) {
            if (down) legacy$crouchToggleActive = isDown();
        } else if (!down) {
            legacy$crouchToggleActive = false;
        }
    }

    @Inject(method = "release", at = @At("HEAD"), cancellable = true)
    private void release(CallbackInfo ci) {
        if (!legacy$isShiftKey()) return;
        if (isDown() && legacy$canCrouch(Minecraft.getInstance())) {
            releasedByScreenWhenDown = false;
            legacy$restoreCrouchAfterScreenClose = false;
            ci.cancel();
            return;
        }
        legacy$restoreCrouchAfterScreenClose = legacy$crouchToggleActive;
    }

    @Inject(method = "shouldRestoreStateOnScreenClosed", at = @At("HEAD"), cancellable = true)
    private void shouldRestoreStateOnScreenClosed(CallbackInfoReturnable<Boolean> cir) {
        if (!legacy$isShiftKey()) return;
        boolean restore = shouldRestore
                && key.getType() == InputConstants.Type.KEYSYM
                && releasedByScreenWhenDown
                && legacy$restoreCrouchAfterScreenClose
                && legacy$canCrouch(Minecraft.getInstance());
        releasedByScreenWhenDown = false;
        legacy$restoreCrouchAfterScreenClose = false;
        cir.setReturnValue(restore);
    }

    @Inject(method = "shouldSetOnIngameFocus", at = @At("HEAD"), cancellable = true)
    private void shouldSetOnIngameFocus(CallbackInfoReturnable<Boolean> cir) {
        if (legacy$isShiftKey() && legacy$crouchToggleActive) cir.setReturnValue(false);
    }

    @Unique
    private boolean legacy$isShiftKey() {
        Minecraft minecraft = Minecraft.getInstance();
        return minecraft != null && minecraft.options != null && minecraft.options.keyShift == this;
    }

    @Unique
    private static boolean legacy$canCrouch(Minecraft minecraft) {
        return minecraft != null
                && minecraft.player != null
                && !minecraft.player.getAbilities().flying
                && minecraft.player.getVehicle() == null
                && (!minecraft.player.isInWater() || minecraft.player.onGround())
                && !minecraft.player./*? if >=1.21 {*/getInBlockState/*?} else {*//*getFeetBlockState*//*?}*/().is(Blocks.SCAFFOLDING);
    }
}
