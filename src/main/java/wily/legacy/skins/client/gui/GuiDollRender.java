package wily.legacy.skins.client.gui;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.entity.state.AvatarRenderState;
import com.mojang.blaze3d.platform.Lighting;
import net.minecraft.core.ClientAsset;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.entity.player.PlayerSkin;
import wily.legacy.skins.client.render.RenderStateSkinIdAccess;
import wily.legacy.skins.skin.ClientSkinAssets;
import org.joml.Quaternionf;
import org.joml.Vector3f;

public final class GuiDollRender {
    public static final int MENU_DOLL_ID = -0x5D011;
    private static final int DOLL_RENDER_STATE_ID = MENU_DOLL_ID;
    private static final float BASE_BBOX_HEIGHT = 1.8F;
    private static final float BASE_BBOX_WIDTH = 0.6F;
    private static final float BASE_TRANSLATE_Y = BASE_BBOX_HEIGHT / 2.0F;
    private static final float SCALE_DIVISOR = 2.75F;
    private static final float CROUCH_Y_OFFSET = -0.125F;
    private static final int MIN_RENDER_SIZE = 20;

    private GuiDollRender() { }

    private static float normalizeYaw(float yaw) {
        while (yaw < 0.0F) yaw += 360.0F;
        return (yaw + 180.0F) % 360.0F - 180.0F;
    }

    public static void renderDollInRect(GuiGraphics gui, String selectionId, PlayerSkin skin, float yawOffset, boolean crouching, float attackTime, float partialTick, int left, int top, int right, int bottom, int sizeCap) {
        if (gui == null || skin == null) return;
        submit(gui, left, right, buildLayout(crouching, top, bottom, sizeCap), buildState(skin, selectionId, null, null, null, null, BASE_BBOX_HEIGHT, BASE_BBOX_WIDTH, yawOffset, crouching, attackTime, false));
    }

    public static void renderDollInRect(GuiGraphics gui, String selectionId, ResourceLocation skinTexture, float yawOffset, boolean crouching, float attackTime, float partialTick, int left, int top, int right, int bottom, int sizeCap) {
        if (gui == null || skinTexture == null) return;
        Minecraft mc = Minecraft.getInstance();
        if (mc != null) mc.getTextureManager().getTexture(skinTexture);

        ClientSkinAssets.ResolvedSkin resolved = ClientSkinAssets.resolveSkin(selectionId, skinTexture, null, null);
        var entry = resolved == null ? null : resolved.entry();
        boolean showCape = ClientSkinAssets.hasCape(resolved);
        ClientAsset.Texture body = new ClientAsset.ResourceTexture(skinTexture, skinTexture);
        ClientAsset.Texture cape = body;
        if (showCape && entry != null && entry.cape() != null) {
            if (mc != null) mc.getTextureManager().getTexture(entry.cape());
            cape = new ClientAsset.ResourceTexture(entry.cape(), entry.cape());
        }
        var built = resolved == null ? null : resolved.boxModel();
        float bboxHeight = built == null ? BASE_BBOX_HEIGHT : Math.max(BASE_BBOX_HEIGHT, built.bboxHeight());
        float bboxWidth = built == null ? BASE_BBOX_WIDTH : Math.max(BASE_BBOX_WIDTH, built.bboxWidth());
        ResourceLocation resolvedTexture = resolved != null && resolved.texture() != null ? resolved.texture() : skinTexture;
        ResourceLocation resolvedBoxTexture = resolved != null && resolved.boxTexture() != null ? resolved.boxTexture() : resolvedTexture;
        PlayerSkin skin = PlayerSkin.insecure(body, cape, body, ClientSkinAssets.resolveModelType(selectionId, resolved));
        submit(gui, left, right, buildLayout(crouching, top, bottom, sizeCap), buildState(skin, selectionId, resolvedTexture, resolvedBoxTexture, resolved == null ? null : resolved.modelId(), built, bboxHeight, bboxWidth, yawOffset, crouching, attackTime, showCape));
    }

    private static void submit(GuiGraphics gui, int left, int right, PreviewLayout layout, AvatarRenderState state) {
        applyPreviewLighting();
        gui.submitEntityRenderState(state, layout.scale(), layout.translate(), layout.bodyRotation(), layout.cameraRotation(), left, layout.expandedTop(), right, layout.expandedBottom());
    }

    private static AvatarRenderState buildState(PlayerSkin skin, String selectionId, ResourceLocation texture, ResourceLocation boxTexture, ResourceLocation modelId, wily.legacy.skins.client.render.boxloader.BuiltBoxModel boxModel, float bboxHeight, float bboxWidth, float yawOffset, boolean crouching, float attackTime, boolean showCape) {
        AvatarRenderState state = new AvatarRenderState();
        if (state instanceof RenderStateSkinIdAccess access) {
            access.consoleskins$setSkinId(selectionId);
            access.consoleskins$setCachedTexture(texture);
            access.consoleskins$setCachedBoxTexture(boxTexture);
            access.consoleskins$setCachedModelId(modelId);
            access.consoleskins$setCachedBoxModel(boxModel);
        }
        float yaw = normalizeYaw(yawOffset + 180.0F);
        state.id = DOLL_RENDER_STATE_ID;
        state.entityType = EntityType.PLAYER;
        state.lightCoords = 15728880;
        state.boundingBoxHeight = bboxHeight;
        state.boundingBoxWidth = bboxWidth;
        state.bodyRot = yaw;
        state.yRot = yaw;
        state.xRot = 0.0F;
        state.pose = Pose.STANDING;
        state.isBaby = false;
        state.scale = 1.0F;
        state.ageScale = 1.0F;
        state.showHat = true;
        state.showJacket = true;
        state.showLeftSleeve = true;
        state.showRightSleeve = true;
        state.showLeftPants = true;
        state.showRightPants = true;
        state.showCape = showCape;
        state.attackArm = HumanoidArm.RIGHT;
        state.attackTime = attackTime;
        state.isCrouching = crouching;
        state.skin = skin;
        return state;
    }

    private static PreviewLayout buildLayout(boolean crouching, int top, int bottom, int sizeCap) {
        int baseHeight = bottom - top;
        int size = Math.min((int) (baseHeight / SCALE_DIVISOR), sizeCap);
        size = Math.max(size, MIN_RENDER_SIZE);
        return new PreviewLayout(
                size,
                new Vector3f(0.0F, BASE_TRANSLATE_Y + (crouching ? CROUCH_Y_OFFSET : 0.0F), 0.0F),
                new Quaternionf().rotationZ((float) Math.PI),
                new Quaternionf(),
                top - baseHeight,
                bottom + baseHeight
        );
    }

    private static void applyPreviewLighting() {
        Minecraft mc = Minecraft.getInstance();
        if (mc != null) mc.gameRenderer.getLighting().setupFor(Lighting.Entry.ENTITY_IN_UI);
    }

    private record PreviewLayout(
            float scale,
            Vector3f translate,
            Quaternionf bodyRotation,
            Quaternionf cameraRotation,
            int expandedTop,
            int expandedBottom
    ) { }
}
