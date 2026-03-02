package wily.legacy.mixin.base.skins.client;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.model.PlayerModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.LivingEntityRenderer;
import net.minecraft.client.renderer.entity.player.AvatarRenderer;
import net.minecraft.client.renderer.entity.state.AvatarRenderState;
import net.minecraft.client.renderer.entity.state.EntityRenderState;
import net.minecraft.client.renderer.entity.state.LivingEntityRenderState;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.resources.ResourceLocation;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import wily.legacy.Skins.client.render.RenderStateSkinIdAccess;
import wily.legacy.Skins.client.render.boxloader.AttachSlot;
import wily.legacy.Skins.client.render.boxloader.BoxModelManager;
import wily.legacy.Skins.client.render.boxloader.BuiltBoxModel;
import wily.legacy.Skins.skin.ClientSkinAssets;
import wily.legacy.Skins.skin.ClientSkinCache;
import wily.legacy.Skins.skin.SkinEntry;
import wily.legacy.Skins.skin.SkinPackLoader;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.List;

@Mixin(LivingEntityRenderer.class)
public abstract class LivingEntityRendererSpectatorBoxHeadMixin {

    @Shadow
    public abstract EntityModel getModel();

    @WrapOperation(
            method = "submit*",
            at = @org.spongepowered.asm.mixin.injection.At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/client/renderer/SubmitNodeCollector;submitModelPart(Lnet/minecraft/client/model/geom/ModelPart;Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/RenderType;IILnet/minecraft/client/renderer/texture/TextureAtlasSprite;)V"
            ),
            require = 0
    )
    private void consoleskins$submitSpectatorBoxHead(
            SubmitNodeCollector collector,
            ModelPart part,
            PoseStack poseStack,
            RenderType renderType,
            int packedLight,
            int overlay,
            TextureAtlasSprite sprite,
            Operation<Void> original,
            LivingEntityRenderState state
    ) {
        original.call(collector, part, poseStack, renderType, packedLight, overlay, sprite);

        if (!(state instanceof AvatarRenderState ars)) return;
        if (!((Object) this instanceof AvatarRenderer)) return;
        if (!consoleskins$isSpectator(ars)) return;

        EntityModel em = (EntityModel) (Object) getModel();
        if (!(em instanceof PlayerModel pm)) return;
        if (part != pm.head) return;

        String skinId = consoleskins$resolveSkinId(ars);
        ResourceLocation texture = consoleskins$resolveTexture(ars, skinId);
        if (texture == null) return;

        ResourceLocation modelId = consoleskins$modelIdFromTexture(texture);

        BuiltBoxModel built = BoxModelManager.get(modelId);
        if (built == null && skinId != null && !skinId.isBlank() && !"auto_select".equals(skinId)) {
            var mj = ClientSkinAssets.getModelJson(skinId);
            if (mj != null) BoxModelManager.registerRuntime(modelId, mj);
            built = BoxModelManager.get(modelId);
        }
        if (built == null) return;

        List<ModelPart> headParts = built.get(AttachSlot.HEAD);
        List<ModelPart> hatParts = built.get(AttachSlot.HAT);
        if ((headParts == null || headParts.isEmpty()) && (hatParts == null || hatParts.isEmpty())) return;

        final BuiltBoxModel baked = built;
        final ResourceLocation texFinal = texture;

        collector.submitCustomGeometry(
                poseStack,
                RenderType.entityCutoutNoCull(texFinal),
                (pose, vc) -> {
                    PoseStack ps = new PoseStack();
                    ps.last().set(pose);

                    List<ModelPart> head = baked.get(AttachSlot.HEAD);
                    if (head != null && !head.isEmpty()) {
                        ps.pushPose();
                        pm.head.translateAndRotate(ps);
                        for (ModelPart mp : head) mp.render(ps, vc, packedLight, OverlayTexture.NO_OVERLAY);
                        ps.popPose();
                    }

                    List<ModelPart> hat = baked.get(AttachSlot.HAT);
                    if (hat != null && !hat.isEmpty()) {
                        ps.pushPose();
                        pm.hat.translateAndRotate(ps);
                        for (ModelPart mp : hat) mp.render(ps, vc, packedLight, OverlayTexture.NO_OVERLAY);
                        ps.popPose();
                    }
                }
        );
    }

    private static boolean consoleskins$isSpectator(EntityRenderState state) {
        if (!(state instanceof AvatarRenderState ars)) return false;
        try {
            Field f = ars.getClass().getField("isSpectator");
            if (f.getType() == boolean.class) return f.getBoolean(ars);
        } catch (Throwable ignored) {
        }
        try {
            Method m = ars.getClass().getMethod("isSpectator");
            if (m.getReturnType() == boolean.class) return (boolean) m.invoke(ars);
        } catch (Throwable ignored) {
        }
        return false;
    }

    private static String consoleskins$resolveSkinId(AvatarRenderState state) {
        if (state instanceof RenderStateSkinIdAccess a) {
            String skinId = null;
            try {
                skinId = a.consoleskins$getSkinId();
            } catch (Throwable ignored) {
            }
            if (skinId != null && !skinId.isBlank()) return skinId;

            try {
                if (state.nameTag != null) skinId = ClientSkinCache.getByName(state.nameTag.getString());
            } catch (Throwable ignored) {
            }
            if (skinId != null && !skinId.isBlank()) {
                try {
                    a.consoleskins$setSkinId(skinId);
                } catch (Throwable ignored) {
                }
                return skinId;
            }
        }
        try {
            if (state.nameTag != null) return ClientSkinCache.getByName(state.nameTag.getString());
        } catch (Throwable ignored) {
        }
        return null;
    }

    private static ResourceLocation consoleskins$resolveTexture(AvatarRenderState state, String skinId) {
        ResourceLocation tex = null;
        if (skinId != null && !skinId.isBlank() && !"auto_select".equals(skinId)) {
            tex = ClientSkinAssets.getTexture(skinId);
            if (tex == null) {
                SkinEntry entry = SkinPackLoader.getSkin(skinId);
                if (entry != null) tex = entry.texture();
            }
        }
        if (tex != null) return tex;

        try {
            Object skin = state.skin;
            if (skin != null) return consoleskins$extractTextureFromSkin(skin);
        } catch (Throwable ignored) {
        }
        return null;
    }

    private static ResourceLocation consoleskins$extractTextureFromSkin(Object skin) {
        if (skin == null) return null;
        try {
            Method m = skin.getClass().getMethod("texture");
            Object r = m.invoke(skin);
            if (r instanceof ResourceLocation rl) return rl;
        } catch (Throwable ignored) {
        }
        try {
            Method m = skin.getClass().getMethod("textureLocation");
            Object r = m.invoke(skin);
            if (r instanceof ResourceLocation rl) return rl;
        } catch (Throwable ignored) {
        }
        try {
            Field f = skin.getClass().getField("texture");
            Object r = f.get(skin);
            if (r instanceof ResourceLocation rl) return rl;
        } catch (Throwable ignored) {
        }
        return null;
    }

    private static ResourceLocation consoleskins$modelIdFromTexture(ResourceLocation texture) {
        String p = texture.getPath();
        int slash = p.lastIndexOf('/');
        if (slash != -1) p = p.substring(slash + 1);
        if (p.endsWith(".png")) p = p.substring(0, p.length() - 4);
        return ResourceLocation.fromNamespaceAndPath(texture.getNamespace(), p);
    }
}
