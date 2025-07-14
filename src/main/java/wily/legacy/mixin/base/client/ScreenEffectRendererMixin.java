package wily.legacy.mixin.base.client;

import com.llamalad7.mixinextras.sugar.Local;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.ScreenEffectRenderer;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;
import org.apache.commons.lang3.tuple.Pair;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyArg;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import wily.factoryapi.util.ColorUtil;
import wily.legacy.client.LegacyActivationAnim;

import java.util.Collections;
import java.util.List;

@Mixin(ScreenEffectRenderer.class)
public abstract class ScreenEffectRendererMixin {

    @Shadow private ItemStack itemActivationItem;

    @Unique
    private static int texRenderColor = 0xFFFFFFFF;

    @Shadow @Final private Minecraft minecraft;

    @Shadow @Final private MultiBufferSource bufferSource;

    @ModifyArg(method = "renderScreenEffect", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/renderer/ScreenEffectRenderer;renderTex(Lnet/minecraft/client/renderer/texture/TextureAtlasSprite;Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/MultiBufferSource;)V"))
    private TextureAtlasSprite renderScreenEffect(TextureAtlasSprite f, PoseStack i, MultiBufferSource f1, @Local /*? if neoforge {*//*Pair<BlockState, BlockPos> pair*//*?} else {*/BlockState state/*?}*/) {
        //? if neoforge {
        /*BlockState state = pair.getLeft();
        *///?}
        List<BakedQuad> quads = Collections.emptyList();
        var parts = minecraft.getBlockRenderer().getBlockModelShaper().getBlockModel(state).collectParts(minecraft.player.getRandom());
        if (!parts.isEmpty()) quads = parts.get(0).getQuads(Direction.UP);
        if (!quads.isEmpty()) {
            BakedQuad quad = quads.get(0);
            f = quad.sprite();
            texRenderColor = ColorUtil.withAlpha(minecraft.getBlockColors().getColor(state, minecraft.level, minecraft.player.blockPosition(), quad.tintIndex()), 1.0f);
        } else texRenderColor = 0xFFFFFFFF;

        return f;
    }

    @ModifyArg(method = "renderTex", at = @At(value = "INVOKE", target = "Lcom/mojang/blaze3d/vertex/VertexConsumer;setColor(I)Lcom/mojang/blaze3d/vertex/VertexConsumer;"))
    private static int renderTex(int i){
        return ColorUtil.mergeColors(texRenderColor, i);
    }

    @Inject(method = "tick", at = @At("RETURN"))
    private void tick(CallbackInfo ci){
        if (itemActivationItem == null && LegacyActivationAnim.itemActivationRenderReplacement != null) LegacyActivationAnim.itemActivationRenderReplacement = null;
    }

    @Inject(method = "renderItemActivationAnimation", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/renderer/entity/ItemRenderer;renderStatic(Lnet/minecraft/world/item/ItemStack;Lnet/minecraft/world/item/ItemDisplayContext;IILcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/MultiBufferSource;Lnet/minecraft/world/level/Level;I)V"), cancellable = true)
    private void renderItemActivationAnimation(PoseStack poseStack, float f, CallbackInfo ci){
        if (LegacyActivationAnim.itemActivationRenderReplacement != null){
            ci.cancel();
            LegacyActivationAnim.itemActivationRenderReplacement.render(poseStack, f, bufferSource);
            poseStack.popPose();
        }
    }

}
