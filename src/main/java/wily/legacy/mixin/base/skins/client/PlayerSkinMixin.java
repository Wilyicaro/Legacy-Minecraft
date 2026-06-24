package wily.legacy.mixin.base.skins.client;

import net.minecraft.client.Minecraft;
import net.minecraft.client.player.AbstractClientPlayer;
//? if >1.20.1 {
import net.minecraft.client.resources.PlayerSkin;
//?}
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.Items;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import wily.legacy.skins.skin.ClientSkinAssets;
import wily.legacy.skins.skin.ClientSkinCache;
import wily.legacy.skins.skin.SkinFairness;
import wily.legacy.skins.skin.SkinIdUtil;

@Mixin(AbstractClientPlayer.class)
public abstract class PlayerSkinMixin {
    @Unique
    private ClientSkinAssets.ResolvedSkin consoleskins$resolveSkin() {
        AbstractClientPlayer player = (AbstractClientPlayer) (Object) this;
        if (ClientSkinCache.isSkinOverrideBypassed()) return null;
        String skinId = SkinFairness.effectiveSkinId(Minecraft.getInstance(), ClientSkinCache.get(player.getUUID(), player.getScoreboardName()));
        if (SkinIdUtil.isBlankOrAutoSelect(skinId)) return null;
        return ClientSkinAssets.resolveSkin(skinId, player.getUUID());
    }

    //? if >1.20.1 {
    @Inject(method = "getSkin", at = @At("RETURN"), cancellable = true, require = 0)
    private void consoleskins$overrideSkin(CallbackInfoReturnable<PlayerSkin> callbackInfo) {
        AbstractClientPlayer player = (AbstractClientPlayer) (Object) this;
        ClientSkinAssets.ResolvedSkin resolved = consoleskins$resolveSkin();
        if (resolved == null || callbackInfo.getReturnValue() == null) return;
        boolean showCape = ClientSkinAssets.shouldShowCape(resolved, player.getItemBySlot(EquipmentSlot.CHEST).is(Items.ELYTRA));
        PlayerSkin skin = ClientSkinAssets.resolvePlayerSkin(SkinFairness.effectiveSkinId(Minecraft.getInstance(), ClientSkinCache.get(player.getUUID(), player.getScoreboardName())), resolved, showCape);
        if (skin != null) callbackInfo.setReturnValue(skin);
    }
    //?} else {
    /*@Inject(method = "getSkinTextureLocation", at = @At("RETURN"), cancellable = true, require = 0)
    private void consoleskins$overrideSkinTexture(CallbackInfoReturnable<ResourceLocation> callbackInfo) {
        ClientSkinAssets.ResolvedSkin resolved = consoleskins$resolveSkin();
        if (resolved != null && resolved.texture() != null) callbackInfo.setReturnValue(resolved.texture());
    }

    @Inject(method = "getCloakTextureLocation", at = @At("RETURN"), cancellable = true, require = 0)
    private void consoleskins$overrideCloakTexture(CallbackInfoReturnable<ResourceLocation> callbackInfo) {
        AbstractClientPlayer player = (AbstractClientPlayer) (Object) this;
        ClientSkinAssets.ResolvedSkin resolved = consoleskins$resolveSkin();
        boolean showCape = ClientSkinAssets.shouldShowCape(resolved, player.getItemBySlot(EquipmentSlot.CHEST).is(Items.ELYTRA));
        if (showCape && resolved.capeTexture() != null) callbackInfo.setReturnValue(resolved.capeTexture());
    }

    @Inject(method = "getModelName", at = @At("RETURN"), cancellable = true, require = 0)
    private void consoleskins$overrideModelName(CallbackInfoReturnable<String> callbackInfo) {
        AbstractClientPlayer player = (AbstractClientPlayer) (Object) this;
        String skinId = SkinFairness.effectiveSkinId(Minecraft.getInstance(), ClientSkinCache.get(player.getUUID(), player.getScoreboardName()));
        ClientSkinAssets.ResolvedSkin resolved = consoleskins$resolveSkin();
        if (resolved != null) callbackInfo.setReturnValue(ClientSkinAssets.isSlimModel(skinId, resolved) ? "slim" : "default");
    }
    *///?}
}
