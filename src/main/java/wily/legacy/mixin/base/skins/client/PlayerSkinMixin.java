package wily.legacy.mixin.base.skins.client;

import net.minecraft.client.Minecraft;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.PlayerSkin;
import net.minecraft.world.item.Items;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import wily.legacy.skins.skin.*;

@Mixin(AbstractClientPlayer.class)
public abstract class PlayerSkinMixin {
    @Inject(method = "isCapeLoaded", at = @At("HEAD"), cancellable = true, require = 0)
    private void consoleskins$disableCapeWhenElytra(CallbackInfoReturnable<Boolean> cir) {
        if (((AbstractClientPlayer) (Object) this).getItemBySlot(EquipmentSlot.CHEST).is(Items.ELYTRA)) cir.setReturnValue(false);
    }
    @Inject(method = "getSkin", at = @At("RETURN"), cancellable = true, require = 0)
    private void consoleskins$overrideSkin(CallbackInfoReturnable<PlayerSkin> cir) {
        AbstractClientPlayer player = (AbstractClientPlayer) (Object) this;
        if (ClientSkinCache.isSkinOverrideBypassed()) return;
        String skinId = SkinFairness.effectiveSkinId(Minecraft.getInstance(), ClientSkinCache.get(player.getUUID()));
        if (SkinIdUtil.isBlankOrAutoSelect(skinId)) return;
        if (cir.getReturnValue() == null) return;
        ClientSkinAssets.ResolvedSkin resolved = ClientSkinAssets.resolveSkin(skinId, player.getUUID());
        boolean wantCape = ClientSkinAssets.shouldShowCape(resolved, player.getItemBySlot(EquipmentSlot.CHEST).is(Items.ELYTRA));
        PlayerSkin skin = ClientSkinAssets.resolvePlayerSkin(skinId, resolved, wantCape);
        if (skin == null) return;
        cir.setReturnValue(skin);
    }
}
