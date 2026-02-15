package wily.legacy.Skins.mixin.client;


/**
 * Mixin: console skins / CPM rendering glue.
 */

import wily.legacy.Skins.client.cpm.CpmModelManager;
import wily.legacy.Skins.skin.SkinIdUtil;
import wily.legacy.Skins.skin.ClientSkinCache;
import wily.legacy.Skins.skin.SkinEntry;
import wily.legacy.Skins.skin.SkinPackLoader;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.core.ClientAsset;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.PlayerSkin;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.Optional;

@Mixin(AbstractClientPlayer.class)
public abstract class AbstractClientPlayerMixin {

    @Inject(method = "getSkin", at = @At("RETURN"), cancellable = true, require = 0)
    private void consoleskins$overrideSkin(CallbackInfoReturnable<PlayerSkin> cir) {
        AbstractClientPlayer player = (AbstractClientPlayer) (Object) this;

        if (ClientSkinCache.isSkinOverrideBypassed()) return;

        String skinId = ClientSkinCache.get(player.getUUID());
        if (skinId == null || skinId.isBlank()) return;
        if ("auto_select".equals(skinId)) return;

        PlayerSkin original = cir.getReturnValue();
        if (original == null) return;

        SkinEntry entry = SkinPackLoader.getSkin(skinId);
        if (entry == null) return;

        ResourceLocation texturePath = entry.texture();
        if (texturePath == null) return;

        if (SkinIdUtil.isCpm(skinId)) {
            try {
                CpmModelManager.applyToProfile(player.getGameProfile(), skinId);
            } catch (Throwable ignored) {
            }
        }

        try {
            Minecraft.getInstance().getTextureManager().getTexture(texturePath);
        } catch (Throwable ignored) {
        }

        ClientAsset.ResourceTexture body = new ClientAsset.ResourceTexture(texturePath, texturePath);

        PlayerSkin.Patch patch = PlayerSkin.Patch.create(
                Optional.of(body),
                Optional.empty(),
                Optional.empty(),
                Optional.empty()
        );

        cir.setReturnValue(original.with(patch));
    }
}
