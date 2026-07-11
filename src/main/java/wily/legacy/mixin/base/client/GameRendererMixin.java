package wily.legacy.mixin.base.client;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import com.llamalad7.mixinextras.injector.v2.WrapWithCondition;
import com.llamalad7.mixinextras.sugar.Local;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.joml.Matrix4f;
import org.objectweb.asm.Opcodes;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import wily.factoryapi.FactoryAPI;
import wily.legacy.client.LegacyGamma;
import wily.legacy.client.LegacyOptions;
import wily.legacy.client.LegacySaveCache;
import wily.legacy.client.ScreenshotToast;
import wily.legacy.entity.PlayerYBobbing;
import wily.legacy.util.LegacyBlockProtection;
import wily.legacy.util.client.LegacyRenderUtil;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Optional;
import java.util.function.Consumer;

@Mixin(GameRenderer.class)
public abstract class GameRendererMixin {
    @Unique
    private LegacyGamma legacy$gamma;

    @Shadow
    @Final
    private Minecraft minecraft;

    @Shadow
    private boolean hasWorldScreenshot;

    @Shadow
    protected abstract void takeAutoScreenshot(Path path);

    @Inject(method = "render", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/components/toasts/ToastManager;render(Lnet/minecraft/client/gui/GuiGraphics;)V", shift = At.Shift.AFTER))
    private void render(CallbackInfo ci, @Local(ordinal = 0) GuiGraphics graphics) {
        LegacyRenderUtil.renderGameOverlay(graphics);
        ScreenshotToast.render(graphics);
    }

    @Inject(method = "render", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/render/GuiRenderer;render(Lcom/mojang/blaze3d/buffers/GpuBufferSlice;)V", shift = At.Shift.AFTER))
    private void render(DeltaTracker deltaTracker, boolean bl, CallbackInfo ci) {
        if (!legacy$canRenderGamma()) return;
        if (legacy$gamma == null) legacy$gamma = new LegacyGamma();
        legacy$gamma.render();
    }

    @Unique
    private boolean legacy$canRenderGamma() {
        return minecraft.isGameLoadFinished() && LegacyOptions.displayLegacyGamma.get() && !FactoryAPI.isModLoaded("vulkanmod");
    }

    @Inject(method = "close", at = @At("HEAD"))
    private void legacy$closeGamma(CallbackInfo ci) {
        if (legacy$gamma == null) return;
        legacy$gamma.close();
        legacy$gamma = null;
    }

    @Inject(method = "bobView", at = @At("RETURN"))
    private void bobView(PoseStack poseStack, float f, CallbackInfo ci) {
        float xAngle = PlayerYBobbing.getAngle(minecraft, f);
        if (xAngle != 0) poseStack.mulPose(Axis.XP.rotationDegrees(xAngle));
    }

    @Inject(method = "shouldRenderBlockOutline", at = @At("HEAD"), cancellable = true)
    private void renderLevel(CallbackInfoReturnable<Boolean> cir) {
        if (!LegacyOptions.displayHUD.get()) cir.setReturnValue(false);
    }

    @WrapWithCondition(method = "renderLevel", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/renderer/GameRenderer;renderItemInHand(FZLorg/joml/Matrix4f;)V"))
    private boolean renderLevel(GameRenderer instance, float matrix4fstack, boolean b, Matrix4f f) {
        return LegacyOptions.displayHand.get();
    }

    @Inject(method = "pick", at = @At("RETURN"))
    private void pick(float tickDelta, CallbackInfo ci) {
        if (minecraft.level == null || minecraft.player == null || !(minecraft.hitResult instanceof BlockHitResult blockHit)) return;
        if (LegacyBlockProtection.blocksNetherPortalBreak(minecraft.level.getBlockState(blockHit.getBlockPos()))) {
            minecraft.hitResult = legacy$pickThroughNetherPortal(tickDelta);
        }
    }

    @Unique
    private HitResult legacy$pickThroughNetherPortal(float tickDelta) {
        Vec3 from = minecraft.player.getEyePosition(tickDelta);
        Vec3 to = from.add(minecraft.player.getViewVector(tickDelta).scale(minecraft.player.blockInteractionRange()));
        return minecraft.level.clip(new ClipContext(from, to, ClipContext.Block.OUTLINE, ClipContext.Fluid.NONE, minecraft.player) {
            @Override
            public VoxelShape getBlockShape(BlockState state, BlockGetter level, BlockPos pos) {
                if (LegacyBlockProtection.blocksNetherPortalBreak(state)) return Shapes.empty();
                return super.getBlockShape(state, level, pos);
            }
        });
    }

    @ModifyExpressionValue(method = "tryTakeScreenshotIfNeeded", at = @At(value = "FIELD", opcode = Opcodes.GETFIELD, target = "Lnet/minecraft/client/renderer/GameRenderer;hasWorldScreenshot:Z"))
    private boolean canTakeWorldIcon(boolean original) {
        return original && !LegacySaveCache.retakeWorldIcon;
    }

    @Redirect(method = "tryTakeScreenshotIfNeeded", at = @At(value = "INVOKE", target = "Ljava/util/Optional;ifPresent(Ljava/util/function/Consumer;)V"))
    private void tryTakeScreenshotIfNeeded(Optional<Path> instance, Consumer<? super Path> action) {
        instance.ifPresent(path -> {
                    if (!LegacySaveCache.retakeWorldIcon && Files.isRegularFile(path)) {
                        this.hasWorldScreenshot = true;
                    } else {
                        this.takeAutoScreenshot(path);
                        LegacySaveCache.retakeWorldIcon = false;
                    }
                }
        );
    }
}
