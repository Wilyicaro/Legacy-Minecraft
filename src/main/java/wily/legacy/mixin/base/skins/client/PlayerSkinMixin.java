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
import wily.legacy.skins.client.util.BirthdayCapeUtil;
import wily.legacy.skins.skin.ClientSkinAssets;
import wily.legacy.skins.skin.ClientSkinCache;
import wily.legacy.skins.skin.SkinFairness;
import wily.legacy.skins.skin.SkinIdUtil;

@Mixin(AbstractClientPlayer.class)
public abstract class PlayerSkinMixin {
    @Inject(method = "getSkin", at = @At("RETURN"), cancellable = true, require = 0)
    private void consoleskins$overrideSkin(CallbackInfoReturnable<PlayerSkin> cir) {
        AbstractClientPlayer player = (AbstractClientPlayer) (Object) this;
        PlayerSkin skin = cir.getReturnValue();
        if (skin == null) return;
        boolean blockedByElytra = player.getItemBySlot(EquipmentSlot.CHEST).is(Items.ELYTRA);
        if (!ClientSkinCache.isSkinOverrideBypassed()) {
            String skinId = SkinFairness.effectiveSkinId(Minecraft.getInstance(), ClientSkinCache.get(player.getUUID()));
            if (!SkinIdUtil.isBlankOrAutoSelect(skinId)) {
                ClientSkinAssets.ResolvedSkin resolved = ClientSkinAssets.resolveSkin(skinId, player.getUUID());
                boolean wantCape = ClientSkinAssets.shouldShowCape(resolved, blockedByElytra);
                PlayerSkin resolvedSkin = ClientSkinAssets.resolvePlayerSkin(skinId, resolved, wantCape);
                if (resolvedSkin != null) skin = resolvedSkin;
            }
        }
        PlayerSkin birthdaySkin = BirthdayCapeUtil.apply(skin, blockedByElytra);
        if (birthdaySkin != skin) skin = birthdaySkin;
        cir.setReturnValue(skin);
    }
}
