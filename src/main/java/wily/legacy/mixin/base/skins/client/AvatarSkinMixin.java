package wily.legacy.mixin.base.skins.client;

import net.minecraft.client.Minecraft;
import wily.legacy.Skins.client.render.RenderStateSkinIdAccess;
import wily.legacy.Skins.skin.ClientSkinCache;
import wily.legacy.Skins.skin.ClientSkinAssets;
import wily.legacy.Skins.skin.SkinFairness;
import wily.legacy.Skins.skin.SkinIdUtil;
import net.minecraft.client.renderer.entity.player.AvatarRenderer;
import net.minecraft.client.renderer.entity.state.AvatarRenderState;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Avatar;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.entity.player.PlayerSkin;
import net.minecraft.world.item.Items;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(AvatarRenderer.class)
public abstract class AvatarSkinMixin {
    @Inject(method = "extractRenderState", at = @At("RETURN"), require = 0)
    private void consoleskins$patchStateSkin(Avatar avatar, AvatarRenderState state, float partialTick, CallbackInfo ci) {
        if (avatar == null || state == null) return;
        consoleskins$applySkinToState(avatar, state);
    }
    @Unique
    private static void consoleskins$applySkinToState(Avatar avatar, AvatarRenderState state) {
        if (!(state instanceof RenderStateSkinIdAccess access)) return;
        consoleskins$clearSkinState(access);
        String skinId = SkinFairness.effectiveSkinId(Minecraft.getInstance(), consoleskins$resolveSkinId(avatar));
        if (SkinIdUtil.isBlankOrAutoSelect(skinId)) return;
        access.consoleskins$setSkinId(skinId);
        access.consoleskins$setEntityUuid(avatar.getUUID());
        var movement = avatar.getDeltaMovement();
        float speedSq = movement == null ? 0.0F : (float) (movement.x * movement.x + movement.z * movement.z);
        access.consoleskins$setMoving(speedSq > 1.0E-4F);
        access.consoleskins$setMoveSpeedSq(speedSq);
        access.consoleskins$setSitting(avatar.isPassenger() || avatar.getPose() == Pose.SITTING);
        access.consoleskins$setUsingItem(avatar.isUsingItem());
        access.consoleskins$setBlocking(avatar.isBlocking());
        ClientSkinAssets.ResolvedSkin resolved = ClientSkinAssets.resolveSkin(access);
        access.consoleskins$setCachedTexture(resolved == null ? null : resolved.texture());
        access.consoleskins$setCachedBoxTexture(resolved == null ? null : resolved.boxTexture());
        access.consoleskins$setCachedModelId(resolved == null ? null : resolved.modelId());
        access.consoleskins$setCachedBoxModel(resolved == null ? null : resolved.boxModel());
        boolean showCape = ClientSkinAssets.shouldShowCape(resolved, avatar.getItemBySlot(EquipmentSlot.CHEST).is(Items.ELYTRA));
        state.showCape = showCape;
        PlayerSkin cachedSkin = ClientSkinAssets.resolvePlayerSkin(skinId, resolved, showCape);
        if (cachedSkin != null) { state.skin = cachedSkin; }
    }
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
    @Unique
    private static String consoleskins$resolveSkinId(Avatar avatar) {
        String name = avatar.getScoreboardName();
        if (name != null && !name.isBlank()) {
            String skinId = ClientSkinCache.getByName(name);
            if (skinId != null && !skinId.isBlank()) return skinId;
        }
        return ClientSkinCache.get(avatar.getUUID());
    }
}
