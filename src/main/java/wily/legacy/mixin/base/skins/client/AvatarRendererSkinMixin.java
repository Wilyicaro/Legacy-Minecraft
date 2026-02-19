package wily.legacy.mixin.base.skins.client;


/**
 * Mixin: console skins / CPM rendering glue.
 */

import wily.legacy.Skins.client.cpm.CpmModelManager;
import wily.legacy.Skins.client.render.RenderStateSkinIdAccess;
import wily.legacy.Skins.skin.SkinIdUtil;
import wily.legacy.Skins.skin.ClientSkinCache;
import wily.legacy.Skins.skin.SkinEntry;
import wily.legacy.Skins.skin.SkinPackLoader;
import net.minecraft.client.renderer.entity.player.AvatarRenderer;
import net.minecraft.client.renderer.entity.state.AvatarRenderState;
import net.minecraft.core.ClientAsset;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Avatar;
import net.minecraft.world.entity.player.PlayerSkin;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.Optional;
import java.util.UUID;

@Mixin(AvatarRenderer.class)
public abstract class AvatarRendererSkinMixin {


    @Inject(method = "extractRenderState", at = @At("RETURN"), require = 0)
    private void consoleskins$patchStateSkin(Avatar avatar, AvatarRenderState state, float partialTick, CallbackInfo ci) {
        if (avatar == null || state == null) return;
        consoleskins$applySkinToState(avatar, state);
    }

    @Unique
    private static void consoleskins$applySkinToState(Avatar avatar, AvatarRenderState state) {
        try {

            String name = null;
            try {
                name = avatar.getScoreboardName();
            } catch (Throwable ignored) {
            }

            String skinId = null;
            if (name != null && !name.isBlank()) {
                skinId = ClientSkinCache.getByName(name);
            }


            if (skinId == null || skinId.isBlank()) {
                try {
                    skinId = ClientSkinCache.get(avatar.getUUID());
                } catch (Throwable ignored) {
                }
            }

            if (skinId == null || skinId.isBlank()) return;
            if ("auto_select".equals(skinId)) return;

            if (state instanceof RenderStateSkinIdAccess a) {
                a.consoleskins$setSkinId(skinId);
                try {
                    a.consoleskins$setEntityUuid(avatar.getUUID());
                } catch (Throwable ignored) {
                }
                try {
                    boolean moving = false;
                    try {
                        var v = avatar.getDeltaMovement();
                        moving = v != null && (v.x * v.x + v.z * v.z) > 1.0E-4;
                    } catch (Throwable ignored2) {
                    }
                    a.consoleskins$setMoving(moving);
                } catch (Throwable ignored) {
                }
            }


            try {
                UUID uuid = avatar.getUUID();
                if (uuid != null && SkinIdUtil.isCpm(skinId) && CpmModelManager.isModelLoaded(uuid)) {
                    return;
                }
            } catch (Throwable ignored) {
            }

            SkinEntry entry = SkinPackLoader.getSkin(skinId);
            if (entry == null) return;
            ResourceLocation tex = entry.texture();
            if (tex == null) return;

            PlayerSkin original = state.skin;
            if (original == null) return;

            ClientAsset.ResourceTexture body = new ClientAsset.ResourceTexture(tex, tex);
            PlayerSkin.Patch patch = PlayerSkin.Patch.create(
                    Optional.of(body),
                    Optional.empty(),
                    Optional.empty(),
                    Optional.empty()
            );
            state.skin = original.with(patch);
        } catch (Throwable ignored) {
        }
    }


    @Inject(
            method = "getTextureLocation(Lnet/minecraft/client/renderer/entity/state/AvatarRenderState;)Lnet/minecraft/resources/ResourceLocation;",
            at = @At("RETURN"),
            cancellable = true,
            require = 0
    )
    private void consoleskins$overrideAvatarTexture(AvatarRenderState state, CallbackInfoReturnable<ResourceLocation> cir) {

    }
}
