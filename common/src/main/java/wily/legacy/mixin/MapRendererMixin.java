package wily.legacy.mixin;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.MapRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.resources.language.I18n;
import net.minecraft.world.level.saveddata.maps.MapDecoration;
import net.minecraft.world.level.saveddata.maps.MapDecorationTypes;
import net.minecraft.world.level.saveddata.maps.MapItemSavedData;
import org.joml.Matrix4f;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import wily.legacy.Legacy4JClient;
import wily.legacy.client.PlayerIdentifier;
import wily.legacy.entity.LegacyPlayerInfo;

import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

@Mixin(MapRenderer.MapInstance.class)
public abstract class MapRendererMixin {
   @Shadow private MapItemSavedData data;

    private boolean isPlayerDecoration(MapDecoration decoration){
        return decoration.type() == MapDecorationTypes.PLAYER || decoration.type() == MapDecorationTypes.PLAYER_OFF_MAP || decoration.type() == MapDecorationTypes.PLAYER_OFF_LIMITS;
    }
    @Redirect(method = "draw",at = @At(value = "INVOKE", target = "Lnet/minecraft/world/level/saveddata/maps/MapItemSavedData;getDecorations()Ljava/lang/Iterable;"))
    Iterable<MapDecoration> drawDecorations(MapItemSavedData instance){
        return StreamSupport.stream(instance.getDecorations().spliterator(),false).filter(s-> !isPlayerDecoration(s)).collect(Collectors.toSet());
    }
    @Inject(method = "draw", at = @At("HEAD"))
    void draw(PoseStack poseStack, MultiBufferSource multiBufferSource, boolean bl, int i, CallbackInfo ci) {
        Minecraft minecraft = Minecraft.getInstance();
        Font font = minecraft.font;
        if (!bl) {
            poseStack.pushPose();
            poseStack.translate(-0.2f,0.4f,-0.1f);
            poseStack.scale(1f, 0.95f, 1);
            font.drawInBatch(I18n.get("legacy.map.coords",(int)minecraft.player.getX(), (int)minecraft.player.getEyeY(),(int)minecraft.player.getZ()), 0.0f, 0.0f, 0, false, poseStack.last().pose(), multiBufferSource, Font.DisplayMode.NORMAL, 0, i);
            poseStack.popPose();
        }
    }
    private MapRenderer.MapInstance self(){
        return ((MapRenderer.MapInstance)(Object)this);
    }
    @Inject(method = "draw", at = @At("RETURN"))
    void drawReturn(PoseStack poseStack, MultiBufferSource multiBufferSource, boolean bl, int i, CallbackInfo ci) {
        int l = 0;
        for (MapDecoration mapDecoration : this.data.getDecorations()) {
            if ((bl && !mapDecoration.renderOnFrame()) || !isPlayerDecoration(mapDecoration)) continue;
            poseStack.pushPose();
            poseStack.translate(0.0f + (float)mapDecoration.x() / 2.0f + 64.0f, 0.0f + (float)mapDecoration.y() / 2.0f + 64.0f, -0.02f);
            poseStack.mulPose(Axis.ZP.rotationDegrees((float)(mapDecoration.rot() * 360) / 16.0f));
            poseStack.scale(4.0f, 4.0f, 3.0f);
            poseStack.translate(-0.125f, 0.125f, 0.0f);
            Minecraft minecraft = Minecraft.getInstance();
            LegacyPlayerInfo playerInfo = mapDecoration.name().isEmpty() || minecraft.getConnection() == null || !(minecraft.getConnection().getPlayerInfo(mapDecoration.name().get().getString()) instanceof LegacyPlayerInfo info) ? null : info;
            TextureAtlasSprite textureAtlasSprite = minecraft.getMapDecorationTextures().textureAtlas.getSprite(playerInfo == null ? mapDecoration.getSpriteLocation() : PlayerIdentifier.of(playerInfo.getIdentifierIndex()).spriteByMapDecorationType(mapDecoration.type()));
            float g = textureAtlasSprite.getU0();
            float h = textureAtlasSprite.getV0();
            float m = textureAtlasSprite.getU1();
            float n = textureAtlasSprite.getV1();
            Matrix4f matrix4f2 = poseStack.last().pose();
            float[] color = playerInfo == null ? new float[]{1.0f,1.0f,1.0f} : Legacy4JClient.getVisualPlayerColor(playerInfo);
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
