package wily.legacy.mixin.base.skins.client;

import wily.legacy.Skins.client.render.RenderStateSkinIdAccess;
import wily.legacy.Skins.skin.ClientSkinCache;
import wily.legacy.Skins.skin.SkinEntry;
import wily.legacy.Skins.skin.ClientSkinAssets;
import wily.legacy.Skins.skin.SkinPackLoader;
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
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(AvatarRenderer.class)
public abstract class AvatarSkinMixin {

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

                try {
                    boolean using = false;
                    boolean blocking = false;
                    try {
                        using = avatar.isUsingItem();
                    } catch (Throwable ignored2) {
                    }
                    try {
                        blocking = avatar.isBlocking();
                    } catch (Throwable ignored2) {
                    }
                    a.consoleskins$setUsingItem(using);
                    a.consoleskins$setBlocking(blocking);
                } catch (Throwable ignored) {
                }
            }

            SkinEntry entry = SkinPackLoader.getSkin(skinId);

            // Cache the entry and resolved texture/model on the render state so
            // downstream mixins (PlayerPartsMixin, ModelOffsetsMixin, BoxAddonLayer,
            // ArmorOffsetsMixin) don't repeat these lookups.
            if (state instanceof RenderStateSkinIdAccess acc) {
                acc.consoleskins$setCachedEntry(entry);

                ResourceLocation tex = ClientSkinAssets.getTexture(skinId);
                if (tex == null && entry != null) tex = entry.texture();
                acc.consoleskins$setCachedTexture(tex);

                if (tex != null) {
                    ResourceLocation modelId = ClientSkinAssets.getModelIdFromTexture(tex);
                    acc.consoleskins$setCachedModelId(modelId);

                    wily.legacy.Skins.client.render.boxloader.BuiltBoxModel boxModel =
                            wily.legacy.Skins.client.render.boxloader.BoxModelManager.get(modelId);
                    if (boxModel == null) {
                        com.google.gson.JsonObject mj = ClientSkinAssets.getModelJson(skinId);
                        if (mj != null) {
                            wily.legacy.Skins.client.render.boxloader.BoxModelManager.registerRuntime(modelId, mj);
                            boxModel = wily.legacy.Skins.client.render.boxloader.BoxModelManager.get(modelId);
                        }
                    }
                    acc.consoleskins$setCachedBoxModel(boxModel);
                }
            }

            // Determine if cape should show
            boolean wantCape = entry != null && entry.cape() != null;
            if (wantCape) {
                try {
                    if (avatar.getItemBySlot(EquipmentSlot.CHEST).is(Items.ELYTRA)) wantCape = false;
                } catch (Throwable ignored) {
                }
            }

            state.showCape = wantCape;

            // Use cached PlayerSkin to avoid per-frame allocation of ResourceTexture + PlayerSkin
            PlayerSkin cachedSkin = ClientSkinAssets.getCachedPlayerSkin(skinId, entry, wantCape);
            if (cachedSkin == null) return;

            state.skin = cachedSkin;
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
