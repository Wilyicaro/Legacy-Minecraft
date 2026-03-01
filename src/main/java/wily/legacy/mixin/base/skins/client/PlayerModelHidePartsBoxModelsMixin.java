package wily.legacy.mixin.base.skins.client;

import com.google.gson.JsonObject;
import net.minecraft.client.model.PlayerModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.renderer.entity.state.AvatarRenderState;
import net.minecraft.resources.ResourceLocation;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import wily.legacy.Skins.client.render.RenderStateSkinIdAccess;
import wily.legacy.Skins.client.render.boxloader.AttachSlot;
import wily.legacy.Skins.client.render.boxloader.BoxModelManager;
import wily.legacy.Skins.client.render.boxloader.BuiltBoxModel;
import wily.legacy.Skins.skin.ClientSkinAssets;
import wily.legacy.Skins.skin.SkinEntry;
import wily.legacy.Skins.skin.SkinPackLoader;
import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.player.Player;
import java.lang.reflect.Field;
import java.util.UUID;

@Mixin(PlayerModel.class)
public abstract class PlayerModelHidePartsBoxModelsMixin {
private static void consoleskins$resetSkipDraw(ModelPart part) {
    if (part == null) return;
    ((ModelPartSkipDrawAccessorMixin) (Object) part).consoleskins$setSkipDraw(false);
}

private static boolean consoleskins$isSpectator(AvatarRenderState state) {
    if (state == null) return false;
    try {
        Field f = state.getClass().getField("isSpectator");
        if (f.getType() == boolean.class) return f.getBoolean(state);
    } catch (Throwable ignored) {
    }
    if (state instanceof RenderStateSkinIdAccess a) {
        UUID u = null;
        try {
            u = a.consoleskins$getEntityUuid();
        } catch (Throwable ignored) {
        }
        if (u != null) {
            Minecraft mc = Minecraft.getInstance();
            if (mc.level != null) {
                Player p = mc.level.getPlayerByUUID(u);
                if (p != null) return p.isSpectator();
            }
        }
    }
    return false;
}


    private static void consoleskins$resetPart(ModelPart part) {
        if (part == null) return;
        part.visible = true;
        ((ModelPartSkipDrawAccessorMixin) (Object) part).consoleskins$setSkipDraw(false);
    }

    private static void consoleskins$hidePart(ModelPart part) {
        if (part == null) return;
        part.visible = true;
        ((ModelPartSkipDrawAccessorMixin) (Object) part).consoleskins$setSkipDraw(true);
    }

    @Inject(method = "setupAnim(Lnet/minecraft/client/renderer/entity/state/AvatarRenderState;)V", at = @At("TAIL"), require = 0)
    private void consoleskins$hidePartsForBoxModels(AvatarRenderState state, CallbackInfo ci) {
        PlayerModel self = (PlayerModel) (Object) this;
if (consoleskins$isSpectator(state)) {
    consoleskins$resetSkipDraw(self.head);
    consoleskins$resetSkipDraw(self.hat);
    consoleskins$resetSkipDraw(self.body);
    consoleskins$resetSkipDraw(self.rightArm);
    consoleskins$resetSkipDraw(self.leftArm);
    consoleskins$resetSkipDraw(self.rightLeg);
    consoleskins$resetSkipDraw(self.leftLeg);
    consoleskins$resetSkipDraw(self.jacket);
    consoleskins$resetSkipDraw(self.rightSleeve);
    consoleskins$resetSkipDraw(self.leftSleeve);
    consoleskins$resetSkipDraw(self.rightPants);
    consoleskins$resetSkipDraw(self.leftPants);
    return;
}


        consoleskins$resetPart(self.head);
        consoleskins$resetPart(self.hat);
        consoleskins$resetPart(self.body);
        consoleskins$resetPart(self.rightArm);
        consoleskins$resetPart(self.leftArm);
        consoleskins$resetPart(self.rightLeg);
        consoleskins$resetPart(self.leftLeg);

        consoleskins$resetPart(self.jacket);
        consoleskins$resetPart(self.rightSleeve);
        consoleskins$resetPart(self.leftSleeve);
        consoleskins$resetPart(self.rightPants);
        consoleskins$resetPart(self.leftPants);

        if (!(state instanceof RenderStateSkinIdAccess a)) return;
        String skinId = a.consoleskins$getSkinId();
        if (skinId == null || skinId.isBlank() || "auto_select".equals(skinId)) return;

        SkinEntry entry = SkinPackLoader.getSkin(skinId);
        ResourceLocation tex = entry != null ? entry.texture() : null;
        if (tex == null) tex = ClientSkinAssets.getTexture(skinId);
        if (tex == null) return;

        String p = tex.getPath();
        int slash = p.lastIndexOf('/');
        if (slash != -1) p = p.substring(slash + 1);
        if (p.endsWith(".png")) p = p.substring(0, p.length() - 4);

        ResourceLocation modelId = ResourceLocation.fromNamespaceAndPath(tex.getNamespace(), p);
        BuiltBoxModel model = BoxModelManager.get(modelId);
        if (model == null) {
            JsonObject mj = ClientSkinAssets.getModelJson(skinId);
            if (mj != null) BoxModelManager.registerRuntime(modelId, mj);
            model = BoxModelManager.get(modelId);
        }
        if (model == null) return;

        if (model.hides(AttachSlot.HEAD)) {
            consoleskins$hidePart(self.head);
            consoleskins$hidePart(self.hat);
        }
        if (model.hides(AttachSlot.HAT)) consoleskins$hidePart(self.hat);

        if (model.hides(AttachSlot.BODY)) {
            consoleskins$hidePart(self.body);
            consoleskins$hidePart(self.jacket);
        }
        if (model.hides(AttachSlot.JACKET)) consoleskins$hidePart(self.jacket);

        if (model.hides(AttachSlot.RIGHT_ARM)) {
            consoleskins$hidePart(self.rightArm);
            consoleskins$hidePart(self.rightSleeve);
        }
        if (model.hides(AttachSlot.RIGHT_SLEEVE)) consoleskins$hidePart(self.rightSleeve);

        if (model.hides(AttachSlot.LEFT_ARM)) {
            consoleskins$hidePart(self.leftArm);
            consoleskins$hidePart(self.leftSleeve);
        }
        if (model.hides(AttachSlot.LEFT_SLEEVE)) consoleskins$hidePart(self.leftSleeve);

        if (model.hides(AttachSlot.RIGHT_LEG)) {
            consoleskins$hidePart(self.rightLeg);
            consoleskins$hidePart(self.rightPants);
        }
        if (model.hides(AttachSlot.RIGHT_PANTS)) consoleskins$hidePart(self.rightPants);

        if (model.hides(AttachSlot.LEFT_LEG)) {
            consoleskins$hidePart(self.leftLeg);
            consoleskins$hidePart(self.leftPants);
        }
        if (model.hides(AttachSlot.LEFT_PANTS)) consoleskins$hidePart(self.leftPants);
    }
}