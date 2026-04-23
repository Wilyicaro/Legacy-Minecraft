package wily.legacy.mixin.base.skins.client;

import net.minecraft.client.Minecraft;
import net.minecraft.client.model.PlayerModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.renderer.entity.state.AvatarRenderState;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import wily.legacy.Skins.client.render.PlayerModelParts;
import wily.legacy.Skins.client.render.RenderStateSkinIdAccess;
import wily.legacy.Skins.client.render.boxloader.AttachSlot;
import wily.legacy.Skins.client.render.boxloader.BoxModelManager;
import wily.legacy.Skins.client.render.boxloader.BuiltBoxModel;
import wily.legacy.Skins.skin.ClientSkinAssets;
import wily.legacy.Skins.skin.SkinIdUtil;
import wily.legacy.compat.cpm.CpmRenderCompat;
import java.util.EnumMap;
import java.util.*;

@Mixin(PlayerModel.class)
public abstract class PlayerPartsMixin {
    @Unique private boolean consoleskins$specBoxHeadInstalled;
    @Unique private String consoleskins$specBoxHeadSkinId;
    @Unique private Map<String, ModelPart> consoleskins$specHeadChildrenBackup, consoleskins$specHatChildrenBackup;
    @Unique private boolean consoleskins$specHeadSkipDrawBackup, consoleskins$specHatSkipDrawBackup;
    @Unique private final float[][] consoleskins$prevOffsets = new float[PlayerModelParts.ALL.length][];
    private static void consoleskins$resetPart(ModelPart part) { consoleskins$setSkipDraw(part, false); }
    private static void consoleskins$hidePart(ModelPart part) { consoleskins$setSkipDraw(part, true); }
    private static void consoleskins$setSkipDraw(ModelPart part, boolean skip) {
        if (part == null) return;
        part.visible = true;
        ((ModelPartAccessor) (Object) part).consoleskins$setSkipDraw(skip);
    }
    @Inject(method = "setupAnim(Lnet/minecraft/client/renderer/entity/state/AvatarRenderState;)V", at = @At("HEAD"), require = 0)
    private void consoleskins$undoVisualOffsets(AvatarRenderState state, CallbackInfo ci) {
        PlayerModel self = (PlayerModel) (Object) this;
        for (int i = 0; i < PlayerModelParts.ALL.length; i++) {
            AttachSlot slot = PlayerModelParts.ALL[i];
            consoleskins$applyOffset(self, slot, consoleskins$prevOffsets[i], -1.0F);
            consoleskins$prevOffsets[i] = null;
            consoleskins$resetScale(self, slot);
        }
    }
    @Inject(method = "setupAnim(Lnet/minecraft/client/renderer/entity/state/AvatarRenderState;)V", at = @At("TAIL"), require = 0)
    private void consoleskins$hidePartsForBoxModels(AvatarRenderState state, CallbackInfo ci) {
        PlayerModel self = (PlayerModel) (Object) this;
        if (CpmRenderCompat.isCpmModelActive(state)) {
            consoleskins$uninstallSpectatorBoxHead(self);
            consoleskins$resetAllParts(self);
            return;
        }
        RenderStateSkinIdAccess access = state instanceof RenderStateSkinIdAccess skinAccess ? skinAccess : null;
        UUID uuid = access == null ? null : access.consoleskins$getEntityUuid();
        String skinId = access == null ? null : access.consoleskins$getSkinId();
        boolean spectator = consoleskins$isSpectator(uuid);
        if (!spectator) consoleskins$uninstallSpectatorBoxHead(self);
        if (spectator) {
            consoleskins$applySpectatorBoxHeadIfNeeded(self, uuid, skinId);
            return;
        }
        consoleskins$resetAllParts(self);
        if (SkinIdUtil.isBlankOrAutoSelect(skinId)) return;
        ClientSkinAssets.ResolvedSkin resolved = ClientSkinAssets.resolveSkin(access);
        if (resolved == null) return;
        consoleskins$applyVisualOffsets(self, resolved.modelId());
        BuiltBoxModel model = resolved.boxModel();
        if (model == null) return;
        consoleskins$hidePair(self, model, AttachSlot.HEAD, AttachSlot.HAT);
        consoleskins$hidePair(self, model, AttachSlot.BODY, AttachSlot.JACKET);
        consoleskins$hidePair(self, model, AttachSlot.RIGHT_ARM, AttachSlot.RIGHT_SLEEVE);
        consoleskins$hidePair(self, model, AttachSlot.LEFT_ARM, AttachSlot.LEFT_SLEEVE);
        consoleskins$hidePair(self, model, AttachSlot.RIGHT_LEG, AttachSlot.RIGHT_PANTS);
        consoleskins$hidePair(self, model, AttachSlot.LEFT_LEG, AttachSlot.LEFT_PANTS);
    }
    @Unique
    private static void consoleskins$resetAllParts(PlayerModel self) {
        for (AttachSlot slot : PlayerModelParts.ALL) { consoleskins$resetPart(PlayerModelParts.get(self, slot)); }
    }
    @Unique
    private static void consoleskins$hidePair(PlayerModel self, BuiltBoxModel model, AttachSlot base, AttachSlot overlay) {
        var basePart = PlayerModelParts.get(self, base);
        var overlayPart = PlayerModelParts.get(self, overlay);
        if (model.hides(base)) {
            consoleskins$hidePart(basePart);
            consoleskins$hidePart(overlayPart);
        }
        if (model.hides(overlay)) { consoleskins$hidePart(overlayPart); }
    }
    @Unique
    private void consoleskins$applySpectatorBoxHeadIfNeeded(PlayerModel self, UUID uuid, String skinId) {
        if (SkinIdUtil.isBlankOrAutoSelect(skinId)) {
            consoleskins$uninstallSpectatorBoxHead(self);
            return;
        }
        ClientSkinAssets.ResolvedSkin resolved = ClientSkinAssets.resolveSkin(skinId, uuid);
        if (!ClientSkinAssets.hasHeadBox(resolved)) {
            consoleskins$uninstallSpectatorBoxHead(self);
            return;
        }
        BuiltBoxModel model = resolved.boxModel();
        boolean hasHeadParts = model.get(AttachSlot.HEAD) != null && !model.get(AttachSlot.HEAD).isEmpty();
        boolean hasHatParts = model.get(AttachSlot.HAT) != null && !model.get(AttachSlot.HAT).isEmpty();
        if (consoleskins$specBoxHeadInstalled && skinId.equals(consoleskins$specBoxHeadSkinId)) {
            consoleskins$setSkipDraw(self.head, true);
            consoleskins$setSkipDraw(self.hat, true);
            return;
        }
        consoleskins$uninstallSpectatorBoxHead(self);
        ModelPartAccessor head = (ModelPartAccessor) (Object) self.head;
        ModelPartAccessor hat = (ModelPartAccessor) (Object) self.hat;
        consoleskins$specHeadSkipDrawBackup = head.consoleskins$getSkipDraw();
        consoleskins$specHatSkipDrawBackup = hat.consoleskins$getSkipDraw();
        consoleskins$specHeadChildrenBackup = new HashMap<>(head.consoleskins$getChildren());
        consoleskins$specHatChildrenBackup = new HashMap<>(hat.consoleskins$getChildren());
        Map<String, ModelPart> headChildren = head.consoleskins$getChildren();
        Map<String, ModelPart> hatChildren = hat.consoleskins$getChildren();
        consoleskins$removeChildrenWithPrefix(headChildren, "consoleskins$spec_head_");
        consoleskins$removeChildrenWithPrefix(hatChildren, "consoleskins$spec_hat_");
        if (hasHeadParts) {
            int index = 0;
            for (ModelPart part : model.get(AttachSlot.HEAD)) { headChildren.put("consoleskins$spec_head_" + index++, part); }
        }
        if (hasHatParts) {
            int index = 0;
            for (ModelPart part : model.get(AttachSlot.HAT)) { hatChildren.put("consoleskins$spec_hat_" + index++, part); }
        }
        consoleskins$setSkipDraw(self.head, true);
        consoleskins$setSkipDraw(self.hat, true);
        consoleskins$specBoxHeadInstalled = true;
        consoleskins$specBoxHeadSkinId = skinId;
    }
    @Unique
    private void consoleskins$uninstallSpectatorBoxHead(PlayerModel self) {
        if (!consoleskins$specBoxHeadInstalled) return;
        ModelPartAccessor head = (ModelPartAccessor) (Object) self.head;
        ModelPartAccessor hat = (ModelPartAccessor) (Object) self.hat;
        head.consoleskins$getChildren().clear();
        head.consoleskins$getChildren().putAll(consoleskins$specHeadChildrenBackup);
        hat.consoleskins$getChildren().clear();
        hat.consoleskins$getChildren().putAll(consoleskins$specHatChildrenBackup);
        consoleskins$setSkipDraw(self.head, consoleskins$specHeadSkipDrawBackup);
        consoleskins$setSkipDraw(self.hat, consoleskins$specHatSkipDrawBackup);
        consoleskins$specHeadChildrenBackup = null;
        consoleskins$specHatChildrenBackup = null;
        consoleskins$specHeadSkipDrawBackup = false;
        consoleskins$specHatSkipDrawBackup = false;
        consoleskins$specBoxHeadInstalled = false;
        consoleskins$specBoxHeadSkinId = null;
    }
    @Unique
    private static void consoleskins$removeChildrenWithPrefix(Map<String, ModelPart> parts, String prefix) { parts.keySet().removeIf(key -> key.startsWith(prefix)); }
    @Unique
    private void consoleskins$applyVisualOffsets(PlayerModel self, ResourceLocation modelId) {
        if (modelId == null) return;
        EnumMap<AttachSlot, float[]> offsets = BoxModelManager.getOffsets(modelId);
        EnumMap<AttachSlot, float[]> scales = BoxModelManager.getScales(modelId);
        if ((offsets == null || offsets.isEmpty()) && (scales == null || scales.isEmpty())) return;
        for (int i = 0; i < PlayerModelParts.ALL.length; i++) {
            AttachSlot slot = PlayerModelParts.ALL[i];
            float[] offset = offsets == null ? null : offsets.get(slot);
            consoleskins$applyOffset(self, slot, offset, 1.0F);
            consoleskins$prevOffsets[i] = offset == null ? null : offset.clone();
            consoleskins$applyScale(self, slot, scales == null ? null : scales.get(slot));
        }
    }
    @Unique
    private static void consoleskins$applyOffset(PlayerModel self, AttachSlot slot, float[] offset, float scale) {
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
    @Unique
    private static boolean consoleskins$isSpectator(UUID uuid) {
        if (uuid == null) return false;
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.level == null) return false;
        Player player = minecraft.level.getPlayerByUUID(uuid);
        return player != null && player.isSpectator();
    }
}
