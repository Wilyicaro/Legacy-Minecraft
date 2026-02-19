package wily.legacy.CustomModelSkins.cpm.client;


/**
 * Console skins / CPM glue.
 */

import com.llamalad7.mixinextras.sugar.ref.LocalRef;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.gui.Font.DisplayMode;
import net.minecraft.client.model.Model;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.renderer.OrderedSubmitNodeCollector;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.SubmitNodeCollector.CustomGeometryRenderer;
import net.minecraft.client.renderer.SubmitNodeCollector.ParticleGroupRenderer;
import net.minecraft.client.renderer.block.MovingBlockRenderState;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.client.renderer.block.model.BlockStateModel;
import net.minecraft.client.renderer.entity.state.EntityRenderState;
import net.minecraft.client.renderer.entity.state.EntityRenderState.LeashState;
import net.minecraft.client.renderer.entity.state.EntityRenderState.ShadowPiece;
import net.minecraft.client.renderer.entity.state.HitboxesRenderState;
import net.minecraft.client.renderer.feature.ModelFeatureRenderer.CrumblingOverlay;
import net.minecraft.client.renderer.item.ItemStackRenderState.FoilType;
import net.minecraft.client.renderer.state.CameraRenderState;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.network.chat.Component;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import org.joml.Quaternionf;
import wily.legacy.CustomModelSkins.cpm.client.SelfRenderer.RenderCollector;
import wily.legacy.Skins.client.render.BadSantaSitConfig;
import wily.legacy.Skins.client.render.IdleSitPose;
import wily.legacy.Skins.client.render.IdleSitTracker;
import wily.legacy.Skins.client.render.RenderStateSkinIdAccess;
import wily.legacy.Skins.client.util.ConsoleSkinsClientSettings;

import java.util.List;
import java.util.UUID;

public class CPMOrderedSubmitNodeCollector implements OrderedSubmitNodeCollector {

    private final OrderedSubmitNodeCollector collector;

    public CPMOrderedSubmitNodeCollector(OrderedSubmitNodeCollector collector) {
        this.collector = collector;
    }

    @Override
    public void submitShadow(PoseStack pose, float f, List<ShadowPiece> pieces) {
        collector.submitShadow(pose, f, pieces);
    }

    @Override
    public void submitNameTag(PoseStack pose, Vec3 attachment, int i, Component component, boolean discrete, int light, double distSq, CameraRenderState camera) {
        collector.submitNameTag(pose, attachment, i, component, discrete, light, distSq, camera);
    }

    @Override
    public void submitText(PoseStack pose, float x, float y, FormattedCharSequence text, boolean shadow, DisplayMode mode, int color, int backgroundColor, int light, int packed) {
        collector.submitText(pose, x, y, text, shadow, mode, color, backgroundColor, light, packed);
    }

    @Override
    public void submitFlame(PoseStack pose, EntityRenderState state, Quaternionf rotation) {
        collector.submitFlame(pose, state, rotation);
    }

    @Override
    public void submitLeash(PoseStack pose, LeashState leashState) {
        collector.submitLeash(pose, leashState);
    }

    private static boolean consoleskins$shouldOffsetElytra(Model<?> model, Object state) {
        if (!ConsoleSkinsClientSettings.isSkinAnimations()) return false;
        if (!(model instanceof net.minecraft.client.model.ElytraModel)) return false;
        if (!(state instanceof RenderStateSkinIdAccess a)) return false;

        String skinId = null;
        try {
            skinId = a.consoleskins$getSkinId();
        } catch (Throwable ignored) {
        }
        if (skinId == null || skinId.isBlank()) return false;
        if (!BadSantaSitConfig.isIdleSitSkin(skinId)) return false;

        UUID uuid = null;
        try {
            uuid = a.consoleskins$getEntityUuid();
        } catch (Throwable ignored) {
        }
        if (uuid == null) return false;

        return IdleSitTracker.isSitting(uuid);
    }

    private static boolean consoleskins$shouldFlipForUpsideDown(Model<?> model, Object state) {

        if (!(model instanceof net.minecraft.client.model.ElytraModel)) return false;
        if (!(state instanceof RenderStateSkinIdAccess a)) return false;
        try {
            java.util.UUID uuid = a.consoleskins$getEntityUuid();
            return wily.legacy.CustomModelSkins.cpm.shared.util.UpsideDownModelFix.isFlipped(uuid);
        } catch (Throwable ignored) {
            return false;
        }
    }

    private static void consoleskins$pushElytraOffset(PoseStack pose) {
        pose.pushPose();
        pose.translate(0.0D, (double) (IdleSitPose.BODY_DOWN / 16.0F), 0.0D);
    }

    private static void consoleskins$popElytraOffset(PoseStack pose) {
        pose.popPose();
    }

    private static void consoleskins$applyUpsideDownFlip(PoseStack pose) {

        pose.mulPose(new org.joml.Quaternionf().rotationZ((float) Math.PI));
        pose.translate(0.0D, -1.0D, 0.0D);
    }

    @Override
    public <S> void submitModel(Model<? super S> model, S state, PoseStack pose, RenderType type, int light, int overlay, int tint, TextureAtlasSprite sprite, int outline, CrumblingOverlay crumble) {
        boolean flip = consoleskins$shouldFlipForUpsideDown(model, state);
        boolean offset = consoleskins$shouldOffsetElytra(model, state);
        if (flip || offset) {
            pose.pushPose();
            if (flip) consoleskins$applyUpsideDownFlip(pose);
            if (offset) pose.translate(0.0D, (double) (IdleSitPose.BODY_DOWN / 16.0F), 0.0D);
        }
        try {
            SelfRenderer sr = ModelPartHooks.getSelfRenderer(model.root());
            if (sr != null) {
                wily.legacy.CustomModelSkins.cpm.shared.util.Log.info("[CPM_DEBUG] submitModel intercepted for: " + model.getClass().getSimpleName());
                model.setupAnim(state);
                sr.submitSelf(new RenderCollector(pose, collector, type, light, overlay, tint, outline, sprite, state));
            } else {
                collector.submitModel(model, state, pose, type, light, overlay, tint, sprite, outline, crumble);
            }
        } finally {
            if (flip || offset) pose.popPose();
        }
    }

    @Override
    public <S> void submitModel(Model<? super S> model, S state, PoseStack pose, RenderType type, int light, int overlay, int outline, CrumblingOverlay crumble) {
        boolean flip = consoleskins$shouldFlipForUpsideDown(model, state);
        boolean offset = consoleskins$shouldOffsetElytra(model, state);
        if (flip || offset) {
            pose.pushPose();
            if (flip) consoleskins$applyUpsideDownFlip(pose);
            if (offset) pose.translate(0.0D, (double) (IdleSitPose.BODY_DOWN / 16.0F), 0.0D);
        }
        try {
            SelfRenderer sr = ModelPartHooks.getSelfRenderer(model.root());
            if (sr != null) {
                model.setupAnim(state);
                sr.submitSelf(new RenderCollector(pose, collector, type, light, overlay, -1, outline, null, state));
            } else {
                collector.submitModel(model, state, pose, type, light, overlay, outline, crumble);
            }
        } finally {
            if (flip || offset) pose.popPose();
        }
    }

    @Override
    public void submitModelPart(ModelPart part, PoseStack pose, RenderType type, int light, int overlay, TextureAtlasSprite sprite) {
        SelfRenderer sr = ModelPartHooks.getSelfRenderer(part);
        if (sr != null) {
            wily.legacy.CustomModelSkins.cpm.shared.util.Log.info("[CPM_DEBUG] submitModelPart intercepted");
            sr.submitSelf(new RenderCollector(pose, collector, type, light, overlay, -1, 0, sprite, null));
        } else {
            collector.submitModelPart(part, pose, type, light, overlay, sprite);
        }
    }

    @Override
    public void submitModelPart(ModelPart part, PoseStack pose, RenderType type, int light, int overlay, TextureAtlasSprite sprite, int tint, CrumblingOverlay crumble) {
        SelfRenderer sr = ModelPartHooks.getSelfRenderer(part);
        if (sr != null) {
            sr.submitSelf(new RenderCollector(pose, collector, type, light, overlay, tint, 0, sprite, null));
        } else {
            collector.submitModelPart(part, pose, type, light, overlay, sprite, tint, crumble);
        }
    }

    @Override
    public void submitModelPart(ModelPart part, PoseStack pose, RenderType type, int light, int overlay, TextureAtlasSprite sprite, boolean b1, boolean b2) {
        SelfRenderer sr = ModelPartHooks.getSelfRenderer(part);
        if (sr != null) {
            sr.submitSelf(new RenderCollector(pose, collector, type, light, overlay, -1, 0, sprite, null));
        } else {
            collector.submitModelPart(part, pose, type, light, overlay, sprite, b1, b2);
        }
    }

    @Override
    public void submitModelPart(ModelPart part, PoseStack pose, RenderType type, int light, int overlay, TextureAtlasSprite sprite, boolean b1, boolean b2, int tint, CrumblingOverlay crumble, int outline) {
        SelfRenderer sr = ModelPartHooks.getSelfRenderer(part);
        if (sr != null) {
            sr.submitSelf(new RenderCollector(pose, collector, type, light, overlay, tint, outline, sprite, null));
        } else {
            collector.submitModelPart(part, pose, type, light, overlay, sprite, b1, b2, tint, crumble, outline);
        }
    }

    @Override
    public void submitBlock(PoseStack pose, BlockState state, int i1, int i2, int i3) {
        collector.submitBlock(pose, state, i1, i2, i3);
    }

    @Override
    public void submitMovingBlock(PoseStack pose, MovingBlockRenderState state) {
        collector.submitMovingBlock(pose, state);
    }

    @Override
    public void submitBlockModel(PoseStack pose, RenderType type, BlockStateModel model, float r, float g, float b, int light, int overlay, int packed) {
        collector.submitBlockModel(pose, type, model, r, g, b, light, overlay, packed);
    }

    @Override
    public void submitItem(PoseStack pose, ItemDisplayContext ctx, int light, int overlay, int packed, int[] ints, List<BakedQuad> quads, RenderType type, FoilType foil) {
        collector.submitItem(pose, ctx, light, overlay, packed, ints, quads, type, foil);
    }

    @Override
    public void submitCustomGeometry(PoseStack pose, RenderType type, CustomGeometryRenderer renderer) {
        collector.submitCustomGeometry(pose, type, renderer);
    }

    @Override
    public void submitParticleGroup(ParticleGroupRenderer renderer) {
        collector.submitParticleGroup(renderer);
    }

    @Override
    public void submitHitbox(PoseStack pose, EntityRenderState state, HitboxesRenderState hitboxes) {
        collector.submitHitbox(pose, state, hitboxes);
    }

    public static class CPMSubmitNodeCollector extends CPMOrderedSubmitNodeCollector implements SubmitNodeCollector {
        private final SubmitNodeCollector collector;

        public CPMSubmitNodeCollector(SubmitNodeCollector collector) {
            super(collector);
            this.collector = collector;
        }

        @Override
        public OrderedSubmitNodeCollector order(int order) {
            return new CPMOrderedSubmitNodeCollector(collector.order(order));
        }

        public static void injectSNC(LocalRef<SubmitNodeCollector> snc) {
            var c = snc.get();
            if (c instanceof CPMSubmitNodeCollector) return;
            snc.set(new CPMSubmitNodeCollector(c));
        }
    }
}
