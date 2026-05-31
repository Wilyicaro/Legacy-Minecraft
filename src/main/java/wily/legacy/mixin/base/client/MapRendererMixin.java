package wily.legacy.mixin.base.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.renderer.state.MapRenderState;
import net.minecraft.client.renderer.MapRenderer;
import net.minecraft.data.AtlasIds;
import net.minecraft.network.chat.Component;
import wily.legacy.client.CommonColor;
import wily.legacy.client.LegacyMapDecorationRenderState;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.core.Holder;
import net.minecraft.resources.Identifier;
import net.minecraft.world.level.saveddata.maps.*;
import org.joml.Matrix4f;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import wily.factoryapi.base.config.FactoryConfig;
import wily.legacy.Legacy4JClient;
import wily.legacy.client.LegacyMapFillAnimation;
import wily.legacy.client.LegacyOptions;
import wily.legacy.config.LegacyCommonOptions;
import wily.legacy.client.PlayerIdentifier;
import wily.legacy.entity.LegacyPlayerInfo;

import java.util.Iterator;
import java.util.List;

@Mixin(MapRenderer.class)
public abstract class MapRendererMixin {
    @Unique
    private static final Identifier LEGACY_MAP_BACKGROUND = Identifier.withDefaultNamespace("textures/map/map_background_checkerboard.png");

    @Inject(method = "extractRenderState", at = @At("RETURN"))
    private void extractRenderState(MapId mapId, MapItemSavedData mapItemSavedData, MapRenderState mapRenderState, CallbackInfo ci) {
        if (FactoryConfig.hasCommonConfigEnabled(LegacyCommonOptions.legacyMapBehavior)) {
            LegacyMapFillAnimation.track(mapRenderState, mapId, mapItemSavedData);
        }
        int i = 0;
        for (MapDecoration decoration : mapItemSavedData.getDecorations()) {
            LegacyMapDecorationRenderState.of(mapRenderState.decorations.get(i)).extractRenderState(decoration);
            i++;
        }
    }

    @Unique
    private boolean isPlayerDecoration(Holder<MapDecorationType> type) {
        return type.equals(MapDecorationTypes.PLAYER) || type.equals(MapDecorationTypes.PLAYER_OFF_MAP) || type.equals(MapDecorationTypes.PLAYER_OFF_LIMITS);
    }

    @Redirect(method = "render", at = @At(value = "INVOKE", target = "Ljava/util/List;iterator()Ljava/util/Iterator;"))
    Iterator<MapRenderState.MapDecorationRenderState> drawDecorations(List<MapRenderState.MapDecorationRenderState> iterable) {
        if (!FactoryConfig.hasCommonConfigEnabled(LegacyCommonOptions.legacyMapBehavior)) {
            return iterable.iterator();
        }
        return iterable.stream().filter(s -> !isPlayerDecoration(LegacyMapDecorationRenderState.of(s).getType())).iterator();
    }

    @Inject(method = "render", at = @At(value = "INVOKE", target = "Ljava/util/List;iterator()Ljava/util/Iterator;"))
    private void drawFillAnimation(MapRenderState mapRenderState, PoseStack poseStack, SubmitNodeCollector submitNodeCollector, boolean bl, int i, CallbackInfo ci) {
        if (!FactoryConfig.hasCommonConfigEnabled(LegacyCommonOptions.legacyMapBehavior)) {
            return;
        }
        byte[] colors = LegacyMapFillAnimation.colors(mapRenderState);
        if (colors == null) return;
        drawHiddenMapPixels(mapRenderState, poseStack, submitNodeCollector, colors, i);
    }

    @Unique
    private void drawHiddenMapPixels(MapRenderState mapRenderState, PoseStack poseStack, SubmitNodeCollector submitNodeCollector, byte[] colors, int light) {
        if (!hasHiddenMapPixels(mapRenderState, colors)) return;
        submitNodeCollector.submitCustomGeometry(poseStack, RenderTypes.text(LEGACY_MAP_BACKGROUND), (pose, vertexConsumer) -> {
            for (int x = 0; x < 128; x++) {
                if (LegacyMapFillAnimation.isColumnVisible(mapRenderState, x)) continue;
                int y = 0;
                while (y < 128) {
                    while (y < 128 && colors[x + y * 128] == 0) y++;
                    int start = y;
                    while (y < 128 && colors[x + y * 128] != 0) y++;
                    if (start < y) drawHiddenMapRun(pose, vertexConsumer, x, start, y, light);
                }
            }
        });
    }

    @Unique
    private boolean hasHiddenMapPixels(MapRenderState mapRenderState, byte[] colors) {
        for (int x = 0; x < 128; x++) {
            if (LegacyMapFillAnimation.isColumnVisible(mapRenderState, x)) continue;
            for (int y = 0; y < 128; y++) {
                if (colors[x + y * 128] != 0) return true;
            }
        }
        return false;
    }

    @Unique
    private void drawHiddenMapRun(PoseStack.Pose pose, com.mojang.blaze3d.vertex.VertexConsumer vertexConsumer, int x, int y0, int y1, int light) {
        float u0 = x / 128.0f;
        float u1 = (x + 1) / 128.0f;
        float v0 = y0 / 128.0f;
        float v1 = y1 / 128.0f;
        vertexConsumer.addVertex(pose, x, y1, -0.015f).setColor(-1).setUv(u0, v1).setLight(light);
        vertexConsumer.addVertex(pose, x + 1, y1, -0.015f).setColor(-1).setUv(u1, v1).setLight(light);
        vertexConsumer.addVertex(pose, x + 1, y0, -0.015f).setColor(-1).setUv(u1, v0).setLight(light);
        vertexConsumer.addVertex(pose, x, y0, -0.015f).setColor(-1).setUv(u0, v0).setLight(light);
    }

    @Inject(method = "render", at = @At("RETURN"))
    void draw(MapRenderState mapRenderState, PoseStack poseStack, SubmitNodeCollector submitNodeCollector, boolean bl, int i, CallbackInfo ci) {
        Minecraft minecraft = Minecraft.getInstance();
        if (!bl && LegacyOptions.mapsWithCoords.get()) {
            poseStack.pushPose();
            poseStack.translate(-0.2f, 0.4f, -0.1f);
            poseStack.scale(1.0f, 0.95f, 1.0f);
            submitNodeCollector.submitText(poseStack, 0.0f, 0.0f, Component.translatable("legacy.map.coords", (int) minecraft.player.getX(), (int) minecraft.player.getEyeY(), (int) minecraft.player.getZ()).getVisualOrderText(), false, Font.DisplayMode.NORMAL, i, CommonColor.MAP_COORDINATE_TEXT.get(), 0, 0);
            poseStack.popPose();
        }
    }

    @Inject(method = "render", at = @At("RETURN"))
    void drawReturn(MapRenderState mapRenderState, PoseStack poseStack, SubmitNodeCollector submitNodeCollector, boolean bl, int i, CallbackInfo ci) {
        if (!FactoryConfig.hasCommonConfigEnabled(LegacyCommonOptions.legacyMapBehavior)) {
            return;
        }
        int l = 0;
        for (MapRenderState.MapDecorationRenderState mapDecoration : mapRenderState.decorations) {
            if ((bl && !mapDecoration.renderOnFrame)) continue;
            Holder<MapDecorationType> type = LegacyMapDecorationRenderState.of(mapDecoration).getType();
            if (isPlayerDecoration(type)) {
                poseStack.pushPose();
                poseStack.translate(0.0f + (float) mapDecoration.x / 2.0f + 64.0f, 0.0f + (float) mapDecoration.y / 2.0f + 64.0f, -0.02f);
                poseStack.mulPose(Axis.ZP.rotationDegrees((float) (mapDecoration.rot * 360) / 16.0f));
                poseStack.scale(4.0f, 4.0f, 3.0f);
                poseStack.translate(-0.125f, 0.125f, 0.0f);
                Minecraft minecraft = Minecraft.getInstance();
                LegacyPlayerInfo playerInfo = mapDecoration.name == null || minecraft.getConnection() == null || !(minecraft.getConnection().getPlayerInfo(mapDecoration.name.getString()) instanceof LegacyPlayerInfo info) ? null : info;
                float[] color = playerInfo == null ? new float[]{1.0f, 1.0f, 1.0f} : Legacy4JClient.getVisualPlayerColor(playerInfo);
                TextureAtlasSprite textureAtlasSprite = playerInfo == null ? mapDecoration.atlasSprite : minecraft.getAtlasManager().getAtlasOrThrow(AtlasIds.MAP_DECORATIONS).getSprite(PlayerIdentifier.of(playerInfo.getIdentifierIndex()).spriteByMapDecorationType(type));
                float g = textureAtlasSprite.getU0();
                float h = textureAtlasSprite.getV0();
                float m = textureAtlasSprite.getU1();
                float n = textureAtlasSprite.getV1();
                float z = l * -0.001f;
                submitNodeCollector.submitCustomGeometry(poseStack, RenderTypes.text(textureAtlasSprite.atlasLocation()), (pose, vertexConsumer) -> {
                    vertexConsumer.addVertex(pose, -1.0f, 1.0f, z).setColor(color[0], color[1], color[2], 1.0f).setUv(g, h).setLight(i);
                    vertexConsumer.addVertex(pose, 1.0f, 1.0f, z).setColor(color[0], color[1], color[2], 1.0f).setUv(m, h).setLight(i);
                    vertexConsumer.addVertex(pose, 1.0f, -1.0f, z).setColor(color[0], color[1], color[2], 1.0f).setUv(m, n).setLight(i);
                    vertexConsumer.addVertex(pose, -1.0f, -1.0f, z).setColor(color[0], color[1], color[2], 1.0f).setUv(g, n).setLight(i);
                });
                poseStack.popPose();
            }
            ++l;
        }
    }
}
