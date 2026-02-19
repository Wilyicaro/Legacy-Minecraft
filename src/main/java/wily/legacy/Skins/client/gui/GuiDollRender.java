package wily.legacy.Skins.client.gui;

import wily.legacy.Skins.client.render.RenderStateSkinIdAccess;
import wily.legacy.Skins.client.render.ZombieArmsConfig;
import wily.legacy.Skins.client.util.ConsoleSkinsClientSettings;
import com.mojang.authlib.GameProfile;
import wily.legacy.CustomModelSkins.cpm.client.CustomPlayerModelsClient;
import wily.legacy.CustomModelSkins.cpm.client.PlayerProfile;
import wily.legacy.CustomModelSkins.cpm.client.PlayerRenderStateAccess;
import wily.legacy.CustomModelSkins.cpm.shared.config.Player;
import wily.legacy.CustomModelSkins.cpm.shared.definition.ModelDefinitionLoader;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.renderer.entity.state.AvatarRenderState;
import net.minecraft.core.ClientAsset;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.entity.player.PlayerModelType;
import net.minecraft.world.entity.player.PlayerSkin;
import org.joml.Quaternionf;
import org.joml.Vector3f;

import java.util.UUID;

public final class GuiDollRender {
    private static final int DOLL_RENDER_STATE_ID = DollRenderIds.MENU_DOLL_ID;
    public static final int CPM_TOP_CLIP_HEADROOM_PX = 240;

    private GuiDollRender() {
    }

    public static void renderDollInRect(GuiGraphics gui, String selectionId, PlayerSkin skin, float yawOffset, boolean crouching, float attackTime, float partialTick, int left, int top, int right, int bottom, int sizeCap) {
        final float BASE_BBOX_H = 1.8F;
        final float BASE_BBOX_W = 0.6F;
        final float BASE_TRANSLATE_Y = BASE_BBOX_H / 2.0F;
        AvatarRenderState state = new AvatarRenderState();
        state.id = DOLL_RENDER_STATE_ID;
        state.entityType = EntityType.PLAYER;
        state.lightCoords = 15728880;
        state.boundingBoxHeight = BASE_BBOX_H;
        state.boundingBoxWidth = BASE_BBOX_W;
        float yaw = 180.0F + yawOffset;
        state.bodyRot = yaw;
        state.yRot = yaw;
        state.xRot = 0.0F;
        state.pose = crouching ? Pose.CROUCHING : Pose.STANDING;
        state.isBaby = false;
        state.scale = 1.0F;
        state.ageScale = 1.0F;
        state.showHat = true;
        state.showJacket = true;
        state.showLeftSleeve = true;
        state.showRightSleeve = true;
        state.showLeftPants = true;
        state.showRightPants = true;
        state.showCape = false;
        state.attackArm = HumanoidArm.RIGHT;
        state.attackTime = attackTime;
        state.isCrouching = crouching;        state.skin = skin;
        int height = bottom - top;
        int size = Math.min((int) (height / 2.75F), sizeCap);
        size = Math.max(size, 20);
        float renderScale = (float) size;
        Vector3f translate = new Vector3f(0.0F, BASE_TRANSLATE_Y, 0.0F);
        Quaternionf quat = new Quaternionf().rotationZ((float) Math.toRadians(180.0F));
        Quaternionf quat2 = new Quaternionf().rotationX(0);
        quat.mul(quat2);
        quat2.conjugate();
        gui.submitEntityRenderState(state, renderScale, translate, quat, quat2, left, top, right, bottom);
    }

    public static void renderDollInRect(GuiGraphics gui, String selectionId, ResourceLocation skinTexture, float yawOffset, boolean crouching, float attackTime, float partialTick, int left, int top, int right, int bottom, int sizeCap) {
        final float BASE_BBOX_H = 1.8F;
        final float BASE_BBOX_W = 0.6F;
        final float BASE_TRANSLATE_Y = BASE_BBOX_H / 2.0F;
        AvatarRenderState state = new AvatarRenderState();
        boolean isCpmModel = selectionId != null && selectionId.startsWith("cpm:");
        try {
            Minecraft.getInstance().getTextureManager().getTexture(skinTexture);
        } catch (Throwable ignored) {
        }
        if (state instanceof RenderStateSkinIdAccess a) a.consoleskins$setSkinId(selectionId);
        if (ConsoleSkinsClientSettings.isSkinAnimations() && ZombieArmsConfig.isZombieArmsSkin(selectionId)) {
            state.leftArmPose = HumanoidModel.ArmPose.EMPTY;
            state.rightArmPose = HumanoidModel.ArmPose.EMPTY;
        }
        state.id = DOLL_RENDER_STATE_ID;
        state.entityType = EntityType.PLAYER;
        state.lightCoords = 15728880;
        state.boundingBoxHeight = BASE_BBOX_H;
        state.boundingBoxWidth = BASE_BBOX_W;
        float yaw = 180.0F + yawOffset;
        state.bodyRot = yaw;
        state.yRot = yaw;
        state.xRot = 0.0F;
        state.pose = crouching ? Pose.CROUCHING : Pose.STANDING;
        state.isBaby = false;
        state.scale = 1.0F;
        state.ageScale = 1.0F;
        state.showHat = true;
        state.showJacket = true;
        state.showLeftSleeve = true;
        state.showRightSleeve = true;
        state.showLeftPants = true;
        state.showRightPants = true;
        state.showCape = false;
        state.attackArm = HumanoidArm.RIGHT;
        state.attackTime = attackTime;
        state.isCrouching = crouching;        ClientAsset.Texture body = new ClientAsset.ResourceTexture(skinTexture, skinTexture);
        PlayerModelType model = isAlexLike(selectionId, skinTexture) ? PlayerModelType.SLIM : PlayerModelType.WIDE;
        PlayerSkin skin = PlayerSkin.insecure(body, body, body, model);
        state.skin = skin;
        if (isCpmModel) {
            GuiCpmPreviewCache.prewarmMenuPreview(selectionId, skinTexture);
            if (GuiCpmPreviewCache.isReady(selectionId, skinTexture)) {
                try {
                    boolean prevInGui = PlayerProfile.inGui;
                    PlayerProfile.inGui = true;
                    GameProfile gp = GuiCpmPreviewCache.getOrCreateCpmProfile(selectionId, skinTexture);
                    PlayerRenderStateAccess sa = (PlayerRenderStateAccess) (Object) state;
                    Player<?> pl = CustomPlayerModelsClient.mc.getDefinitionLoader().loadPlayer(gp, ModelDefinitionLoader.PLAYER_UNIQUE);
                    if (pl != null && pl.getModelDefinition() != null) {
                        sa.cpm$setPlayer((Player) pl);
                    } else {
                        sa.cpm$setPlayer(null);
                    }
                    PlayerProfile.inGui = prevInGui;
                } catch (Throwable ignored) {
                }
            }
        }
        int height = bottom - top;
        int size = Math.min((int) (height / 2.75F), sizeCap);
        size = Math.max(size, 20);
        float renderScale = (float) size;
        int renderTop = top;
        float headroomTranslateY = 0.0F;
        if (isCpmModel) {
            int desiredTop = top - CPM_TOP_CLIP_HEADROOM_PX;
            renderTop = Math.max(0, desiredTop);
            int headroomPx = top - renderTop;
            if (headroomPx > 0 && renderScale > 0.01F) {
                headroomTranslateY = (headroomPx * 0.5F) / renderScale;
            }
        }
        Vector3f translate = new Vector3f(0.0F, BASE_TRANSLATE_Y + headroomTranslateY, 0.0F);
        Quaternionf quat = new Quaternionf().rotationZ((float) Math.toRadians(180.0F));
        Quaternionf quat2 = new Quaternionf().rotationX(0);
        quat.mul(quat2);
        quat2.conjugate();
        gui.submitEntityRenderState(state, renderScale, translate, quat, quat2, left, renderTop, right, bottom);
    }

    private static boolean isAlexLike(String selectionId, ResourceLocation skinTexture) {
        String sid = selectionId == null ? "" : selectionId.toLowerCase(java.util.Locale.ROOT);
        if (sid.contains("alex")) return true;
        String path = skinTexture == null ? "" : skinTexture.getPath().toLowerCase(java.util.Locale.ROOT);
        return path.contains("alex");
    }
}
