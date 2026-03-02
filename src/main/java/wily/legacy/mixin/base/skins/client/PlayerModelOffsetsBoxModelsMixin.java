package wily.legacy.mixin.base.skins.client;

import com.google.gson.JsonObject;
import net.minecraft.client.model.PlayerModel;
import net.minecraft.client.renderer.entity.state.AvatarRenderState;
import net.minecraft.resources.ResourceLocation;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import wily.legacy.Skins.client.render.RenderStateSkinIdAccess;
import wily.legacy.Skins.client.render.boxloader.AttachSlot;
import wily.legacy.Skins.client.render.boxloader.BoxModelManager;
import wily.legacy.Skins.skin.ClientSkinAssets;
import wily.legacy.Skins.skin.SkinEntry;
import wily.legacy.Skins.skin.SkinPackLoader;

import java.util.EnumMap;

@Mixin(PlayerModel.class)
public abstract class PlayerModelOffsetsBoxModelsMixin {

    @Unique private float consoleskins$prevHeadX, consoleskins$prevHeadY, consoleskins$prevHeadZ;
    @Unique private float consoleskins$prevHatX, consoleskins$prevHatY, consoleskins$prevHatZ;
    @Unique private float consoleskins$prevBodyX, consoleskins$prevBodyY, consoleskins$prevBodyZ;
    @Unique private float consoleskins$prevRArmX, consoleskins$prevRArmY, consoleskins$prevRArmZ;
    @Unique private float consoleskins$prevLArmX, consoleskins$prevLArmY, consoleskins$prevLArmZ;
    @Unique private float consoleskins$prevRLegX, consoleskins$prevRLegY, consoleskins$prevRLegZ;
    @Unique private float consoleskins$prevLLegX, consoleskins$prevLLegY, consoleskins$prevLLegZ;
    @Unique private float consoleskins$prevJacketX, consoleskins$prevJacketY, consoleskins$prevJacketZ;
    @Unique private float consoleskins$prevRSleeveX, consoleskins$prevRSleeveY, consoleskins$prevRSleeveZ;
    @Unique private float consoleskins$prevLSleeveX, consoleskins$prevLSleeveY, consoleskins$prevLSleeveZ;
    @Unique private float consoleskins$prevRPantsX, consoleskins$prevRPantsY, consoleskins$prevRPantsZ;
    @Unique private float consoleskins$prevLPantsX, consoleskins$prevLPantsY, consoleskins$prevLPantsZ;

    @Inject(method = "setupAnim(Lnet/minecraft/client/renderer/entity/state/AvatarRenderState;)V", at = @At("HEAD"), require = 0)
    private void consoleskins$undoVisualOffsets(AvatarRenderState state, CallbackInfo ci) {
        PlayerModel self = (PlayerModel)(Object)this;

        self.head.x -= consoleskins$prevHeadX; self.head.y -= consoleskins$prevHeadY; self.head.z -= consoleskins$prevHeadZ;
        self.hat.x  -= consoleskins$prevHatX;  self.hat.y  -= consoleskins$prevHatY;  self.hat.z  -= consoleskins$prevHatZ;
        self.body.x -= consoleskins$prevBodyX; self.body.y -= consoleskins$prevBodyY; self.body.z -= consoleskins$prevBodyZ;
        self.rightArm.x -= consoleskins$prevRArmX; self.rightArm.y -= consoleskins$prevRArmY; self.rightArm.z -= consoleskins$prevRArmZ;
        self.leftArm.x  -= consoleskins$prevLArmX; self.leftArm.y  -= consoleskins$prevLArmY; self.leftArm.z  -= consoleskins$prevLArmZ;
        self.rightLeg.x -= consoleskins$prevRLegX; self.rightLeg.y -= consoleskins$prevRLegY; self.rightLeg.z -= consoleskins$prevRLegZ;
        self.leftLeg.x  -= consoleskins$prevLLegX; self.leftLeg.y  -= consoleskins$prevLLegY; self.leftLeg.z  -= consoleskins$prevLLegZ;

        self.jacket.x -= consoleskins$prevJacketX; self.jacket.y -= consoleskins$prevJacketY; self.jacket.z -= consoleskins$prevJacketZ;
        self.rightSleeve.x -= consoleskins$prevRSleeveX; self.rightSleeve.y -= consoleskins$prevRSleeveY; self.rightSleeve.z -= consoleskins$prevRSleeveZ;
        self.leftSleeve.x  -= consoleskins$prevLSleeveX; self.leftSleeve.y  -= consoleskins$prevLSleeveY; self.leftSleeve.z  -= consoleskins$prevLSleeveZ;
        self.rightPants.x  -= consoleskins$prevRPantsX;  self.rightPants.y  -= consoleskins$prevRPantsY;  self.rightPants.z  -= consoleskins$prevRPantsZ;
        self.leftPants.x   -= consoleskins$prevLPantsX;   self.leftPants.y   -= consoleskins$prevLPantsY;   self.leftPants.z   -= consoleskins$prevLPantsZ;

        consoleskins$prevHeadX = consoleskins$prevHeadY = consoleskins$prevHeadZ = 0f;
        consoleskins$prevHatX = consoleskins$prevHatY = consoleskins$prevHatZ = 0f;
        consoleskins$prevBodyX = consoleskins$prevBodyY = consoleskins$prevBodyZ = 0f;
        consoleskins$prevRArmX = consoleskins$prevRArmY = consoleskins$prevRArmZ = 0f;
        consoleskins$prevLArmX = consoleskins$prevLArmY = consoleskins$prevLArmZ = 0f;
        consoleskins$prevRLegX = consoleskins$prevRLegY = consoleskins$prevRLegZ = 0f;
        consoleskins$prevLLegX = consoleskins$prevLLegY = consoleskins$prevLLegZ = 0f;
        consoleskins$prevJacketX = consoleskins$prevJacketY = consoleskins$prevJacketZ = 0f;
        consoleskins$prevRSleeveX = consoleskins$prevRSleeveY = consoleskins$prevRSleeveZ = 0f;
        consoleskins$prevLSleeveX = consoleskins$prevLSleeveY = consoleskins$prevLSleeveZ = 0f;
        consoleskins$prevRPantsX = consoleskins$prevRPantsY = consoleskins$prevRPantsZ = 0f;
        consoleskins$prevLPantsX = consoleskins$prevLPantsY = consoleskins$prevLPantsZ = 0f;
    }

    @Inject(method = "setupAnim(Lnet/minecraft/client/renderer/entity/state/AvatarRenderState;)V", at = @At("TAIL"), require = 0)
    private void consoleskins$applyVisualOffsets(AvatarRenderState state, CallbackInfo ci) {
        PlayerModel self = (PlayerModel)(Object)this;

        if (!(state instanceof RenderStateSkinIdAccess a)) return;
        String skinId = a.consoleskins$getSkinId();
        if (skinId == null || skinId.isBlank() || "auto_select".equals(skinId)) return;

        SkinEntry entry = SkinPackLoader.getSkin(skinId);
        ResourceLocation tex = ClientSkinAssets.getTexture(skinId);
        if (tex == null && entry != null) tex = entry.texture();
        if (tex == null) return;

        String p = tex.getPath();
        int slash = p.lastIndexOf('/');
        if (slash != -1) p = p.substring(slash + 1);
        if (p.endsWith(".png")) p = p.substring(0, p.length() - 4);

        ResourceLocation modelId = ResourceLocation.fromNamespaceAndPath(tex.getNamespace(), p);
        EnumMap<AttachSlot, float[]> offsets = BoxModelManager.getOffsets(modelId);
        if (offsets == null || offsets.isEmpty()) {
            JsonObject mj = ClientSkinAssets.getModelJson(skinId);
            if (mj != null) BoxModelManager.registerRuntime(modelId, mj);
            offsets = BoxModelManager.getOffsets(modelId);
            if (offsets == null || offsets.isEmpty()) return;
        }

        apply(self.head, offsets.get(AttachSlot.HEAD));
        apply(self.hat, offsets.get(AttachSlot.HAT));
        apply(self.body, offsets.get(AttachSlot.BODY));
        apply(self.rightArm, offsets.get(AttachSlot.RIGHT_ARM));
        apply(self.leftArm, offsets.get(AttachSlot.LEFT_ARM));
        apply(self.rightLeg, offsets.get(AttachSlot.RIGHT_LEG));
        apply(self.leftLeg, offsets.get(AttachSlot.LEFT_LEG));

        apply(self.jacket, offsets.get(AttachSlot.JACKET));
        apply(self.rightSleeve, offsets.get(AttachSlot.RIGHT_SLEEVE));
        apply(self.leftSleeve, offsets.get(AttachSlot.LEFT_SLEEVE));
        apply(self.rightPants, offsets.get(AttachSlot.RIGHT_PANTS));
        apply(self.leftPants, offsets.get(AttachSlot.LEFT_PANTS));

        float[] v;
        v = offsets.get(AttachSlot.HEAD); if (v != null) { consoleskins$prevHeadX = v[0]; consoleskins$prevHeadY = v[1]; consoleskins$prevHeadZ = v[2]; }
        v = offsets.get(AttachSlot.HAT); if (v != null) { consoleskins$prevHatX = v[0]; consoleskins$prevHatY = v[1]; consoleskins$prevHatZ = v[2]; }
        v = offsets.get(AttachSlot.BODY); if (v != null) { consoleskins$prevBodyX = v[0]; consoleskins$prevBodyY = v[1]; consoleskins$prevBodyZ = v[2]; }
        v = offsets.get(AttachSlot.RIGHT_ARM); if (v != null) { consoleskins$prevRArmX = v[0]; consoleskins$prevRArmY = v[1]; consoleskins$prevRArmZ = v[2]; }
        v = offsets.get(AttachSlot.LEFT_ARM); if (v != null) { consoleskins$prevLArmX = v[0]; consoleskins$prevLArmY = v[1]; consoleskins$prevLArmZ = v[2]; }
        v = offsets.get(AttachSlot.RIGHT_LEG); if (v != null) { consoleskins$prevRLegX = v[0]; consoleskins$prevRLegY = v[1]; consoleskins$prevRLegZ = v[2]; }
        v = offsets.get(AttachSlot.LEFT_LEG); if (v != null) { consoleskins$prevLLegX = v[0]; consoleskins$prevLLegY = v[1]; consoleskins$prevLLegZ = v[2]; }
        v = offsets.get(AttachSlot.JACKET); if (v != null) { consoleskins$prevJacketX = v[0]; consoleskins$prevJacketY = v[1]; consoleskins$prevJacketZ = v[2]; }
        v = offsets.get(AttachSlot.RIGHT_SLEEVE); if (v != null) { consoleskins$prevRSleeveX = v[0]; consoleskins$prevRSleeveY = v[1]; consoleskins$prevRSleeveZ = v[2]; }
        v = offsets.get(AttachSlot.LEFT_SLEEVE); if (v != null) { consoleskins$prevLSleeveX = v[0]; consoleskins$prevLSleeveY = v[1]; consoleskins$prevLSleeveZ = v[2]; }
        v = offsets.get(AttachSlot.RIGHT_PANTS); if (v != null) { consoleskins$prevRPantsX = v[0]; consoleskins$prevRPantsY = v[1]; consoleskins$prevRPantsZ = v[2]; }
        v = offsets.get(AttachSlot.LEFT_PANTS); if (v != null) { consoleskins$prevLPantsX = v[0]; consoleskins$prevLPantsY = v[1]; consoleskins$prevLPantsZ = v[2]; }
    }

    @Unique
    private static void apply(net.minecraft.client.model.geom.ModelPart part, float[] v) {
        if (part == null || v == null) return;
        part.x += v[0];
        part.y += v[1];
        part.z += v[2];
    }
}
