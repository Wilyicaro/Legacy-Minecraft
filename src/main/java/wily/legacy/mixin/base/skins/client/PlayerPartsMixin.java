package wily.legacy.mixin.base.skins.client;

import net.minecraft.client.Minecraft;
import net.minecraft.client.model.PlayerModel;
import net.minecraft.client.model.geom.ModelPart;
//? if <1.21.2 {
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.world.entity.LivingEntity;
//?} else {
/*import net.minecraft.client.renderer.entity.state.PlayerRenderState;
*///?}
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import wily.legacy.compat.cpm.CpmRenderCompat;
import wily.legacy.skins.client.render.PlayerModelParts;
import wily.legacy.skins.client.render.RenderStateSkinIdAccess;
import wily.legacy.skins.client.render.boxloader.AttachSlot;
import wily.legacy.skins.client.render.boxloader.BoxModelManager;
import wily.legacy.skins.client.render.boxloader.BuiltBoxModel;
import wily.legacy.skins.skin.ClientSkinAssets;
import wily.legacy.skins.skin.ClientSkinCache;
import wily.legacy.skins.skin.SkinFairness;
import wily.legacy.skins.skin.SkinIdUtil;

import java.util.*;

@Mixin(PlayerModel.class)
public abstract class PlayerPartsMixin {
    @Unique
    private final float[][] consoleskins$prevOffsets = new float[PlayerModelParts.ALL.length][];
    @Unique
    private boolean consoleskins$specBoxHeadInstalled;
    @Unique
    private String consoleskins$specBoxHeadSkinId;
    @Unique
    private Map<String, ModelPart> consoleskins$specHeadChildrenBackup, consoleskins$specHatChildrenBackup;
    @Unique
    private boolean consoleskins$specHeadSkipDrawBackup, consoleskins$specHatSkipDrawBackup;

    @Unique
    private static void consoleskins$resetPart(ModelPart part) {
        consoleskins$setSkipDraw(part, false);
    }

    @Unique
    private static void consoleskins$hidePart(ModelPart part) {
        consoleskins$setSkipDraw(part, true);
    }

    @Unique
    private static void consoleskins$setSkipDraw(ModelPart part, boolean skip) {
        if (part == null) return;
        part.visible = true;
        ((ModelPartAccessor) (Object) part).consoleskins$setSkipDraw(skip);
    }

    @Unique
    private static void consoleskins$resetAllParts(PlayerModel self) {
        for (AttachSlot slot : PlayerModelParts.ALL) {
            consoleskins$resetPart(PlayerModelParts.get(self, slot));
        }
    }

    @Unique
    private static void consoleskins$hidePair(PlayerModel self, BuiltBoxModel model, AttachSlot base, AttachSlot overlay) {
        ModelPart basePart = PlayerModelParts.get(self, base);
        ModelPart overlayPart = PlayerModelParts.get(self, overlay);
        if (model.hides(base)) {
            consoleskins$hidePart(basePart);
            consoleskins$hidePart(overlayPart);
        }
        if (model.hides(overlay)) consoleskins$hidePart(overlayPart);
    }

    @Unique
    private static void consoleskins$removeChildrenWithPrefix(Map<String, ModelPart> parts, String prefix) {
        parts.keySet().removeIf(key -> key.startsWith(prefix));
    }

    @Unique
    private static void consoleskins$applyOffset(PlayerModel self, AttachSlot slot, float[] offset, float scale) {
        ModelPart part = PlayerModelParts.get(self, slot);
        if (part == null || offset == null || offset.length < 3) return;
        part.x += offset[0] * scale;
        part.y += offset[1] * scale;
        part.z += offset[2] * scale;
    }

    @Unique
    private static void consoleskins$applyScale(PlayerModel self, AttachSlot slot, float[] scale) {
        ModelPart part = PlayerModelParts.get(self, slot);
        if (part == null || scale == null || scale.length < 3) return;
        part.xScale = scale[0];
        part.yScale = scale[1];
        part.zScale = scale[2];
    }

    @Unique
    private static void consoleskins$resetScale(PlayerModel self, AttachSlot slot) {
        ModelPart part = PlayerModelParts.get(self, slot);
        if (part == null) return;
        part.xScale = 1.0F;
        part.yScale = 1.0F;
        part.zScale = 1.0F;
    }

    //? if <1.21.2 {
    @Inject(method = "setupAnim(Lnet/minecraft/world/entity/LivingEntity;FFFFF)V", at = @At("HEAD"), require = 0)
    private void consoleskins$undoVisualOffsets(LivingEntity entity, float limbSwing, float limbSwingAmount, float ageInTicks, float netHeadYaw, float headPitch, CallbackInfo callbackInfo) {
        consoleskins$undoVisualOffsets();
    }

    @Inject(method = "setupAnim(Lnet/minecraft/world/entity/LivingEntity;FFFFF)V", at = @At("TAIL"), require = 0)
    private void consoleskins$hidePartsForBoxModels(LivingEntity entity, float limbSwing, float limbSwingAmount, float ageInTicks, float netHeadYaw, float headPitch, CallbackInfo callbackInfo) {
        PlayerModel self = (PlayerModel) (Object) this;
        if (!(entity instanceof AbstractClientPlayer player)) {
            consoleskins$uninstallSpectatorBoxHead(self);
            consoleskins$resetAllParts(self);
            return;
        }
        if (CpmRenderCompat.isCpmModelActive(self)) {
            consoleskins$uninstallSpectatorBoxHead(self);
            consoleskins$resetAllParts(self);
            return;
        }
        String skinId = SkinFairness.effectiveSkinId(Minecraft.getInstance(), ClientSkinCache.get(player.getUUID(), player.getScoreboardName()));
        boolean spectator = player.isSpectator();
        if (!spectator) consoleskins$uninstallSpectatorBoxHead(self);
        if (spectator) {
            consoleskins$applySpectatorBoxHeadIfNeeded(self, player.getUUID(), skinId);
            return;
        }
        consoleskins$resetAllParts(self);
        if (SkinIdUtil.isBlankOrAutoSelect(skinId)) return;
        ClientSkinAssets.ResolvedSkin resolved = ClientSkinAssets.resolveSkin(skinId, player.getUUID());
        consoleskins$applyBoxModel(self, resolved);
    }
    //?} else {
    /*@Inject(method = "setupAnim(Lnet/minecraft/client/renderer/entity/state/PlayerRenderState;)V", at = @At("HEAD"), require = 0)
    private void consoleskins$undoVisualOffsets(PlayerRenderState state, CallbackInfo callbackInfo) {
        consoleskins$undoVisualOffsets();
    }

    @Inject(method = "setupAnim(Lnet/minecraft/client/renderer/entity/state/PlayerRenderState;)V", at = @At("TAIL"), require = 0)
    private void consoleskins$hidePartsForBoxModels(PlayerRenderState state, CallbackInfo callbackInfo) {
        PlayerModel self = (PlayerModel) (Object) this;
        if (CpmRenderCompat.isCpmModelActive(state)) {
            consoleskins$uninstallSpectatorBoxHead(self);
            consoleskins$resetAllParts(self);
            return;
        }
        RenderStateSkinIdAccess access = state instanceof RenderStateSkinIdAccess skinAccess ? skinAccess : null;
        if (access == null) {
            consoleskins$uninstallSpectatorBoxHead(self);
            consoleskins$resetAllParts(self);
            return;
        }
        String skinId = access.consoleskins$getSkinId();
        boolean spectator = state.isSpectator;
        if (!spectator) consoleskins$uninstallSpectatorBoxHead(self);
        if (spectator) {
            consoleskins$applySpectatorBoxHeadIfNeeded(self, access.consoleskins$getEntityUuid(), skinId);
            return;
        }
        consoleskins$resetAllParts(self);
        if (SkinIdUtil.isBlankOrAutoSelect(skinId)) return;
        ClientSkinAssets.ResolvedSkin resolved = ClientSkinAssets.resolveSkin(access);
        consoleskins$applyBoxModel(self, resolved);
    }
    *///?}

    @Unique
    private void consoleskins$undoVisualOffsets() {
        PlayerModel self = (PlayerModel) (Object) this;
        for (int i = 0; i < PlayerModelParts.ALL.length; i++) {
            AttachSlot slot = PlayerModelParts.ALL[i];
            consoleskins$applyOffset(self, slot, consoleskins$prevOffsets[i], -1.0F);
            consoleskins$prevOffsets[i] = null;
            consoleskins$resetScale(self, slot);
        }
    }

    @Unique
    private void consoleskins$applyBoxModel(PlayerModel self, ClientSkinAssets.ResolvedSkin resolved) {
        if (resolved == null) return;
        BuiltBoxModel model = resolved.boxModel();
        consoleskins$applyVisualOffsets(self, resolved.modelId(), model);
        if (model == null) return;
        consoleskins$hidePair(self, model, AttachSlot.HEAD, AttachSlot.HAT);
        consoleskins$hidePair(self, model, AttachSlot.BODY, AttachSlot.JACKET);
        consoleskins$hidePair(self, model, AttachSlot.RIGHT_ARM, AttachSlot.RIGHT_SLEEVE);
        consoleskins$hidePair(self, model, AttachSlot.LEFT_ARM, AttachSlot.LEFT_SLEEVE);
        consoleskins$hidePair(self, model, AttachSlot.RIGHT_LEG, AttachSlot.RIGHT_PANTS);
        consoleskins$hidePair(self, model, AttachSlot.LEFT_LEG, AttachSlot.LEFT_PANTS);
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
            for (ModelPart part : model.get(AttachSlot.HEAD)) {
                headChildren.put("consoleskins$spec_head_" + index++, part);
            }
        }
        if (hasHatParts) {
            int index = 0;
            for (ModelPart part : model.get(AttachSlot.HAT)) {
                hatChildren.put("consoleskins$spec_hat_" + index++, part);
            }
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
    private void consoleskins$applyVisualOffsets(PlayerModel self, net.minecraft.resources.ResourceLocation modelId, BuiltBoxModel model) {
        if (modelId == null) return;
        EnumMap<AttachSlot, float[]> offsets = BoxModelManager.getOffsets(modelId);
        EnumMap<AttachSlot, float[]> scales = BoxModelManager.getScales(modelId);
        if ((offsets == null || offsets.isEmpty()) && (scales == null || scales.isEmpty())) return;
        for (int i = 0; i < PlayerModelParts.ALL.length; i++) {
            AttachSlot slot = PlayerModelParts.ALL[i];
            float[] offset = consoleskins$visualOffsetForSlot(offsets, model, slot);
            consoleskins$applyOffset(self, slot, offset, 1.0F);
            consoleskins$prevOffsets[i] = offset == null ? null : Arrays.copyOf(offset, offset.length);
            consoleskins$applyScale(self, slot, scales == null ? null : scales.get(slot));
        }
    }

    @Unique
    private static float[] consoleskins$visualOffsetForSlot(EnumMap<AttachSlot, float[]> offsets, BuiltBoxModel model, AttachSlot slot) {
        if (offsets == null || slot == null) return null;
        float[] offset = offsets.get(slot);
        AttachSlot base = consoleskins$overlayBase(slot);
        if (base == null) return offset;
        float[] baseOffset = offsets.get(base);
        if (baseOffset == null) return offset;
        if (offset == null || (consoleskins$isZeroOffset(offset) && !consoleskins$hasBoxParts(model, slot))) return baseOffset;
        return offset;
    }

    @Unique
    private static AttachSlot consoleskins$overlayBase(AttachSlot slot) {
        return switch (slot) {
            case HAT -> AttachSlot.HEAD;
            case JACKET -> AttachSlot.BODY;
            case RIGHT_SLEEVE -> AttachSlot.RIGHT_ARM;
            case LEFT_SLEEVE -> AttachSlot.LEFT_ARM;
            case RIGHT_PANTS -> AttachSlot.RIGHT_LEG;
            case LEFT_PANTS -> AttachSlot.LEFT_LEG;
            default -> null;
        };
    }

    @Unique
    private static boolean consoleskins$hasBoxParts(BuiltBoxModel model, AttachSlot slot) {
        List<ModelPart> parts = model == null || slot == null ? null : model.get(slot);
        return parts != null && !parts.isEmpty();
    }

    @Unique
    private static boolean consoleskins$isZeroOffset(float[] offset) {
        if (offset == null || offset.length < 3) return false;
        return Math.abs(offset[0]) < 1.0E-4F && Math.abs(offset[1]) < 1.0E-4F && Math.abs(offset[2]) < 1.0E-4F;
    }
}
