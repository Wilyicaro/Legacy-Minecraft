package wily.legacy.mixin.base.skins.client;

import wily.legacy.Skins.skin.ClientSkinCache;
import wily.legacy.Skins.skin.ClientSkinAssets;
import wily.legacy.Skins.skin.SkinIdUtil;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.world.entity.player.PlayerSkin;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.Items;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(AbstractClientPlayer.class)
public abstract class PlayerSkinMixin {
    @Inject(method = "getSkin", at = @At("RETURN"), cancellable = true, require = 0)
    private void consoleskins$overrideSkin(CallbackInfoReturnable<PlayerSkin> cir) {
        AbstractClientPlayer player = (AbstractClientPlayer) (Object) this;
        if (ClientSkinCache.isSkinOverrideBypassed()) return;
        String skinId = ClientSkinCache.get(player.getUUID());
        if (SkinIdUtil.isBlankOrAutoSelect(skinId)) return;
        if (cir.getReturnValue() == null) return;
        ClientSkinAssets.ResolvedSkin resolved = ClientSkinAssets.resolveSkin(skinId);
        boolean wantCape = ClientSkinAssets.shouldShowCape(resolved, player.getItemBySlot(EquipmentSlot.CHEST).is(Items.ELYTRA));
        PlayerSkin skin = ClientSkinAssets.resolvePlayerSkin(skinId, resolved, wantCape);
        if (skin == null) return;
        cir.setReturnValue(skin);
    }
}
