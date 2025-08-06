package wily.legacy.mixin.base.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.renderer.state.MapRenderState;
import net.minecraft.client.renderer.MapRenderer;
import wily.legacy.client.LegacyMapDecorationRenderState;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.resources.language.I18n;
import net.minecraft.core.Holder;
import net.minecraft.world.level.saveddata.maps.*;
import org.joml.Matrix4f;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import wily.legacy.Legacy4JClient;
import wily.legacy.client.LegacyOptions;
import wily.legacy.client.PlayerIdentifier;
import wily.legacy.entity.LegacyPlayerInfo;

import java.util.Iterator;
import java.util.List;

@Mixin(MapRenderer.class)
public abstract class MapRendererMixin {
    @Inject(method = "extractRenderState", at = @At("RETURN"))
    private void extractRenderState(MapId mapId, MapItemSavedData mapItemSavedData, MapRenderState mapRenderState, CallbackInfo ci){
        int i = 0;
        for (MapDecoration decoration : mapItemSavedData.getDecorations()) {
            LegacyMapDecorationRenderState.of(mapRenderState.decorations.get(i)).extractRenderState(decoration);
            i++;
        }
    }

    @Unique
    private boolean isPlayerDecoration(Holder<MapDecorationType> type){
        return type.equals(MapDecorationTypes.PLAYER) || type.equals(MapDecorationTypes.PLAYER_OFF_MAP) || type.equals(MapDecorationTypes.PLAYER_OFF_LIMITS);
    }

    @Redirect(method = "render", at = @At(value = "INVOKE", target = "Ljava/util/List;iterator()Ljava/util/Iterator;"))
    Iterator<MapRenderState.MapDecorationRenderState> drawDecorations(List<MapRenderState.MapDecorationRenderState> iterable){
        return iterable.stream().filter(s-> !isPlayerDecoration(LegacyMapDecorationRenderState.of(s).getType())).iterator();
    }

    @Inject(method = "render", at = @At("HEAD"))
    void draw(MapRenderState mapRenderState, PoseStack poseStack, MultiBufferSource multiBufferSource, boolean bl, int i, CallbackInfo ci) {
        Minecraft minecraft = Minecraft.getInstance();
        Font font = minecraft.font;
        if (!bl && LegacyOptions.mapsWithCoords.get()) {
            poseStack.pushPose();
            poseStack.translate(-0.2f,0.4f,-0.1f);
            poseStack.scale(1f, 0.95f, 1);
            font.drawInBatch(I18n.get("legacy.map.coords",(int)minecraft.player.getX(), (int)minecraft.player.getEyeY(),(int)minecraft.player.getZ()), 0.0f, 0.0f, 0, false, poseStack.last().pose(), multiBufferSource, Font.DisplayMode.NORMAL, 0, i);
            poseStack.popPose();
        }
    }

    @Inject(method = "render", at = @At("RETURN"))
    void drawReturn(MapRenderState mapRenderState, PoseStack poseStack, MultiBufferSource multiBufferSource, boolean bl, int i, CallbackInfo ci) {
        int l = 0;
        for (MapRenderState.MapDecorationRenderState mapDecoration : mapRenderState.decorations) {
            Holder<MapDecorationType> type = LegacyMapDecorationRenderState.of(mapDecoration).getType();
            if ((bl && !mapDecoration.renderOnFrame) || !isPlayerDecoration(type)) continue;
            poseStack.pushPose();
            poseStack.translate(0.0f + (float)mapDecoration.x / 2.0f + 64.0f, 0.0f + (float)mapDecoration.y / 2.0f + 64.0f, -0.02f);
            poseStack.mulPose(Axis.ZP.rotationDegrees((float)(mapDecoration.rot * 360) / 16.0f));
            poseStack.scale(4.0f, 4.0f, 3.0f);
            poseStack.translate(-0.125f, 0.125f, 0.0f);
            Matrix4f matrix4f2 = poseStack.last().pose();
            Minecraft minecraft = Minecraft.getInstance();
            LegacyPlayerInfo playerInfo = mapDecoration.name == null || minecraft.getConnection() == null || !(minecraft.getConnection().getPlayerInfo(mapDecoration.name.getString()) instanceof LegacyPlayerInfo info) ? null : info;
            float[] color = playerInfo == null ? new float[]{1.0f,1.0f,1.0f} : Legacy4JClient.getVisualPlayerColor(playerInfo);
            TextureAtlasSprite textureAtlasSprite = playerInfo == null ? mapDecoration.atlasSprite : minecraft.getMapDecorationTextures().textureAtlas.getSprite(PlayerIdentifier.of(playerInfo.getIdentifierIndex()).spriteByMapDecorationType(type));
            float g = textureAtlasSprite.getU0();
            float h = textureAtlasSprite.getV0();
            float m = textureAtlasSprite.getU1();
            float n = textureAtlasSprite.getV1();
            VertexConsumer vertexConsumer2 = multiBufferSource.getBuffer(RenderType.text(textureAtlasSprite.atlasLocation()));
            vertexConsumer2.addVertex(matrix4f2, -1.0f, 1.0f, (float)l * -0.001f).setColor(color[0], color[1], color[2], 1.0f).setUv(g, h).setLight(i);
            vertexConsumer2.addVertex(matrix4f2, 1.0f, 1.0f, (float)l * -0.001f).setColor(color[0], color[1], color[2], 1.0f).setUv(m, h).setLight(i);
            vertexConsumer2.addVertex(matrix4f2, 1.0f, -1.0f, (float)l * -0.001f).setColor(color[0], color[1], color[2], 1.0f).setUv(m, n).setLight(i);
            vertexConsumer2.addVertex(matrix4f2, -1.0f, -1.0f, (float)l * -0.001f).setColor(color[0], color[1], color[2], 1.0f).setUv(g, n).setLight(i);
            poseStack.popPose();
            ++l;
        }
    }
}
