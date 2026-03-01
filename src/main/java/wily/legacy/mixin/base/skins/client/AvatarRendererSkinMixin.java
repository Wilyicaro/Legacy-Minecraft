package wily.legacy.mixin.base.skins.client;

import wily.legacy.Skins.client.render.RenderStateSkinIdAccess;
import wily.legacy.Skins.skin.ClientSkinCache;
import wily.legacy.Skins.skin.SkinEntry;
import wily.legacy.Skins.skin.ClientSkinAssets;
import wily.legacy.Skins.skin.SkinPackLoader;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.entity.player.AvatarRenderer;
import net.minecraft.client.renderer.entity.state.AvatarRenderState;
import net.minecraft.core.ClientAsset;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Avatar;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.entity.player.PlayerModelType;
import net.minecraft.world.entity.player.PlayerSkin;
import net.minecraft.world.item.Items;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

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
                    float speedSq = 0.0F;
                    try {
                        var v = avatar.getDeltaMovement();
                        if (v != null) {
                            speedSq = (float) (v.x * v.x + v.z * v.z);
                            moving = speedSq > 1.0E-4;
                        }
                    } catch (Throwable ignored2) {
                    }
                    a.consoleskins$setMoving(moving);
                    a.consoleskins$setMoveSpeedSq(speedSq);
                } catch (Throwable ignored) {
                }
                try {
                    boolean sitting = false;
                    try {
                        sitting = avatar.isPassenger();
                    } catch (Throwable ignored2) {
                    }
                    try {
                        sitting = sitting || avatar.getPose() == Pose.SITTING;
                    } catch (Throwable ignored2) {
                    }
                    a.consoleskins$setSitting(sitting);
                } catch (Throwable ignored) {
                }
            }

            SkinEntry entry = SkinPackLoader.getSkin(skinId);
            ResourceLocation tex = entry != null ? entry.texture() : null;
            if (tex == null) tex = ClientSkinAssets.getTexture(skinId);
            if (tex == null) return;
            if (tex == null) return;

            PlayerSkin original = state.skin;
            if (original == null) return;

            ClientAsset.ResourceTexture body = new ClientAsset.ResourceTexture(tex, tex);

            ClientAsset.ResourceTexture capeTex = null;
            if (entry != null && entry.cape() != null) {
                ResourceLocation capePath = entry.cape();
                try {
                    Minecraft.getInstance().getTextureManager().getTexture(capePath);
                } catch (Throwable ignored) {
                }
                capeTex = new ClientAsset.ResourceTexture(capePath, capePath);
            }

            try {
                if (avatar.getItemBySlot(EquipmentSlot.CHEST).is(Items.ELYTRA)) capeTex = null;
            } catch (Throwable ignored) {
            }

            state.showCape = capeTex != null;

            PlayerModelType model = (entry != null && entry.slimArms()) ? PlayerModelType.SLIM : PlayerModelType.WIDE;

            ClientAsset.Texture capeFinal = capeTex;
            state.skin = PlayerSkin.insecure(body, capeFinal, null, model);
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
