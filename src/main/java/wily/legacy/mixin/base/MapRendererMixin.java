package wily.legacy.mixin.base;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
//? if <1.21.2 {
/*import net.minecraft.client.gui.MapRenderer;
*///?} else {
import net.minecraft.client.renderer.state.MapRenderState;
import net.minecraft.client.renderer.MapRenderer;
import wily.legacy.client.LegacyMapDecorationRenderState;
//?}
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.resources.language.I18n;
import net.minecraft.core.Holder;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.saveddata.maps.*;
import org.joml.Matrix4f;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import wily.legacy.Legacy4JClient;
import wily.legacy.client.PlayerIdentifier;
import wily.legacy.entity.LegacyPlayerInfo;

import java.util.Iterator;
import java.util.List;
import java.util.stream.StreamSupport;

@Mixin(/*? if <1.21.2 {*//*MapRenderer.MapInstance*//*?} else {*/ MapRenderer/*?}*/.class)
public abstract class MapRendererMixin {
    //? if <1.20.5
    /*@Unique private static final RenderType MAP_ICONS = RenderType.text(new ResourceLocation("textures/map/map_icons.png"));*/
    //? if <1.21.2 {
    /*@Shadow
    private MapItemSavedData data;
    *///?} else {
    @Inject(method = "extractRenderState", at = @At("RETURN"))
    private void extractRenderState(MapId mapId, MapItemSavedData mapItemSavedData, MapRenderState mapRenderState, CallbackInfo ci){
        int i = 0;
        for (MapDecoration decoration : mapItemSavedData.getDecorations()) {
            LegacyMapDecorationRenderState.of(mapRenderState.decorations.get(i)).extractRenderState(decoration);
            i++;
        }
    }
    //?}
    @Unique
    //? if <1.20.5 {
    /*private boolean isPlayerDecoration(MapDecoration.Type type){
        return type.equals(MapDecoration.Type.PLAYER) || type.equals(MapDecoration.Type.PLAYER_OFF_MAP) || type.equals(MapDecoration.Type.PLAYER_OFF_LIMITS);
    }
    *///?} else {
    private boolean isPlayerDecoration(Holder<MapDecorationType> type){
        return type.equals(MapDecorationTypes.PLAYER) || type.equals(MapDecorationTypes.PLAYER_OFF_MAP) || type.equals(MapDecorationTypes.PLAYER_OFF_LIMITS);
    }
    //?}
    @Redirect(method = /*? if <1.21.2 {*//*"draw"*//*?} else {*/ "render"/*?}*/,at = @At(value = "INVOKE", target = /*? if <1.21.2 {*//*"Ljava/lang/Iterable;iterator()Ljava/util/Iterator;"*//*?} else {*/ "Ljava/util/List;iterator()Ljava/util/Iterator;"/*?}*/))
    Iterator</*? if <1.21.2 {*//*MapDecoration*//*?} else {*/ MapRenderState.MapDecorationRenderState/*?}*/> drawDecorations(/*? if <=1.21.2 {*//*Iterable<MapDecoration>*//*?} else {*/ List<MapRenderState.MapDecorationRenderState>/*?}*/ iterable){
        return /*? if <1.21.2 {*//*StreamSupport.stream(iterable.spliterator(), false)*//*?} else {*/iterable.stream()/*?}*/.filter(s-> !isPlayerDecoration(/*? if <1.20.2 {*//*s.getType() *//*?} else if <1.21.2 {*//*s.type()*//*?} else {*/LegacyMapDecorationRenderState.of(s).getType()/*?}*/)).iterator();
    }
    @Inject(method = /*? if <1.21.2 {*//*"draw"*//*?} else {*/ "render"/*?}*/, at = @At("HEAD"))
    void draw(/*? if >=1.21.2 {*/MapRenderState mapRenderState, /*?}*/PoseStack poseStack, MultiBufferSource multiBufferSource, boolean bl, int i, CallbackInfo ci) {
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

    @Inject(method = /*? if <1.21.2 {*//*"draw"*//*?} else {*/ "render"/*?}*/, at = @At("RETURN"))
    void drawReturn(/*? if >=1.21.2 {*/MapRenderState mapRenderState, /*?}*/PoseStack poseStack, MultiBufferSource multiBufferSource, boolean bl, int i, CallbackInfo ci) {
        int l = 0;
        for (/*? if <1.21.2 {*//*MapDecoration*//*?} else {*/ MapRenderState.MapDecorationRenderState/*?}*/ mapDecoration : /*? if <1.21.2 {*//*this.data.getDecorations()*//*?} else {*/ mapRenderState.decorations/*?}*/) {
            /*? if <1.20.5 {*//*MapDecoration.Type*//*?} else {*/Holder<MapDecorationType>/*?}*/ type = /*? if <1.20.2 {*//*mapDecoration.getType() *//*?} else if <1.21.2 {*//*mapDecoration.type()*//*?} else {*/LegacyMapDecorationRenderState.of(mapDecoration).getType()/*?}*/;
            if ((bl && !mapDecoration.renderOnFrame/*? if <1.21.2 {*//*()*//*?}*/) || !isPlayerDecoration(type)) continue;
            poseStack.pushPose();
            poseStack.translate(0.0f + (float)mapDecoration./*? if >=1.21.2 {*/x/*?} else if >1.20.1 {*//*x()*//*?} else {*//*getX()*//*?}*/ / 2.0f + 64.0f, 0.0f + (float)mapDecoration./*? if >=1.21.2 {*/y/*?} else if >1.20.1 {*//*y()*//*?} else {*//*getY()*//*?}*/ / 2.0f + 64.0f, -0.02f);
            poseStack.mulPose(Axis.ZP.rotationDegrees((float)(mapDecoration./*? if >=1.21.2 {*/rot/*?} else if >1.20.1 {*//*rot()*//*?} else {*//*getRot()*//*?}*/ * 360) / 16.0f));
            poseStack.scale(4.0f, 4.0f, 3.0f);
            poseStack.translate(-0.125f, 0.125f, 0.0f);
            Matrix4f matrix4f2 = poseStack.last().pose();
            Minecraft minecraft = Minecraft.getInstance();
            LegacyPlayerInfo playerInfo = /*? if <1.20.5 {*//*mapDecoration./^? if >1.20.1 {^/name/^?} else {^//^getName^//^?}^/() == null *//*?} else if <1.21.2 {*//*mapDecoration.name().isEmpty()*//*?} else {*/mapDecoration.name == null/*?}*/ || minecraft.getConnection() == null || !(minecraft.getConnection().getPlayerInfo(mapDecoration./*? if >1.20.1 {*/name/*?} else {*//*getName*//*?}*//*? if <1.20.5 {*//*()*//*?} else if <1.21.2 {*//*().get()*//*?}*/.getString()) instanceof LegacyPlayerInfo info) ? null : info;
            float[] color = playerInfo == null ? new float[]{1.0f,1.0f,1.0f} : Legacy4JClient.getVisualPlayerColor(playerInfo);
            //? if <1.20.5 {
            /*byte image = playerInfo == null ? mapDecoration.getImage() : PlayerIdentifier.of(playerInfo.getIdentifierIndex()).indexByMapDecorationType(type);
            float g = (float)(image % 16) / 16.0f;
            float h = (float)(image / 16) / 16.0f;
            float m = (float)(image % 16 + 1) / 16.0f;
            float n = (float)(image / 16 + 1) / 16.0f;
            VertexConsumer vertexConsumer2 = multiBufferSource.getBuffer(MAP_ICONS);
            vertexConsumer2.vertex(matrix4f2, -1.0f, 1.0f, (float)l * -0.001f).color(color[0], color[1], color[2], 1.0f).uv(g, h).uv2(i).endVertex();
            vertexConsumer2.vertex(matrix4f2, 1.0f, 1.0f, (float)l * -0.001f).color(color[0], color[1], color[2], 1.0f).uv(m, h).uv2(i).endVertex();
            vertexConsumer2.vertex(matrix4f2, 1.0f, -1.0f, (float)l * -0.001f).color(color[0], color[1], color[2], 1.0f).uv(m, n).uv2(i).endVertex();
            vertexConsumer2.vertex(matrix4f2, -1.0f, -1.0f, (float)l * -0.001f).color(color[0], color[1], color[2], 1.0f).uv(g, n).uv2(i).endVertex();
            *///?} else {
            TextureAtlasSprite textureAtlasSprite = playerInfo == null ? /*? if <1.21.2 {*//*minecraft.getMapDecorationTextures().textureAtlas.getSprite(mapDecoration.getSpriteLocation())*//*?} else {*/ mapDecoration.atlasSprite/*?}*/ : minecraft.getMapDecorationTextures().textureAtlas.getSprite(PlayerIdentifier.of(playerInfo.getIdentifierIndex()).spriteByMapDecorationType(type));
            float g = textureAtlasSprite.getU0();
            float h = textureAtlasSprite.getV0();
            float m = textureAtlasSprite.getU1();
            float n = textureAtlasSprite.getV1();
            VertexConsumer vertexConsumer2 = multiBufferSource.getBuffer(RenderType.text(textureAtlasSprite.atlasLocation()));
            vertexConsumer2.addVertex(matrix4f2, -1.0f, 1.0f, (float)l * -0.001f).setColor(color[0], color[1], color[2], 1.0f).setUv(g, h).setLight(i);
            vertexConsumer2.addVertex(matrix4f2, 1.0f, 1.0f, (float)l * -0.001f).setColor(color[0], color[1], color[2], 1.0f).setUv(m, h).setLight(i);
            vertexConsumer2.addVertex(matrix4f2, 1.0f, -1.0f, (float)l * -0.001f).setColor(color[0], color[1], color[2], 1.0f).setUv(m, n).setLight(i);
            vertexConsumer2.addVertex(matrix4f2, -1.0f, -1.0f, (float)l * -0.001f).setColor(color[0], color[1], color[2], 1.0f).setUv(g, n).setLight(i);
            //?}
            poseStack.popPose();
            ++l;
        }
    }
}
