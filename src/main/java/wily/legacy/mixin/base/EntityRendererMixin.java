package wily.legacy.mixin.base;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.EntityRenderer;
//? if >=1.21.2 {
import net.minecraft.client.renderer.entity.state.EntityRenderState;
import net.minecraft.client.renderer.entity.state.PlayerRenderState;
//?}
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.Entity;
import org.joml.Matrix4f;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import wily.factoryapi.base.client.FactoryGuiGraphics;
import wily.legacy.Legacy4JClient;
import wily.legacy.client.LegacyOption;
import wily.legacy.entity.LegacyPlayerInfo;
import wily.legacy.util.ScreenUtil;

@Mixin(EntityRenderer.class)
public abstract class EntityRendererMixin {
    @Shadow @Final private Font font;

    @Inject(method = "renderNameTag", at = @At(value = "INVOKE", target = "Lcom/mojang/blaze3d/vertex/PoseStack;scale(FFF)V", shift = At.Shift.AFTER))
    protected void renderNameTag(/*? if <1.21.2 {*/ /*Entity*//*?} else {*/ EntityRenderState/*?}*/ entity, Component component, PoseStack poseStack, MultiBufferSource multiBufferSource, int i, /*? if >=1.20.5 && <1.21.2 {*//*float f, *//*?}*/CallbackInfo ci) {
        Minecraft minecraft = Minecraft.getInstance();
        float thickness = Math.max(0.1f,/*? if <1.21.2 {*/ /*minecraft.player.distanceTo(entity)*//*?} else {*/ (float) Math.sqrt(entity.distanceToCameraSq)/*?}*/ / 16f);
        if (!component.equals(entity./*? if <1.21.2 {*/ /*getDisplayName()*//*?} else {*/ nameTag/*?}*/) || !LegacyOption.displayNameTagBorder.get() || thickness >=1) return;
        String name = component.getString();
        int j = "deadmau5".equals(name) ? -10 : 0;
        int h = (int) (-font.width(component) / 2f);
        float[] color = !(entity instanceof /*? if <1.21.2 {*/ /*AbstractClientPlayer*//*?} else {*/ PlayerRenderState/*?}*/ p)  || minecraft.getConnection() == null || !(minecraft.getConnection().getPlayerInfo(p./*? if <1.21.2 {*/ /*getUUID()*//*?} else {*/ name/*?}*/) instanceof LegacyPlayerInfo info) || info.getIdentifierIndex() == 0 ?  new float[]{0,0,0} : Legacy4JClient.getVisualPlayerColor(info);
        renderOutline(multiBufferSource.getBuffer(entity./*? if <1.21.2 {*/ /*isShiftKeyDown()*//*?} else {*/ isDiscrete/*?}*/ ?  RenderType.textBackground() : RenderType.textBackgroundSeeThrough()), poseStack, h - 1.1f, j - 1.1f, font.width(component) + 2.1f,10.1f, thickness, color[0],color[1],color[2],1.0f);
    }
    @Redirect(method = "renderNameTag", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/Font;drawInBatch(Lnet/minecraft/network/chat/Component;FFIZLorg/joml/Matrix4f;Lnet/minecraft/client/renderer/MultiBufferSource;Lnet/minecraft/client/gui/Font$DisplayMode;II)I", ordinal = 0))
    protected int renderNameTag(Font instance, Component arg, float f, float g, int i, boolean bl, Matrix4f matrix4f, MultiBufferSource arg2, Font.DisplayMode arg3, int j, int k, /*? if <1.21.2 {*/ /*Entity*//*?} else {*/ EntityRenderState/*?}*/ entity) {
        Minecraft minecraft = Minecraft.getInstance();
        float thickness = Math.max(0.1f,/*? if <1.21.2 {*/ /*minecraft.player.distanceTo(entity)*//*?} else {*/ (float) Math.sqrt(entity.distanceToCameraSq)/*?}*/ / 16f);
        float[] color = thickness < 1 || !LegacyOption.displayNameTagBorder.get() ? null : !(entity instanceof /*? if <1.21.2 {*/ /*AbstractClientPlayer*//*?} else {*/ PlayerRenderState/*?}*/ p)  || minecraft.getConnection() == null || !(minecraft.getConnection().getPlayerInfo(p./*? if <1.21.2 {*/ /*getUUID()*//*?} else {*/ name/*?}*/) instanceof LegacyPlayerInfo info) || info.getIdentifierIndex() == 0 ?  new float[]{0,0,0} : Legacy4JClient.getVisualPlayerColor(info);
        return instance.drawInBatch(arg,f,g,i,bl,matrix4f,arg2,arg3,color == null ? j : ScreenUtil.colorFromFloat(color[0],color[1],color[2],1.0f),k);
    }


    public void renderOutline(VertexConsumer consumer, PoseStack poseStack, float x, float y, float width, float height,float thickness, float r, float g, float b , float a) {
        this.fill(consumer,poseStack,x, y, x + width, y + thickness, r,g,b,a);
        this.fill(consumer,poseStack,x, y + height - thickness, x + width, y + height, r,g,b,a);
        this.fill(consumer,poseStack,x, y + thickness, x + thickness, y + height - thickness, r,g,b,a);
        this.fill(consumer,poseStack,x + width - thickness, y + thickness, x + width, y + height - thickness, r,g,b,a);
    }
    public void fill(VertexConsumer vertexConsumer, PoseStack poseStack, float i, float j, float k, float l, float r, float g, float b , float a) {
        float o;
        Matrix4f matrix4f = poseStack.last().pose();
        if (i < k) {
            o = i;
            i = k;
            k = o;
        }
        if (j < l) {
            o = j;
            j = l;
            l = o;
        }
        //? if <1.20.5 {
        /*vertexConsumer.vertex(matrix4f, i, j, 0).color(r,g,b,a).uv2(LightTexture.FULL_BRIGHT).endVertex();
        vertexConsumer.vertex(matrix4f, i, l, 0).color(r,g,b,a).uv2(LightTexture.FULL_BRIGHT).endVertex();
        vertexConsumer.vertex(matrix4f, k, l, 0).color(r,g,b,a).uv2(LightTexture.FULL_BRIGHT).endVertex();
        vertexConsumer.vertex(matrix4f, k, j, 0).color(r,g,b,a).uv2(LightTexture.FULL_BRIGHT).endVertex();
        *///?} else {
        vertexConsumer.addVertex(matrix4f, i, j, 0).setColor(r,g,b,a).setLight(LightTexture.FULL_BRIGHT);
        vertexConsumer.addVertex(matrix4f, i, l, 0).setColor(r,g,b,a).setLight(LightTexture.FULL_BRIGHT);
        vertexConsumer.addVertex(matrix4f, k, l, 0).setColor(r,g,b,a).setLight(LightTexture.FULL_BRIGHT);
        vertexConsumer.addVertex(matrix4f, k, j, 0).setColor(r,g,b,a).setLight(LightTexture.FULL_BRIGHT);
        //?}
    }
}
