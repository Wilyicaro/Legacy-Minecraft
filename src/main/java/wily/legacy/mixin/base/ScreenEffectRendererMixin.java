package wily.legacy.mixin.base;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.ScreenEffectRenderer;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.core.Direction;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import wily.factoryapi.util.FactoryScreenUtil;

import java.util.List;

@Mixin(ScreenEffectRenderer.class)
public abstract class ScreenEffectRendererMixin {

    @Shadow
    @Nullable
    private static BlockState getViewBlockingState(Player player) {
        return null;
    }

    @Shadow
    private static void renderTex(TextureAtlasSprite textureAtlasSprite, PoseStack poseStack/*? if >=1.21.4 {*/, MultiBufferSource multiBufferSource/*?}*/) {
    }

    @Redirect(method = "renderScreenEffect", at = @At(value = "INVOKE", target = /*? if <1.21.4 {*//*"Lnet/minecraft/client/renderer/ScreenEffectRenderer;renderTex(Lnet/minecraft/client/renderer/texture/TextureAtlasSprite;Lcom/mojang/blaze3d/vertex/PoseStack;)V"*//*?} else {*/"Lnet/minecraft/client/renderer/ScreenEffectRenderer;renderTex(Lnet/minecraft/client/renderer/texture/TextureAtlasSprite;Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/MultiBufferSource;)V"/*?}*/))
    private static void renderScreenEffect(TextureAtlasSprite f, PoseStack f1/*? if >=1.21.4 {*/, MultiBufferSource multiBufferSource/*?}*/, Minecraft minecraft) {
        BlockState state = getViewBlockingState(minecraft.player);
        List<BakedQuad> quads = minecraft.getBlockRenderer().getBlockModelShaper().getBlockModel(state).getQuads(state, Direction.UP, minecraft.player.getRandom());
        if (!quads.isEmpty()) {
            BakedQuad quad = quads.get(0);
            f = quad.getSprite();
            int color = minecraft.getBlockColors().getColor(state, minecraft.level, minecraft.player.blockPosition(), quad.getTintIndex());
            RenderSystem.setShaderColor(FactoryScreenUtil.getRed(color), FactoryScreenUtil.getGreen(color), FactoryScreenUtil.getBlue(color), 1.0f);
        }
        renderTex(f,f1/*? if >=1.21.4 {*/, multiBufferSource/*?}*/);
        RenderSystem.setShaderColor(1.0f,1.0f,1.0f, 1.0f);
    }
}
