package wily.legacy.mixin.base.skins.client;

import net.minecraft.client.model.PlayerModel;
import net.minecraft.client.renderer.entity.state.AvatarRenderState;
import net.minecraft.resources.ResourceLocation;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import wily.legacy.Skins.client.render.PlayerModelParts;
import wily.legacy.Skins.client.render.RenderStateSkinIdAccess;
import wily.legacy.Skins.client.render.boxloader.AttachSlot;
import wily.legacy.Skins.client.render.boxloader.BoxModelManager;
import wily.legacy.Skins.skin.ClientSkinAssets;
import wily.legacy.Skins.skin.SkinIdUtil;
import wily.legacy.compat.cpm.CpmRenderCompat;
import java.util.EnumMap;

@Mixin(PlayerModel.class)
public abstract class ModelOffsetsMixin {
    @Unique
    private final float[][] consoleskins$prevOffsets = new float[PlayerModelParts.ALL.length][];
    @Inject(method = "setupAnim(Lnet/minecraft/client/renderer/entity/state/AvatarRenderState;)V", at = @At("HEAD"), require = 0)
    private void consoleskins$undoVisualOffsets(AvatarRenderState state, CallbackInfo ci) {
        PlayerModel self = (PlayerModel) (Object) this;
        for (int i = 0; i < PlayerModelParts.ALL.length; i++) {
            consoleskins$apply(self, PlayerModelParts.ALL[i], consoleskins$prevOffsets[i], -1.0F);
            consoleskins$prevOffsets[i] = null;
            consoleskins$resetScale(self, PlayerModelParts.ALL[i]);
        }
    }
    @Inject(method = "setupAnim(Lnet/minecraft/client/renderer/entity/state/AvatarRenderState;)V", at = @At("TAIL"), require = 0)
    private void consoleskins$applyVisualOffsets(AvatarRenderState state, CallbackInfo ci) {
        PlayerModel self = (PlayerModel)(Object)this;
        if (CpmRenderCompat.isCpmModelActive(state)) return;
        if (!(state instanceof RenderStateSkinIdAccess a)) return;
        String skinId = a.consoleskins$getSkinId();
        if (SkinIdUtil.isBlankOrAutoSelect(skinId)) return;
        ClientSkinAssets.ResolvedSkin resolved = ClientSkinAssets.resolveSkin(
                skinId,
                a.consoleskins$getCachedTexture(),
                a.consoleskins$getCachedModelId(),
                null
        );
        ResourceLocation modelId = resolved == null ? null : resolved.modelId();
        if (modelId == null) return;
        EnumMap<AttachSlot, float[]> offsets = BoxModelManager.getOffsets(modelId);
        EnumMap<AttachSlot, float[]> scales = BoxModelManager.getScales(modelId);
        if ((offsets == null || offsets.isEmpty()) && (scales == null || scales.isEmpty())) return;
        for (int i = 0; i < PlayerModelParts.ALL.length; i++) {
            AttachSlot slot = PlayerModelParts.ALL[i];
            float[] offset = offsets == null ? null : offsets.get(slot);
            consoleskins$apply(self, slot, offset, 1.0F);
            consoleskins$prevOffsets[i] = offset == null ? null : offset.clone();
            consoleskins$applyScale(self, slot, scales == null ? null : scales.get(slot));
        }
    }
    @Unique
    private static void consoleskins$apply(PlayerModel self, AttachSlot slot, float[] offset, float scale) {
        var part = PlayerModelParts.get(self, slot);
        if (part == null || offset == null) return;
        part.x += offset[0] * scale;
        part.y += offset[1] * scale;
        part.z += offset[2] * scale;
    }
    @Unique
    private static void consoleskins$applyScale(PlayerModel self, AttachSlot slot, float[] scale) {
        var part = PlayerModelParts.get(self, slot);
        if (part == null || scale == null || scale.length < 3) return;
        part.xScale = scale[0];
        part.yScale = scale[1];
        part.zScale = scale[2];
    }
    @Unique
    private static void consoleskins$resetScale(PlayerModel self, AttachSlot slot) {
        var part = PlayerModelParts.get(self, slot);
        if (part == null) return;
        part.xScale = 1.0F;
        part.yScale = 1.0F;
        part.zScale = 1.0F;
    }
}
