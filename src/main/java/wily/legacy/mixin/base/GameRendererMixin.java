package wily.legacy.mixin.base;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import com.llamalad7.mixinextras.injector.v2.WrapWithCondition;
import com.llamalad7.mixinextras.sugar.Local;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.core.BlockPos;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.util.Mth;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
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
import wily.legacy.Legacy4JClient;
import wily.legacy.client.LegacyOptions;
import wily.legacy.client.ScreenshotToast;
import wily.legacy.entity.PlayerYBobbing;
import wily.legacy.util.LegacyBlockProtection;
import wily.legacy.util.ScreenUtil;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Optional;
import java.util.function.Consumer;

@Mixin(GameRenderer.class)
public abstract class GameRendererMixin {
    @Shadow @Final
    Minecraft minecraft;

    @Shadow private boolean hasWorldScreenshot;

    @Shadow protected abstract void takeAutoScreenshot(Path path);

    @Shadow private ItemStack itemActivationItem;

    @Inject(method = "render", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/GuiGraphics;flush()V"))
    private void render(CallbackInfo ci, @Local(ordinal = 0) GuiGraphics graphics){
        ScreenUtil.renderGameOverlay(graphics);
    }

    @Inject(method = "render", at = @At(value = "INVOKE", target = /*? if <1.21.2 {*/"Lnet/minecraft/client/gui/components/toasts/ToastComponent;render(Lnet/minecraft/client/gui/GuiGraphics;)V"/*?} else {*//*"Lnet/minecraft/client/gui/components/toasts/ToastManager;render(Lnet/minecraft/client/gui/GuiGraphics;)V"*//*?}*/, shift = At.Shift.AFTER))
    private void renderScreenshotToast(CallbackInfo ci, @Local(ordinal = 0) GuiGraphics graphics){
        ScreenshotToast.render(graphics);
    }

    @Inject(method = "tick", at = @At("RETURN"))
    private void tick(CallbackInfo ci){
        if (itemActivationItem == null && Legacy4JClient.itemActivationRenderReplacement != null) Legacy4JClient.itemActivationRenderReplacement = null;
    }

    //? if <1.21.1 {
    @ModifyExpressionValue(method = "renderLevel", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/Camera;getYRot()F"))
    private float applyFlyingViewYRotation(float yRot) {
        float base = -yRot * Mth.DEG_TO_RAD;
        return yRot - (ScreenUtil.getFlyingViewYRotation(base) - base) / Mth.DEG_TO_RAD;
    }
    //?}

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
        double reach = /*? if <1.20.5 {*//*minecraft.gameMode == null ? 0.0 : minecraft.gameMode.getPickRange()*//*?} else {*/minecraft.player.blockInteractionRange()/*?}*/;
        Vec3 to = from.add(minecraft.player.getViewVector(tickDelta).scale(reach));
        return minecraft.level.clip(new ClipContext(from, to, ClipContext.Block.OUTLINE, ClipContext.Fluid.NONE, minecraft.player) {
            @Override
            public VoxelShape getBlockShape(BlockState state, BlockGetter level, BlockPos pos) {
                if (LegacyBlockProtection.blocksNetherPortalBreak(state)) return Shapes.empty();
                return super.getBlockShape(state, level, pos);
            }
        });
    }

    //? if >=1.20.5 && <1.21.2 {
    @Redirect(method = "renderItemActivationAnimation", at = @At(value = "NEW", target = "()Lcom/mojang/blaze3d/vertex/PoseStack;"))
    private PoseStack renderItemActivationAnimation(GuiGraphics graphics){
        return graphics.pose();
    }
    //?}

    //? if >=1.20.5 {
    @Inject(method = "renderItemActivationAnimation", at = @At(value = "INVOKE", target = /*? if <1.21.2 {*/"Lnet/minecraft/client/gui/GuiGraphics;drawManaged(Ljava/lang/Runnable;)V"/*?} else {*//*"Lnet/minecraft/client/gui/GuiGraphics;drawSpecial(Ljava/util/function/Consumer;)V"*//*?}*/), cancellable = true)
    private void renderItemActivationAnimation(GuiGraphics guiGraphics, float f, CallbackInfo ci){
        if (Legacy4JClient.itemActivationRenderReplacement != null){
            ci.cancel();
            Legacy4JClient.itemActivationRenderReplacement.render(guiGraphics,0,0,f);
            guiGraphics.pose().popPose();
        }
    }
    //?} else {
    /*@WrapWithCondition(method = "render", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/renderer/GameRenderer;renderItemActivationAnimation(IIF)V"))
    private boolean renderItemActivationAnimation(GameRenderer instance, int i, int j, float f){
        return Legacy4JClient.itemActivationRenderReplacement == null;
    }

    @Inject(method = "renderLevel", at = @At("HEAD"))
    private void applyFlyingViewRolling(float f, long l, PoseStack poseStack, CallbackInfo ci){
        float z = ScreenUtil.getFlyingViewRollingRotation(0);
        if (z != 0){
            poseStack.mulPose(Axis.ZN.rotation(z));
        }
    }
    *///?}
    @Inject(method = "bobView", at = @At("RETURN"))
    private void bobView(PoseStack poseStack, float f, CallbackInfo ci){
        float xAngle = PlayerYBobbing.getAngle(minecraft, f);
        if (xAngle != 0) poseStack.mulPose(Axis.XP.rotationDegrees(xAngle));
    }

    @Inject(method = "shouldRenderBlockOutline", at = @At("HEAD"), cancellable = true)
    private void renderLevel(CallbackInfoReturnable<Boolean> cir){
        if (!LegacyOptions.displayHUD.get()) cir.setReturnValue(false);
    }

    @ModifyExpressionValue(method = "renderLevel", at = @At(value = "FIELD", target = "Lnet/minecraft/client/renderer/GameRenderer;renderHand:Z"))
    private boolean renderLevel(boolean original){
        return original && LegacyOptions.displayHand.get();
    }

    @ModifyExpressionValue(method = "tryTakeScreenshotIfNeeded",at = @At(value = "FIELD", opcode = Opcodes.GETFIELD, target = "Lnet/minecraft/client/renderer/GameRenderer;hasWorldScreenshot:Z"))
    private boolean canTakeWorldIcon(boolean original) {
        return original && !Legacy4JClient.retakeWorldIcon;
    }

    @Redirect(method = "tryTakeScreenshotIfNeeded",at = @At(value = "INVOKE", target = "Ljava/util/Optional;ifPresent(Ljava/util/function/Consumer;)V"))
    private void tryTakeScreenshotIfNeeded(Optional<Path> instance, Consumer<? super Path> action) {
        instance.ifPresent(path->{
                    if (!Legacy4JClient.retakeWorldIcon && Files.isRegularFile(path)) {
                        this.hasWorldScreenshot = true;
                    } else {
                        this.takeAutoScreenshot(path);
                        Legacy4JClient.retakeWorldIcon = false;
                    }
                }
        );
    }

    //? if <1.21.2 {
    @Inject(method = "resize",at = @At("RETURN"))
    public void resize(int i, int j, CallbackInfo ci) {
        if (Legacy4JClient.gammaEffect != null) Legacy4JClient.gammaEffect.resize(i,j);
    }
    //?}
}
