/*? if >=1.21.2 {*/
/*package wily.legacy.mixin.base.skins.client;

import net.minecraft.client.Minecraft;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.renderer.entity.player.PlayerRenderer;
import net.minecraft.client.renderer.entity.state.PlayerRenderState;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.item.Items;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import wily.legacy.skins.client.render.RenderStateSkinIdAccess;
import wily.legacy.skins.skin.ClientSkinAssets;
import wily.legacy.skins.skin.ClientSkinCache;
import wily.legacy.skins.skin.SkinFairness;
import wily.legacy.skins.skin.SkinIdUtil;

@Mixin(PlayerRenderer.class)
public abstract class PlayerRendererSkinStateMixin {
    @Unique
    private static void consoleskins$clearSkinState(RenderStateSkinIdAccess access) {
        if (access == null) return;
        access.consoleskins$setSkinId(null);
        access.consoleskins$setEntityUuid(null);
        access.consoleskins$setMoving(false);
        access.consoleskins$setMoveSpeedSq(0.0F);
        access.consoleskins$setSitting(false);
        access.consoleskins$setUsingItem(false);
        access.consoleskins$setBlocking(false);
        access.consoleskins$setCachedTexture(null);
        access.consoleskins$setCachedBoxTexture(null);
        access.consoleskins$setCachedModelId(null);
        access.consoleskins$setCachedBoxModel(null);
    }

    @Inject(method = "extractRenderState(Lnet/minecraft/client/player/AbstractClientPlayer;Lnet/minecraft/client/renderer/entity/state/PlayerRenderState;F)V", at = @At("RETURN"), require = 0)
    private void consoleskins$patchStateSkin(AbstractClientPlayer player, PlayerRenderState state, float partialTick, CallbackInfo callbackInfo) {
        if (player == null || state == null || !(state instanceof RenderStateSkinIdAccess access)) return;
        consoleskins$clearSkinState(access);
        String skinId = SkinFairness.effectiveSkinId(Minecraft.getInstance(), ClientSkinCache.get(player.getUUID(), player.getScoreboardName()));
        if (SkinIdUtil.isBlankOrAutoSelect(skinId)) return;
        access.consoleskins$setSkinId(skinId);
        access.consoleskins$setEntityUuid(player.getUUID());
        var movement = player.getDeltaMovement();
        float speedSq = movement == null ? 0.0F : (float) (movement.x * movement.x + movement.z * movement.z);
        access.consoleskins$setMoving(speedSq > 1.0E-4F);
        access.consoleskins$setMoveSpeedSq(speedSq);
        access.consoleskins$setSitting(player.isPassenger() || player.getPose() == Pose.SITTING);
        access.consoleskins$setUsingItem(player.isUsingItem());
        access.consoleskins$setBlocking(player.isBlocking());
        ClientSkinAssets.ResolvedSkin resolved = ClientSkinAssets.resolveSkin(access);
        access.consoleskins$setCachedTexture(resolved == null ? null : resolved.texture());
        access.consoleskins$setCachedBoxTexture(resolved == null ? null : resolved.boxTexture());
        access.consoleskins$setCachedModelId(resolved == null ? null : resolved.modelId());
        access.consoleskins$setCachedBoxModel(resolved == null ? null : resolved.boxModel());
        boolean showCape = ClientSkinAssets.shouldShowCape(resolved, player.getItemBySlot(EquipmentSlot.CHEST).is(Items.ELYTRA));
        state.showCape = showCape;
        var skin = ClientSkinAssets.resolvePlayerSkin(skinId, resolved, showCape);
        if (skin != null) state.skin = skin;
    }
}
*//*?}*/
