package wily.legacy.mixin.base.skins.client;

import net.minecraft.client.Minecraft;
import net.minecraft.client.model.PlayerModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.renderer.entity.state.AvatarRenderState;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import wily.legacy.Skins.client.render.PlayerModelParts;
import wily.legacy.Skins.client.render.RenderStateSkinIdAccess;
import wily.legacy.Skins.client.render.boxloader.AttachSlot;
import wily.legacy.Skins.client.render.boxloader.BuiltBoxModel;
import wily.legacy.Skins.skin.ClientSkinAssets;
import wily.legacy.Skins.skin.SkinIdUtil;
import wily.legacy.compat.cpm.CpmRenderCompat;
import java.util.*;

@Mixin(PlayerModel.class)
public abstract class PlayerPartsMixin {
    @Unique private boolean consoleskins$specBoxHeadInstalled;
    @Unique private String consoleskins$specBoxHeadSkinId;
    @Unique private List<ModelPart.Cube> consoleskins$specHeadCubesBackup, consoleskins$specHatCubesBackup;
    @Unique private Map<String, ModelPart> consoleskins$specHeadChildrenBackup, consoleskins$specHatChildrenBackup;
    private static void consoleskins$resetPart(ModelPart part) { consoleskins$setSkipDraw(part, false); }
    private static void consoleskins$hidePart(ModelPart part) { consoleskins$setSkipDraw(part, true); }
    private static void consoleskins$setSkipDraw(ModelPart part, boolean skip) {
        if (part == null) return;
        part.visible = true;
        ((SkipDrawAccessor) (Object) part).consoleskins$setSkipDraw(skip);
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
            consoleskins$applySpectatorBoxHeadIfNeeded(self, skinId);
            return;
        }
        consoleskins$resetAllParts(self);
        if (SkinIdUtil.isBlankOrAutoSelect(skinId)) return;
        ClientSkinAssets.ResolvedSkin resolved = ClientSkinAssets.resolveSkin(
                skinId,
                access == null ? null : access.consoleskins$getCachedTexture(),
                access == null ? null : access.consoleskins$getCachedModelId(),
                access == null ? null : access.consoleskins$getCachedBoxModel()
        );
        if (resolved == null) return;
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
    private void consoleskins$applySpectatorBoxHeadIfNeeded(PlayerModel self, String skinId) {
        if (SkinIdUtil.isBlankOrAutoSelect(skinId)) {
            consoleskins$uninstallSpectatorBoxHead(self);
            return;
        }
        ClientSkinAssets.ResolvedSkin resolved = ClientSkinAssets.resolveSkin(skinId);
        BuiltBoxModel model = resolved == null ? null : resolved.boxModel();
        if (model == null) {
            consoleskins$uninstallSpectatorBoxHead(self);
            return;
        }
        boolean hasHeadParts = model.get(AttachSlot.HEAD) != null && !model.get(AttachSlot.HEAD).isEmpty();
        boolean hasHatParts = model.get(AttachSlot.HAT) != null && !model.get(AttachSlot.HAT).isEmpty();
        if (!model.hides(AttachSlot.HEAD) || (!hasHeadParts && !hasHatParts)) {
            consoleskins$uninstallSpectatorBoxHead(self);
            return;
        }
        if (consoleskins$specBoxHeadInstalled && skinId.equals(consoleskins$specBoxHeadSkinId)) {
            consoleskins$setSkipDraw(self.head, false);
            consoleskins$setSkipDraw(self.hat, false);
            return;
        }
        consoleskins$uninstallSpectatorBoxHead(self);
        consoleskins$setSkipDraw(self.head, false);
        consoleskins$setSkipDraw(self.hat, false);
        ModelPartAccessor head = (ModelPartAccessor) (Object) self.head;
        ModelPartAccessor hat = (ModelPartAccessor) (Object) self.hat;
        consoleskins$specHeadCubesBackup = head.consoleskins$getCubes();
        consoleskins$specHatCubesBackup = hat.consoleskins$getCubes();
        consoleskins$specHeadChildrenBackup = head.consoleskins$getChildren();
        consoleskins$specHatChildrenBackup = hat.consoleskins$getChildren();
        head.consoleskins$setCubes(List.of());
        hat.consoleskins$setCubes(List.of());
        HashMap<String, ModelPart> headChildren = new HashMap<>(consoleskins$specHeadChildrenBackup);
        HashMap<String, ModelPart> hatChildren = new HashMap<>(consoleskins$specHatChildrenBackup);
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
        head.consoleskins$setChildren(headChildren);
        hat.consoleskins$setChildren(hatChildren);
        consoleskins$specBoxHeadInstalled = true;
        consoleskins$specBoxHeadSkinId = skinId;
    }
    @Unique
    private void consoleskins$uninstallSpectatorBoxHead(PlayerModel self) {
        if (!consoleskins$specBoxHeadInstalled) return;
        ModelPartAccessor head = (ModelPartAccessor) (Object) self.head;
        ModelPartAccessor hat = (ModelPartAccessor) (Object) self.hat;
        head.consoleskins$setCubes(consoleskins$specHeadCubesBackup);
        hat.consoleskins$setCubes(consoleskins$specHatCubesBackup);
        head.consoleskins$setChildren(consoleskins$specHeadChildrenBackup);
        hat.consoleskins$setChildren(consoleskins$specHatChildrenBackup);
        consoleskins$specHeadCubesBackup = null;
        consoleskins$specHatCubesBackup = null;
        consoleskins$specHeadChildrenBackup = null;
        consoleskins$specHatChildrenBackup = null;
        consoleskins$specBoxHeadInstalled = false;
        consoleskins$specBoxHeadSkinId = null;
    }
    @Unique
    private static void consoleskins$removeChildrenWithPrefix(Map<String, ModelPart> parts, String prefix) { parts.keySet().removeIf(key -> key.startsWith(prefix)); }
    @Unique
    private static boolean consoleskins$isSpectator(UUID uuid) {
        if (uuid == null) return false;
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.level == null) return false;
        Player player = minecraft.level.getPlayerByUUID(uuid);
        return player != null && player.isSpectator();
    }
}
